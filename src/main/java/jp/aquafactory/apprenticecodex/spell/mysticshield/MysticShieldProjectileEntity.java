package jp.aquafactory.apprenticecodex.spell.mysticshield;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;

public class MysticShieldProjectileEntity extends Projectile implements AntiMagicSusceptible {
    private static final int LIFE_TICKS = 60;
    private static final double SPEED = 1.55;
    private static final double TRAIL_INTERPOLATION_STEP = 0.24;
    private static final int MAX_INTERPOLATED_PARTICLES = 6;

    private float damage;
    private Vec3 clientLastTrailPosition;

    public MysticShieldProjectileEntity(EntityType<? extends MysticShieldProjectileEntity> entityType, Level level) {
        super(entityType, level);
        setViewScale(8.0f);
        setNoGravity(true);
    }

    public MysticShieldProjectileEntity(EntityType<? extends MysticShieldProjectileEntity> entityType, Level level, LivingEntity owner) {
        this(entityType, level);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);

        if (tickCount >= LIFE_TICKS) {
            discard();
            return;
        }

        if (level().isClientSide) {
            move(MoverType.SELF, getDeltaMovement());
            ProjectileUtil.rotateTowardsMovement(this, 1.0f);
            spawnTrailParticles();
            return;
        }

        var hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hitResult)) {
            onHit(hitResult);
        }

        if (isRemoved()) {
            return;
        }

        move(MoverType.SELF, getDeltaMovement());
        // 中心線のレイキャストが外れても当たり箱が障害物を擦るため、move の物理衝突も着弾として扱う。
        if (horizontalCollision || verticalCollision) {
            discard();
            return;
        }
        ProjectileUtil.rotateTowardsMovement(this, 1.0f);
    }

    public void shoot(Vec3 direction) {
        var normalizedDirection = normalizeOrFallback(direction, new Vec3(0.0, 0.0, 1.0));
        setDeltaMovement(normalizedDirection.scale(SPEED));
        ProjectileUtil.rotateTowardsMovement(this, 1.0f);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamageForGameTest() {
        return damage;
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        var target = CombatTools.resolutePartEntity(entity);
        return target != getOwner() && CombatTools.isValidCombatTarget(target, getOwner()) && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (level().isClientSide) {
            return;
        }

        var target = CombatTools.resolutePartEntity(hitResult.getEntity());
        if (CombatTools.isValidCombatTarget(target, getOwner())) {
            var source = CombatTools.getDamageSource(level(), this, getOwner(), DamageTypes.MYSTIC_SHIELD);
            CombatTools.applyDamage(target, damage, source, SpellRegistry.MYSTIC_SHIELD.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
        }
        discard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (!level().isClientSide) {
            discard();
        }
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (level().isClientSide || isRemoved()) {
            return;
        }

        discard();
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity serverEntity) {
        return super.getAddEntityPacket(serverEntity);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        var maxDistance = 128.0;
        return distanceSqr < maxDistance * maxDistance;
    }

    private void spawnTrailParticles() {
        var current = position();
        if (clientLastTrailPosition == null) {
            clientLastTrailPosition = current;
            spawnParticleCluster(current);
            return;
        }

        var travel = current.subtract(clientLastTrailPosition);
        var distance = travel.length();
        var count = Mth.clamp((int) Math.floor(distance / TRAIL_INTERPOLATION_STEP), 1, MAX_INTERPOLATED_PARTICLES);
        for (var i = 0; i <= count; ++i) {
            var t = i / (double) (count + 1);
            spawnParticleCluster(clientLastTrailPosition.lerp(current, t));
        }
        clientLastTrailPosition = current;
    }

    private void spawnParticleCluster(Vec3 center) {
        var random = level().random;
        var direction = normalizeOrFallback(getDeltaMovement(), new Vec3(0.0, 0.0, 1.0));
        var right = computeRightVector(direction);
        var up = right.cross(direction).normalize();

        var offset = right.scale((random.nextDouble() - 0.5) * 0.18)
                .add(up.scale((random.nextDouble() - 0.5) * 0.18))
                .add(direction.scale((random.nextDouble() - 0.5) * 0.08));
        var velocity = getDeltaMovement().scale(-0.055).add(randomOffset(0.006));

        level().addParticle(
                new AdditiveGlowParticleOptions(
                        ParticleRegistry.ADDITIVE_SPARK.get(),
                        0.12f,
                        1.0f,
                        Mth.lerp(random.nextFloat(), 0.45f, 0.62f),
                        Mth.lerp(random.nextFloat(), 0.06f, 0.12f),
                        6,
                        13,
                        4,
                        0.9f,
                        1.25f,
                        0.85f,
                        1.0f,
                        0.05f,
                        0.72f,
                        0.76f,
                        true
                ),
                center.x + offset.x,
                center.y + offset.y,
                center.z + offset.z,
                velocity.x,
                velocity.y,
                velocity.z
        );

        if (random.nextFloat() < 0.45f) {
            var rhombusVelocity = getDeltaMovement().scale(-0.04).add(randomOffset(0.004));
            level().addParticle(
                    new AdditiveGlowParticleOptions(
                            ParticleRegistry.ADDITIVE_RHOMBUS.get(),
                            0.2f,
                            1.0f,
                            Mth.lerp(random.nextFloat(), 0.38f, 0.55f),
                            Mth.lerp(random.nextFloat(), 0.05f, 0.1f),
                            4
                    ),
                    center.x + offset.x * 0.5,
                    center.y + offset.y * 0.5,
                    center.z + offset.z * 0.5,
                    rhombusVelocity.x,
                    rhombusVelocity.y,
                    rhombusVelocity.z
            );
        }
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

    private static Vec3 computeRightVector(Vec3 forward) {
        var right = forward.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 1.0e-6) {
            right = forward.cross(new Vec3(1.0, 0.0, 0.0));
        }
        return right.normalize();
    }
}
