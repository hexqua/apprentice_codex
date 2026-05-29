package jp.aquafactory.apprenticecodex.spell.mysticshield;

import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// Counterspell は AntiMagicSusceptible を raycast の有効対象にするため、盾ではなく術者本体で受ける。
public class MysticShieldShieldEntity extends Entity implements TraceableEntity {
    static final int FADE_TICKS = 4;

    private static final EntityDataAccessor<Boolean> DATA_FADING =
            SynchedEntityData.defineId(MysticShieldShieldEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double SHIELD_DISTANCE = 1.15;
    private static final double SHIELD_Y_OFFSET = -0.65;

    private LivingEntity owner;
    private UUID ownerUuid;
    private int ownerId = -1;
    private int fadeTicks;

    public MysticShieldShieldEntity(EntityType<? extends MysticShieldShieldEntity> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public MysticShieldShieldEntity(EntityType<? extends MysticShieldShieldEntity> entityType, Level level, LivingEntity owner) {
        this(entityType, level);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FADING, false);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;

        if (isFading()) {
            ++fadeTicks;
            if (!level().isClientSide && fadeTicks >= FADE_TICKS) {
                discard();
            }
            return;
        }

        var resolvedOwner = getOwner();
        if (resolvedOwner == null || resolvedOwner.isRemoved() || !resolvedOwner.isAlive()) {
            if (!level().isClientSide) {
                startFade();
            }
            return;
        }

        snapToOwner();
    }

    public void startFade() {
        if (!isFading()) {
            entityData.set(DATA_FADING, true);
            fadeTicks = 0;
            if (!level().isClientSide) {
                spawnBreakParticles();
            }
        }
    }

    public void snapToOwner() {
        var resolvedOwner = getOwner();
        if (resolvedOwner == null) {
            return;
        }

        var look = normalizeOrFallback(resolvedOwner.getLookAngle(), new Vec3(0.0, 0.0, 1.0));
        var position = resolvedOwner.getEyePosition().add(look.scale(SHIELD_DISTANCE)).add(0.0, SHIELD_Y_OFFSET, 0.0);
        setPos(position.x, position.y, position.z);
        setYRot(resolvedOwner.getYRot());
        setXRot(resolvedOwner.getXRot());
        yRotO = getYRot();
        xRotO = getXRot();
    }

    @Override
    public @Nullable LivingEntity getOwner() {
        if (owner != null && !owner.isRemoved()) {
            return owner;
        }

        if (ownerId >= 0) {
            var entity = level().getEntity(ownerId);
            if (entity instanceof LivingEntity livingEntity) {
                owner = livingEntity;
                return livingEntity;
            }
        }

        if (ownerUuid != null && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            var entity = serverLevel.getEntity(ownerUuid);
            if (entity instanceof LivingEntity livingEntity) {
                owner = livingEntity;
                ownerId = livingEntity.getId();
                return livingEntity;
            }
        }

        return null;
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
        ownerUuid = owner.getUUID();
        ownerId = owner.getId();
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            ownerUuid = tag.getUUID("Owner");
        }
        ownerId = tag.getInt("OwnerId");
        entityData.set(DATA_FADING, tag.getBoolean("Fading"));
        fadeTicks = tag.getInt("FadeTicks");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        tag.putInt("OwnerId", ownerId);
        tag.putBoolean("Fading", isFading());
        tag.putInt("FadeTicks", fadeTicks);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity serverEntity) {
        return super.getAddEntityPacket(serverEntity);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(2.5);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        var maxDistance = 96.0;
        return distanceSqr < maxDistance * maxDistance;
    }

    public boolean isFading() {
        return entityData.get(DATA_FADING);
    }

    private void spawnBreakParticles() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var random = level().random;
        var right = rightVectorFromYaw();
        var center = position();
        for (var side : new double[]{-1.0, 1.0}) {
            var panelCenter = center.add(right.scale(side * 0.42));
            for (var i = 0; i < 36; ++i) {
                var velocity = right.scale(side * (0.018 + random.nextDouble() * 0.035))
                        .add(randomOffset(0.055))
                        .add(0.0, 0.015 + random.nextDouble() * 0.035, 0.0);
                var positionOffset = right.scale((random.nextDouble() - 0.5) * 0.3)
                        .add(0.0, (random.nextDouble() - 0.5) * 0.5, 0.0)
                        .add(randomOffset(0.08));

                serverLevel.sendParticles(
                        new AdditiveGlowParticleOptions(
                                i % 3 == 0 ? ParticleRegistry.ADDITIVE_RHOMBUS.get() : ParticleRegistry.ADDITIVE_SPARK.get(),
                                i % 3 == 0 ? 0.18f : 0.12f,
                                1.0f,
                                0.58f + random.nextFloat() * 0.16f,
                                0.08f + random.nextFloat() * 0.1f,
                                i % 3 == 0 ? 3 : 5
                        ),
                        panelCenter.x + positionOffset.x,
                        panelCenter.y + positionOffset.y,
                        panelCenter.z + positionOffset.z,
                        0,
                        velocity.x,
                        velocity.y,
                        velocity.z,
                        1.0
                );
            }
        }
    }

    private Vec3 rightVectorFromYaw() {
        var yawRadians = Math.toRadians(getYRot());
        return new Vec3(Math.cos(yawRadians), 0.0, Math.sin(yawRadians));
    }

    private Vec3 randomOffset(double scale) {
        var random = level().random;
        return new Vec3(
                (random.nextDouble() - 0.5) * scale,
                (random.nextDouble() - 0.5) * scale,
                (random.nextDouble() - 0.5) * scale
        );
    }

    private static Vec3 normalizeOrFallback(Vec3 vector, Vec3 fallback) {
        if (vector != null && vector.lengthSqr() > 1.0e-6) {
            return vector.normalize();
        }
        return fallback.normalize();
    }
}
