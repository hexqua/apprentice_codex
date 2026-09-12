package jp.aquafactory.apprenticecodex.spell.sacredarrow;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.particle.StellarTrailParticles;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.ProjectileCollisionTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import net.minecraft.util.Mth;
import java.util.*;

public final class SacredArrowEntity extends Projectile implements AntiMagicSusceptible {
    public static final double SPEED = 2;
    public static final int LIFETIME = 200;
    private float damage;
    private float penetrateMultiplier;
    private int duration;
    private boolean chasing;
    private UUID targetId;
    private int expiresAt = LIFETIME;
    private final Set<UUID> damaged = new HashSet<>();

    public SacredArrowEntity(EntityType<? extends SacredArrowEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void launch(LivingEntity owner, Vec3 origin, Vec3 direction, float damage,
                       float penetrateMultiplier, int duration, @Nullable LivingEntity target) {
        setOwner(owner);
        setPos(origin);
        setDeltaMovement(direction.normalize().scale(SPEED));
        this.damage = damage;
        this.penetrateMultiplier = penetrateMultiplier;
        this.duration = duration;
        this.chasing = target != null;
        this.targetId = target == null ? null : target.getUUID();
        ProjectileUtil.rotateTowardsMovement(this, 1);
    }

    public static boolean isLiveTarget(@Nullable Entity target, @Nullable Entity owner) {
        return target instanceof LivingEntity living && !living.isRemoved() && !living.isDeadOrDying()
                && living.isAlive() && (owner == null || target.level() == owner.level())
                && CombatTools.isValidCombatTarget(living, owner);
    }

    public boolean isChasing() { return chasing; }

    @Override public void tick() {
        if (isRemoved()) return;
        if (!level().isClientSide && !chasing && tickCount == 11) setNoGravity(false);
        super.tick();
        var start = position();
        if (!level().isClientSide) {
            if (tickCount >= expiresAt) { discard(); return; }
            if (chasing) {
                updateHoming();
                hitAlongPath(start, start.add(getDeltaMovement()));
                if (!isRemoved()) setPos(start.add(getDeltaMovement()));
            } else {
                tickNormal();
            }
        } else {
            // 曲線の位置と速度はserverの毎tick同期を使い、clientで対象を再選出しない。
            setPos(start.add(getDeltaMovement()));
            var end = position();
            StellarTrailParticles.spawn(this, random, end, 0.05, 0.12f);
            StellarTrailParticles.spawn(this, random, end, 0.05, 0.12f);
            int count = Math.min(6, (int) Math.floor(start.distanceTo(end) / 0.22));
            for (int i = 1; i <= count; i++) {
                StellarTrailParticles.spawn(this, random, start.lerp(end, i / (double) (count + 1)), 0.03, 0.1f);
            }
        }
        if (isRemoved()) return;
        if (!isNoGravity()) setDeltaMovement(getDeltaMovement().add(0, -0.05, 0));
        ProjectileUtil.rotateTowardsMovement(this, 1);
    }

    private void updateHoming() {
        if (targetId == null) return;
        var target = ((ServerLevel) level()).getEntity(targetId);
        if (!isLiveTarget(target, getOwner())) {
            targetId = null;
            expiresAt = Math.min(expiresAt, tickCount + 60);
            return;
        }
        var delta = target.getBoundingBox().getCenter().subtract(position());
        if (delta.lengthSqr() < 1.0e-10) return;
        var desired = delta.normalize();
        var current = getDeltaMovement().normalize();
        double angle = Math.acos(Mth.clamp(current.dot(desired), -1, 1));
        double maxTurn = Math.toRadians(6 + 24 * Math.min(tickCount / 20.0, 1));
        // 接近後は中心へ収束させ、低速旋回で対象の周囲を回り続けることを防ぐ。
        if (angle <= maxTurn || delta.length() <= SPEED * 2 || current.lengthSqr() < 0.5) {
            setDeltaMovement(desired.scale(SPEED));
        } else {
            var tangent = desired.subtract(current.scale(current.dot(desired)));
            // 真後ろでは外積が退化するため、安定した垂直方向を選ぶ。
            if (tangent.lengthSqr() < 1.0e-10) {
                tangent = current.cross(Math.abs(current.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0));
            }
            setDeltaMovement(current.scale(Math.cos(maxTurn)).add(tangent.normalize().scale(Math.sin(maxTurn))).normalize().scale(SPEED));
        }
    }

    private void hitAlongPath(Vec3 start, Vec3 end) {
        var hits = new ArrayList<EntityHitResult>();
        // ブロックで打ち切らず全候補を取得する。大型mobの部位はダメージ時に親へ統合する。
        for (var candidate : level().getEntities(this, getBoundingBox().expandTowards(end.subtract(start)).inflate(1), this::canHitEntity)) {
            var box = candidate.getBoundingBox().inflate(0.3);
            var point = box.contains(start) ? Optional.of(start) : box.clip(start, end);
            point.ifPresent(p -> hits.add(new EntityHitResult(candidate, p)));
        }
        hits.sort(Comparator.comparingDouble(hit -> start.distanceToSqr(hit.getLocation())));
        var attempted = new HashSet<UUID>();
        for (var hit : hits) {
            var target = CombatTools.resolutePartEntity(hit.getEntity());
            if (!attempted.add(target.getUUID()) || !canHitEntity(hit.getEntity())) continue;
            if (!EventHooks.onProjectileImpact(this, hit)) onHit(hit);
            if (isRemoved()) return;
        }
    }

    private void tickNormal() {
        var start = position();
        var shape = level().getBlockState(blockPosition()).getCollisionShape(level(), blockPosition(), CollisionContext.of(this));
        if (shape.toAabbs().stream().anyMatch(box -> box.move(blockPosition()).contains(start))) {
            impact(start); discard(); return;
        }
        BlockHitResult cancelledBlockHit = null;
        var hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            if (EventHooks.onProjectileImpact(this, hit)) {
                if (hit instanceof BlockHitResult block) cancelledBlockHit = block;
            } else onHit(hit);
        }
        if (isRemoved()) return;
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

    @Override protected void onHitEntity(@NotNull EntityHitResult hit) {
        if (level().isClientSide || isRemoved()) return;
        var target = CombatTools.resolutePartEntity(hit.getEntity());
        if (!CombatTools.isValidCombatTarget(target, getOwner()) || damaged.contains(target.getUUID())) return;
        boolean primary = target.getUUID().equals(targetId);
        boolean applied = CombatTools.applyDamage(target, damage * (chasing && !primary ? penetrateMultiplier : 1),
                CombatTools.getDamageSource(level(), this, getOwner(), DamageTypes.SACRED_ARROW),
                SchoolRegistry.HOLY.get(), CombatTools.KnockbackTypes.DEFAULT);
        if (applied) {
            damaged.add(target.getUUID());
            if (!chasing && target instanceof LivingEntity living && living.isAlive() && !living.isDeadOrDying()) {
                living.addEffect(new MobEffectInstance(EffectRegistry.SACRED_SIGN, duration));
            }
        }
        impact(hit.getLocation());
        if (!chasing || primary) { setPos(hit.getLocation()); discard(); }
    }

    @Override protected void onHitBlock(@NotNull BlockHitResult hit) {
        if (!level().isClientSide && !chasing) { impact(hit.getLocation()); setPos(hit.getLocation()); discard(); }
    }

    private void impact(Vec3 point) {
        MagicManager.spawnParticles(level(), ParticleHelper.WISP, point.x, point.y, point.z, 25, 0, 0, 0, .18, true);
        level().playSound(null, point.x, point.y, point.z,
                io.redspace.ironsspellbooks.registries.SoundRegistry.GUIDING_BOLT_IMPACT.value(), SoundSource.NEUTRAL, 2, 0.9f + random.nextFloat() * .4f);
    }

    @Override protected boolean canHitEntity(@NotNull Entity entity) {
        var target = CombatTools.resolutePartEntity(entity);
        return target.isAlive() && !damaged.contains(target.getUUID())
                && CombatTools.isValidCombatTarget(target, getOwner()) && super.canHitEntity(entity);
    }
    // 小さい当たり判定に由来する描画距離で、飛行中の矢が近距離から消えるのを防ぐ。
    @Override public boolean shouldRenderAtSqrDistance(double distanceSqr) { return distanceSqr < 160 * 160; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public void onAntiMagic(MagicData data) { if (!level().isClientSide) discard(); }
    @Override protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {}
    @Override protected void readAdditionalSaveData(@NotNull CompoundTag tag) { super.readAdditionalSaveData(tag); }
    @Override protected void addAdditionalSaveData(@NotNull CompoundTag tag) { super.addAdditionalSaveData(tag); }
}
