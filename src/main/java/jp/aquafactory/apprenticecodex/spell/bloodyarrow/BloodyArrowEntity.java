package jp.aquafactory.apprenticecodex.spell.bloodyarrow;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.ProjectileCollisionTools;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public final class BloodyArrowEntity extends Projectile implements AntiMagicSusceptible {
    public static final double SPEED = 2.5;
    public static final int LIFETIME = 200;
    private static final DustParticleOptions BLOOD_DUST =
            new DustParticleOptions(new Vector3f(1, 0, 0), 1);
    private float damage;
    private int orbCount;
    private float orbHealing;

    public BloodyArrowEntity(EntityType<? extends BloodyArrowEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void launch(LivingEntity owner, Vec3 origin, Vec3 direction, float damage, int orbCount, float orbHealing) {
        setOwner(owner);
        setPos(origin);
        setDeltaMovement(direction.normalize().scale(SPEED));
        ProjectileUtil.rotateTowardsMovement(this, 1);
        this.damage = damage;
        this.orbCount = orbCount;
        this.orbHealing = orbHealing;
    }

    @Override
    public void tick() {
        if (isRemoved()) return;
        // NoGravityの標準同期を使い、飛行途中から追跡したclientにも重力開始を伝える。
        if (!level().isClientSide && tickCount == 11) setNoGravity(false);
        super.tick();
        if (!level().isClientSide && tickCount >= LIFETIME) {
            discard();
            return;
        }
        BlockHitResult cancelledBlockHit = null;
        if (!level().isClientSide) {
            var block = blockPosition();
            var shape = level().getBlockState(block).getCollisionShape(level(), block, CollisionContext.of(this));
            // 始点が壁内の場合は中心線のraycastだけでは壁の外へ出られるため、移動前に止める。
            if (shape.toAabbs().stream().anyMatch(box -> box.move(block).contains(position()))) {
                discard();
                return;
            }
            var hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
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
        if (level().isClientSide) spawnTrail(start, position());
        // 最初の10回の移動を直進にし、11回目の移動後から下向き速度を加える。
        if (!isNoGravity()) setDeltaMovement(getDeltaMovement().add(0, -0.05, 0));
        ProjectileUtil.rotateTowardsMovement(this, 1);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        if (!(level() instanceof ServerLevel server) || isRemoved()) return;
        var target = CombatTools.resolutePartEntity(hit.getEntity());
        var owner = getOwner();
        setPos(hit.getLocation());
        if (CombatTools.isValidCombatTarget(target, owner)) {
            var wasAlive = target.isAlive();
            var applied = CombatTools.applyDamage(target, damage,
                    CombatTools.getDamageSource(level(), this, owner, DamageTypes.BLOODY_ARROW),
                    SchoolRegistry.BLOOD.get(), CombatTools.KnockbackTypes.DEFAULT);
            // End Crystalも攻撃できるが、血の反応は体力を持つ対象だけで起こす。
            if (applied && target instanceof LivingEntity && owner instanceof LivingEntity caster) {
                int count = orbCount * (wasAlive && !target.isAlive() ? 2 : 1);
                var anchor = BloodOrbPlacement.findAnchor(caster, hit.getLocation());
                double phase = random.nextDouble() * Math.PI * 2;
                for (int i = 0; i < count; i++) {
                    var orb = new BloodyArrowOrbEntity(EntityRegistry.BLOODY_ARROW_ORB.get(), server);
                    // 同方向に偏って一括回収できないよう、角度を等分して半径だけ揺らす。
                    var destination = anchor == null ? caster.position().add(0, 0.65, 0)
                            : BloodOrbPlacement.destination(caster, anchor, phase + Math.PI * 2 * i / count,
                                    3 + random.nextDouble() * 2);
                    var start = anchor != null && BloodOrbPlacement.clearLine(server, this, hit.getLocation(), anchor)
                            ? hit.getLocation() : (anchor != null ? anchor : destination);
                    orb.spawn(caster, start, orbHealing);
                    orb.scatter(anchor == null ? destination : anchor, destination);
                    server.addFreshEntity(orb);
                }
            }
        }
        discard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        if (!level().isClientSide) {
            setPos(hit.getLocation());
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        return CombatTools.isValidCombatTarget(CombatTools.resolutePartEntity(entity), getOwner())
                && super.canHitEntity(entity);
    }

    private void spawnTrail(Vec3 start, Vec3 end) {
        int count = Math.max(1, Math.min(12, (int) Math.ceil(start.distanceTo(end) * 2)));
        var spark = new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_SPARK.get(), 0.08F, 1, 0.05F, 0.05F, 0);
        for (int i = 0; i < count; i++) {
            var point = start.lerp(end, (i + random.nextDouble()) / count);
            level().addParticle(BLOOD_DUST, point.x, point.y, point.z, 0, 0, 0);
            if (i % 3 == 0) level().addParticle(spark, point.x, point.y, point.z,
                    (random.nextDouble() - 0.5) * 0.06, (random.nextDouble() - 0.5) * 0.06,
                    (random.nextDouble() - 0.5) * 0.06);
        }
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
