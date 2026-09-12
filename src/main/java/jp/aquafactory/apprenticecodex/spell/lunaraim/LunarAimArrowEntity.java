package jp.aquafactory.apprenticecodex.spell.lunaraim;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.particle.LunarParticles;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.ProjectileCollisionTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class LunarAimArrowEntity extends Projectile implements AntiMagicSusceptible {
    public static final double SPEED = 2;
    public static final int LIFETIME = 200;
    public static final int STRAIGHT_TICKS = 5;
    public static final int BURST_TICKS = 20;
    private static final int TRAIL_PARTICLES_PER_TICK = 4;
    private static final EntityDataAccessor<Boolean> BURST = SynchedEntityData.defineId(LunarAimArrowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(LunarAimArrowEntity.class, EntityDataSerializers.FLOAT);
    private float damage;
    private UUID targetId;
    private int burstAge;
    private int clientBurstAge;

    public LunarAimArrowEntity(EntityType<? extends LunarAimArrowEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void launch(LivingEntity owner, Vec3 origin, Vec3 direction, float damage, float radius, @Nullable Entity target) {
        setOwner(owner);
        setPos(origin);
        setDeltaMovement(direction.normalize().scale(SPEED));
        this.damage = damage;
        entityData.set(RADIUS, radius);
        targetId = isLiveTarget(target) ? target.getUUID() : null;
        ProjectileUtil.rotateTowardsMovement(this, 1);
    }

    private boolean isLiveTarget(@Nullable Entity target) {
        return target != null && target.level() == level() && target.isAlive() && !target.isRemoved()
                && !(target instanceof LivingEntity living && living.isDeadOrDying())
                && CombatTools.isValidCombatTarget(target, getOwner());
    }

    public boolean hasTarget() { return targetId != null; }
    public boolean isBursting() { return entityData.get(BURST); }
    public float getBurstAge(float partialTick) { return clientBurstAge + partialTick; }
    public float getRadius() { return entityData.get(RADIUS); }

    @Override public void tick() {
        if (isRemoved()) return;
        super.tick();
        if (level().isClientSide) {
            if (isBursting()) {
                if (clientBurstAge < 10) spawnBurstParticles();
                clientBurstAge++;
            } else {
                var start = position();
                setPos(start.add(getDeltaMovement()));
                for (int i = 0; i < TRAIL_PARTICLES_PER_TICK; i++) {
                    var point = start.lerp(position(), i / (double) TRAIL_PARTICLES_PER_TICK).add(
                            (random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.15,
                            (random.nextDouble() - 0.5) * 0.15);
                    var velocity = getDeltaMovement().scale(-0.03);
                    // 4本分の軌跡が長く重ならないよう、UniteLunaの色・大きさを保ったまま寿命だけ短くする。
                    level().addParticle(LunarParticles.createTrailSpark(0.13f + random.nextFloat() * 0.04f, 10, 4),
                            point.x, point.y, point.z, velocity.x, velocity.y, velocity.z);
                }
            }
            return;
        }
        if (isBursting()) {
            if (++burstAge >= BURST_TICKS) discard();
            return;
        }
        if (tickCount >= LIFETIME) { discard(); return; }
        if (targetId != null) {
            var target = ((ServerLevel) level()).getEntity(targetId);
            // 待機中も喪失を確定し、再ロードや復活によって追尾を再開しない。
            if (!isLiveTarget(target)) targetId = null;
            else if (tickCount > STRAIGHT_TICKS) updateHoming(target);
        }
        moveWithImpact();
        if (!isRemoved() && !isBursting()) ProjectileUtil.rotateTowardsMovement(this, 1);
    }

    private void updateHoming(Entity target) {
        var delta = target.getBoundingBox().getCenter().subtract(position());
        if (delta.lengthSqr() < 1.0e-10) return;
        var desired = delta.normalize();
        var current = getDeltaMovement().normalize();
        double angle = Math.acos(Mth.clamp(current.dot(desired), -1, 1));
        // 散開中の直進期間を補正に含めず、追尾開始時はSacredArrowと同じ弱い旋回から始める。
        int homingTicks = tickCount - STRAIGHT_TICKS;
        double maxTurn = Math.toRadians(6 + 24 * Math.min(homingTicks / 20.0, 1));
        // SacredArrowと同じ旋回上限と近距離収束を使い、対象の周囲を回り続けないようにする。
        if (angle <= maxTurn || delta.length() <= SPEED * 2 || current.lengthSqr() < 0.5) {
            setDeltaMovement(desired.scale(SPEED));
        } else {
            var tangent = desired.subtract(current.scale(current.dot(desired)));
            if (tangent.lengthSqr() < 1.0e-10) {
                tangent = current.cross(Math.abs(current.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0));
            }
            setDeltaMovement(current.scale(Math.cos(maxTurn)).add(tangent.normalize().scale(Math.sin(maxTurn))).normalize().scale(SPEED));
        }
    }

    private void moveWithImpact() {
        var start = position();
        var shape = level().getBlockState(blockPosition()).getCollisionShape(level(), blockPosition(), CollisionContext.of(this));
        if (shape.toAabbs().stream().anyMatch(box -> box.move(blockPosition()).contains(start))) {
            explode(start); return;
        }
        BlockHitResult cancelledBlockHit = null;
        var hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            if (EventHooks.onProjectileImpact(this, hit)) {
                if (hit instanceof BlockHitResult block) cancelledBlockHit = block;
            } else onHit(hit);
        }
        if (isRemoved() || isBursting()) return;
        var requested = getDeltaMovement();
        move(MoverType.SELF, requested);
        if (horizontalCollision || verticalCollision) {
            var block = cancelledBlockHit != null ? cancelledBlockHit : ProjectileCollisionTools.findPhysicalBlockHit(this, start, requested);
            if (block == null) discard();
            else if (cancelledBlockHit != null || EventHooks.onProjectileImpact(this, block))
                ProjectileCollisionTools.continueAfterCancelledImpact(this, start, requested);
            else onHit(block);
        }
    }

    @Override protected boolean canHitEntity(@NotNull Entity entity) {
        return !isBursting() && isLiveTarget(CombatTools.resolutePartEntity(entity)) && super.canHitEntity(entity);
    }
    @Override protected void onHitEntity(@NotNull EntityHitResult hit) { explode(hit.getLocation()); }
    @Override protected void onHitBlock(@NotNull BlockHitResult hit) { explode(hit.getLocation()); }

    private void explode(Vec3 center) {
        if (level().isClientSide || isRemoved() || isBursting()) return;
        // 最初に演出状態へ遷移させ、直撃・物理接触・再入による二重ダメージを防ぐ。
        entityData.set(BURST, true);
        targetId = null;
        setDeltaMovement(Vec3.ZERO);
        setPos(center);
        var source = CombatTools.getDamageSource(level(), this, getOwner(), DamageTypes.LUNAR_AIM);
        var area = new AABB(center, center).inflate(getRadius());
        var candidates = level().getEntities(this, area, entity -> CombatTools.isValidCombatTarget(entity, getOwner()));
        for (var target : CombatTools.resolveUniqueCombatTargets(candidates)) {
            CombatTools.applyDamage(target, damage, source, SchoolRegistry.ICE.get(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
        }
        AudioTools.playSoundFromEntity(level(), this, SoundRegistry.STELLAR_EXPLODE.get(), SoundSource.PLAYERS, 1.5f, 0.92f, 0.04f);
    }

    private void spawnBurstParticles() {
        for (int i = 0; i < 8; i++) {
            double extent = getRadius() * Mth.lerp(random.nextFloat(), 0.82f, 1);
            double x = (random.nextDouble() * 2 - 1) * extent;
            double y = (random.nextDouble() * 2 - 1) * extent;
            double z = (random.nextDouble() * 2 - 1) * extent;
            switch (random.nextInt(6)) {
                case 0 -> x = extent;
                case 1 -> x = -extent;
                case 2 -> y = extent;
                case 3 -> y = -extent;
                case 4 -> z = extent;
                default -> z = -extent;
            }
            var velocity = new Vec3(x, y, z).normalize().scale(0.03 + random.nextDouble() * 0.14);
            level().addParticle(LunarParticles.createBurstSpark(0.20f + random.nextFloat() * 0.08f),
                    getX() + x, getY() + y, getZ() + z, velocity.x, velocity.y, velocity.z);
        }
    }

    @Override public @NotNull AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(isBursting() ? getRadius() + 1 : 2); }
    // 小さい弾体の既定描画距離では、散開を終える前に矢が見えなくなるため射程より長くする。
    @Override public boolean shouldRenderAtSqrDistance(double distanceSqr) { return distanceSqr < 160 * 160; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public void onAntiMagic(MagicData data) { if (!level().isClientSide) discard(); }
    @Override protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(BURST, false);
        builder.define(RADIUS, 2.5f);
    }
    @Override protected void readAdditionalSaveData(@NotNull CompoundTag tag) { super.readAdditionalSaveData(tag); }
    @Override protected void addAdditionalSaveData(@NotNull CompoundTag tag) { super.addAdditionalSaveData(tag); }
}
