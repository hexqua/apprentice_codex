package jp.aquafactory.apprenticecodex.spell.demicreatorwings;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DemicreatorWingsCoreEntity extends Entity implements TraceableEntity, AntiMagicSusceptible {
    private static final int NO_OWNER_ENTITY_ID = -1;
    private static final EntityDataAccessor<Integer> ALLOWED_RADIUS =
            SynchedEntityData.defineId(DemicreatorWingsCoreEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION_TICKS =
            SynchedEntityData.defineId(DemicreatorWingsCoreEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ENTITY_ID =
            SynchedEntityData.defineId(DemicreatorWingsCoreEntity.class, EntityDataSerializers.INT);

    private @Nullable UUID ownerUUID;
    private @Nullable Entity cachedOwner;

    public DemicreatorWingsCoreEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public DemicreatorWingsCoreEntity(EntityType<?> entityType, Level level, ServerPlayer owner, int allowedRadius, int durationTicks) {
        this(entityType, level);
        setOwner(owner);
        entityData.set(ALLOWED_RADIUS, allowedRadius);
        entityData.set(DURATION_TICKS, durationTicks);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ALLOWED_RADIUS, 1);
        builder.define(DURATION_TICKS, 20);
        builder.define(OWNER_ENTITY_ID, NO_OWNER_ENTITY_ID);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        ownerUUID = null;
        cachedOwner = null;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        // ログインを跨いだ維持をしないため保存しない。
    }

    @Override
    public void tick() {
        super.tick();
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);

        if (level().isClientSide) {
            tickClient();
            return;
        }

        if (level() instanceof ServerLevel serverLevel) {
            tickServer(serverLevel);
        }
    }

    @Override
    public @Nullable Entity getOwner() {
        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        }

        int ownerEntityId = entityData.get(OWNER_ENTITY_ID);
        if (ownerEntityId != NO_OWNER_ENTITY_ID) {
            cachedOwner = level().getEntity(ownerEntityId);
            if (cachedOwner != null && !cachedOwner.isRemoved()) {
                return cachedOwner;
            }
        }

        if (ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            cachedOwner = serverLevel.getEntity(ownerUUID);
            return cachedOwner;
        }

        return null;
    }

    public void setOwner(Entity owner) {
        ownerUUID = owner.getUUID();
        cachedOwner = owner;
        entityData.set(OWNER_ENTITY_ID, owner.getId());
    }

    public int getAllowedRadius() {
        return entityData.get(ALLOWED_RADIUS);
    }

    public int getDurationTicks() {
        return entityData.get(DURATION_TICKS);
    }

    public int getRemainingTicks() {
        return Math.max(0, getDurationTicks() - tickCount);
    }

    public int getOwnerEntityId() {
        return entityData.get(OWNER_ENTITY_ID);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public void push(@NotNull Entity entity) {
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return false;
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (level().isClientSide || isRemoved()) {
            return;
        }

        if (getOwner() instanceof ServerPlayer owner) {
            DemicreatorWingsManager.deactivate(owner, true);
            return;
        }

        discard();
    }

    private void tickServer(ServerLevel level) {
        if (!(getOwner() instanceof ServerPlayer owner) || !owner.isAlive()) {
            discard();
            return;
        }

        if (owner.level() != level) {
            discard();
            return;
        }

        var spellData = Capabilities.getSpellDataOrNull(owner);
        if (spellData == null) {
            discard();
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.DEMICREATOR_WINGS_STATE);
        if (!state.active || state.coreEntityId != getId()) {
            discard();
        }
    }

    private void tickClient() {
        var random = level().random;
        var hue = (tickCount % 120) / 120.0f;
        var rgb = Mth.hsvToRgb(hue, 0.9f, 1.0f);
        var red = ((rgb >> 16) & 0xFF) / 255.0f;
        var green = ((rgb >> 8) & 0xFF) / 255.0f;
        var blue = (rgb & 0xFF) / 255.0f;

        for (var index = 0; index < 2; ++index) {
            var angle = tickCount * 0.16 + index * Math.PI;
            var distance = 0.18 + random.nextDouble() * 0.08;
            var x = getX() + Math.cos(angle) * distance;
            var z = getZ() + Math.sin(angle) * distance;
            level().addParticle(
                    new AdditiveGlowParticleOptions(
                            ParticleRegistry.ADDITIVE_CIRCLE.get(),
                            0.24f,
                            red,
                            green,
                            blue,
                            6,
                            18,
                            4,
                            0.9f,
                            1.15f,
                            0.45f,
                            0.85f,
                            0.08f,
                            0.75f,
                            0.82f,
                            true
                    ),
                    x,
                    getY() + 0.08 + random.nextDouble() * 0.04,
                    z,
                    0.0,
                    0.01 + random.nextDouble() * 0.01,
                    0.0
            );
        }

        for (var index = 0; index < 2; ++index) {
            var offset = new Vec3(
                    (random.nextDouble() - 0.5) * 0.18,
                    random.nextDouble() * 0.12,
                    (random.nextDouble() - 0.5) * 0.18
            );
            var velocity = new Vec3(
                    (random.nextDouble() - 0.5) * 0.01,
                    0.02 + random.nextDouble() * 0.02,
                    (random.nextDouble() - 0.5) * 0.01
            );
            level().addParticle(
                    new AdditiveGlowParticleOptions(
                            ParticleRegistry.ADDITIVE_SPARK.get(),
                            0.12f,
                            red,
                            green,
                            blue,
                            5,
                            14,
                            4,
                            0.95f,
                            1.25f,
                            0.75f,
                            1.0f,
                            0.08f,
                            0.7f,
                            0.86f,
                            true
                    ),
                    getX() + offset.x,
                    getY() + 0.06 + offset.y,
                    getZ() + offset.z,
                    velocity.x,
                    velocity.y,
                    velocity.z
            );
        }
    }
}
