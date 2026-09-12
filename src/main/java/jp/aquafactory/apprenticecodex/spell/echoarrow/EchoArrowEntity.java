package jp.aquafactory.apprenticecodex.spell.echoarrow;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.ProjectileCollisionTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.List;

public final class EchoArrowEntity extends Projectile implements AntiMagicSusceptible {
    public static final double SPEED = 2.5;
    public static final int LIFETIME = 200;
    private static final EntityDataAccessor<Boolean> INITIAL = SynchedEntityData.defineId(EchoArrowEntity.class, EntityDataSerializers.BOOLEAN);
    private float damage;
    private long expiresAt;
    private boolean initial;
    private boolean impactReported;
    private EchoArrowCoreEntity core;

    public record TrailSample(Vec3 position, Vec3 direction) {
    }

    private final ArrayDeque<TrailSample> trail = new ArrayDeque<>();

    public List<TrailSample> trail() {
        return List.copyOf(trail);
    }

    public EchoArrowEntity(EntityType<? extends EchoArrowEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void launch(LivingEntity owner, Vec3 origin, Vec3 direction, float damage,
                       boolean initial, double speed, EchoArrowCoreEntity core) {
        setOwner(owner);
        setPos(origin);
        setDeltaMovement(direction.normalize().scale(speed));
        ProjectileUtil.rotateTowardsMovement(this, 1);
        this.damage = damage;
        expiresAt = level().getGameTime() + LIFETIME;
        this.initial = initial;
        entityData.set(INITIAL, initial);
        this.core = core;
    }

    @Override
    public void tick() {
        if (isRemoved()) return;
        // NoGravityの標準同期を使い、飛行途中から追跡したclientにも重力開始を伝える。
        if (!level().isClientSide && initial && tickCount == 11) setNoGravity(false);
        if (!level().isClientSide && !EchoArrowCoreEntity.validOwner(getOwner(), level())) {
            discard();
            return;
        }
        if (level().isClientSide) {
            trail.addFirst(new TrailSample(position(), getDeltaMovement()));
            while (trail.size() > 3) trail.removeLast();
        }
        super.tick();
        // entity tickの停止を跨いでも、期限切れの矢を再開させない。
        if (!level().isClientSide && (tickCount >= LIFETIME || level().getGameTime() >= expiresAt)) {
            discard();
            return;
        }
        BlockHitResult cancelledBlockHit = null;
        if (!level().isClientSide) {
            // 斜めのchunk境界では始点・終点以外のchunkもraycastが通る。
            var path = new AABB(position(), position().add(getDeltaMovement()));
            if (!level().hasChunksAt(BlockPos.containing(path.minX, path.minY, path.minZ),
                    BlockPos.containing(path.maxX, path.maxY, path.maxZ))) {
                discard();
                return;
            }
            var block = blockPosition();
            var shape = level().getBlockState(block).getCollisionShape(level(), block, CollisionContext.of(this));
            // 始点が壁内の場合は中心線のraycastだけでは壁の外へ出られるため、移動前に止める。
            if (shape.toAabbs().stream().anyMatch(box -> box.move(block).contains(position()))) {
                discard();
                return;
            }
            var hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            // ProjectileUtilのこのoverloadは交点を捨てて対象の足元を返す。
            // 同じ0.3Fの判定余白で交点を復元し、hookと追撃の照準へ渡す。
            if (hit instanceof EntityHitResult entityHit) {
                var point = entityHit.getEntity().getBoundingBox().inflate((double) 0.3F)
                        .clip(position(), position().add(getDeltaMovement())).orElse(position());
                hit = new EntityHitResult(entityHit.getEntity(), point);
            }
            if (hit.getType() != HitResult.Type.MISS) {
                if (EventHooks.onProjectileImpact(this, hit)) {
                    if (hit instanceof BlockHitResult blockHit) cancelledBlockHit = blockHit;
                } else {
                    onHit(hit);
                }
            }
            if (isRemoved()) return;
        }
        var start = position();
        var requested = getDeltaMovement();
        move(MoverType.SELF, requested);
        if (!level().isClientSide && (horizontalCollision || verticalCollision)) {
            var hit = cancelledBlockHit != null ? cancelledBlockHit
                    : ProjectileCollisionTools.findPhysicalBlockHit(this, start, requested);
            if (hit == null) {
                discard();
            } else if (cancelledBlockHit != null || EventHooks.onProjectileImpact(this, hit)) {
                ProjectileCollisionTools.continueAfterCancelledImpact(this, start, requested);
            } else {
                onHit(hit);
            }
        }
        if (isRemoved()) return;
        // 最初の10回の移動を直進にし、11回目の移動後から下向き速度を加える。
        if (!isNoGravity()) setDeltaMovement(getDeltaMovement().add(0, -0.05, 0));
        ProjectileUtil.rotateTowardsMovement(this, 1);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        if (level().isClientSide || isRemoved()) return;
        var target = CombatTools.resolutePartEntity(hit.getEntity());
        if (CombatTools.isValidCombatTarget(target, getOwner())) {
            CombatTools.applyDamage(target, damage,
                    CombatTools.getDamageSource(level(), this, getOwner(), DamageTypes.ECHO_ARROW),
                    SchoolRegistry.ELDRITCH.get(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
        }
        impact(hit.getLocation());
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        if (!level().isClientSide) impact(hit.getLocation());
    }

    private void impact(Vec3 point) {
        setPos(point);
        impactReported = true;
        if (initial && core != null && !core.isRemoved()) core.acceptImpact(this, point);
        discard();
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!level().isClientSide && initial && !impactReported && core != null && !core.isRemoved()) core.discard();
        core = null;
        trail.clear();
        super.remove(reason);
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        return CombatTools.isValidCombatTarget(CombatTools.resolutePartEntity(entity), getOwner())
                && super.canHitEntity(entity);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    // 小さい当たり判定に由来する描画距離で、飛行中の矢が近距離から消えるのを防ぐ。
    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 160 * 160;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void onAntiMagic(MagicData data) {
        if (!level().isClientSide) discard();
    }

    public boolean isInitial() {
        return entityData.get(INITIAL);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(INITIAL, false);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }
}
