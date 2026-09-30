package jp.aquafactory.apprenticecodex.spell.flyswatter;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.FlySwatterTrailPacket;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRayCastData;
import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRayCurve;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerResolver;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerUuidHolder;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.UUID;

public class FlySwatterProjectileEntity extends Projectile implements AntiMagicSusceptible, CombatOwnerUuidHolder {
    private static final int LIFE_TICKS = 20 * 10;
    private static final int BURST_TICKS = 14;
    private static final double ENTITY_HIT_MARGIN = 0.3;
    private static final EntityDataAccessor<Boolean> DATA_BURST =
            SynchedEntityData.defineId(FlySwatterProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(FlySwatterProjectileEntity.class, EntityDataSerializers.FLOAT);

    private float damage;
    private float radius;
    private Entity target;
    private Entity aimTarget;
    private int arrivalTicks;
    private int burstTicks;
    private int clientBurstTicks;
    private Vec3 velocity = Vec3.ZERO;
    private Vec3 arrivalVelocity = Vec3.ZERO;
    @Nullable
    private UUID combatOwnerUuid;

    public FlySwatterProjectileEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        setViewScale(8);
        setNoGravity(true);
    }

    public FlySwatterProjectileEntity(EntityType<? extends Projectile> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel);
        setViewScale(8);
        setOwner(owner);
        setCombatOwnerUuid(CombatOwnerResolver.captureCombatOwnerUuid(owner));
        setNoGravity(true);
    }

    public void launch(Vec3 launcherPosition, Vec3 muzzle, Vec3 direction, Entity target, int shot) {
        setPos(launcherPosition);
        var lead = muzzle.subtract(launcherPosition);
        var obstruction = FlySwatterTrajectory.firstObstruction(level(), this,
                new LockOnRayCurve(launcherPosition, muzzle, lead, lead));
        if (obstruction != null) {
            setPos(obstruction.position());
            var hit = obstruction.hit();
            if (obstruction.unloaded() || hit == null) discard();
            else if (!EventHooks.onProjectileImpact(this, hit)) onHit(hit);
            if (isRemoved() || isBursting()) return;
        }
        setPos(muzzle);
        this.target = target;
        aimTarget = selectAimTarget(target);
        var plan = FlySwatterTrajectory.select(level(), this, muzzle, direction, aimTarget.getBoundingBox().getCenter(), shot);
        arrivalTicks = plan.arrivalTicks();
        velocity = plan.curve().startTangent().scale(1.0 / arrivalTicks);
        arrivalVelocity = plan.curve().endTangent().scale(1.0 / arrivalTicks);
        setDeltaMovement(velocity);
        ProjectileUtil.rotateTowardsMovement(this, 1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(DATA_BURST, false);
        builder.define(DATA_RADIUS, 0.0f);
    }

    @Override
    public void tick() {
        if (isRemoved()) return;
        // Projectile / Entityの基底tickも現在位置のブロック・流体を読むため、先に拒否する。
        if (!level().isClientSide && !level().getChunkSource().hasChunk(blockPosition().getX() >> 4, blockPosition().getZ() >> 4)) {
            discard();
            return;
        }
        super.tick();
        if (isBursting()) {
            setDeltaMovement(Vec3.ZERO);
            if (level().isClientSide) ++clientBurstTicks;
            else if (++burstTicks >= BURST_TICKS) discard();
            return;
        }
        if (!(level() instanceof ServerLevel server)) return;
        if (tickCount > LIFE_TICKS) { discard(); return; }
        var start = position();
        var owner = CombatOwnerResolver.resolveCombatOwner(server, getOwner(), combatOwnerUuid);
        LockOnRayCurve step;
        if (LockOnRayCastData.isLoadedTarget(server, target) && CombatTools.isValidCombatTarget(target, owner)
                && tickCount <= arrivalTicks) {
            int remaining = arrivalTicks - tickCount + 1;
            if (aimTarget == null || aimTarget.isRemoved()) aimTarget = selectAimTarget(target);
            var curve = new LockOnRayCurve(start, aimTarget.getBoundingBox().getCenter(),
                    velocity.scale(remaining), arrivalVelocity.scale(remaining));
            step = curve.prefix(1.0 / remaining);
            velocity = step.endTangent();
        } else {
            target = null;
            velocity = getDeltaMovement();
            step = new LockOnRayCurve(start, start.add(velocity), velocity, velocity);
        }
        double fraction = moveAlongCurve(step);
        if (fraction > 0) {
            // 削除直前も、serverが通過した区間を独立して送る。
            Networks.sendToPlayersNear(server, start, 160, new FlySwatterTrailPacket(step.prefix(fraction)));
        }
        if (!isRemoved() && !isBursting()) {
            setPos(step.end());
            setDeltaMovement(position().subtract(start));
            ProjectileUtil.rotateTowardsMovement(this, 1);
            hasImpulse = true;
            if (tickCount >= LIFE_TICKS) discard();
        }
    }

    private double moveAlongCurve(LockOnRayCurve curve) {
        var from = curve.start();
        double previousFraction = 0;
        for (var sample : FlySwatterTrajectory.samples(curve)) {
            var to = sample.position();
            var block = FlySwatterTrajectory.traceSegment(level(), this, from, to);
            if (block != null && block.unloaded()) {
                setPos(from);
                discard();
                return previousFraction;
            }
            var entityEnd = block == null ? to : block.position();
            var entity = findEntityHit(from, entityEnd);
            HitResult hit = entity != null ? entity : block == null ? null : block.hit();
            if (hit != null && !EventHooks.onProjectileImpact(this, hit)) {
                setPos(hit.getLocation());
                onHit(hit);
                if (isRemoved() || isBursting()) {
                    double part = from.distanceTo(to) < 1.0e-12 ? 0
                            : Mth.clamp(from.distanceTo(hit.getLocation()) / from.distanceTo(to), 0, 1);
                    return previousFraction + (sample.fraction() - previousFraction) * part;
                }
            }
            from = to;
            previousFraction = sample.fraction();
        }
        return 1;
    }

    private @Nullable EntityHitResult findEntityHit(Vec3 from, Vec3 to) {
        var search = new AABB(from, to).inflate(getBbWidth() * 0.5 + ENTITY_HIT_MARGIN);
        EntityHitResult nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (var candidate : level().getEntities(this, search, this::canHitEntity)) {
            var box = candidate.getBoundingBox().inflate(ENTITY_HIT_MARGIN);
            // Level版ProjectileUtilは交点を失い、移動する部位が弾を包んだ場合も検出しない。
            var point = box.contains(from) ? from : box.clip(from, to).orElse(null);
            if (point == null) continue;
            double distance = from.distanceToSqr(point);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = new EntityHitResult(candidate, point);
            }
        }
        return nearest;
    }

    private static Entity selectAimTarget(Entity target) {
        Entity selected = target;
        if (!target.isMultipartEntity()) return selected;
        double nearest = Double.MAX_VALUE;
        var parts = target.getParts();
        if (parts == null) return selected;
        var center = target.getBoundingBox().getCenter();
        for (var part : parts) {
            if (part.isRemoved()) continue;
            double distance = part.getBoundingBox().getCenter().distanceToSqr(center);
            if (distance < nearest) {
                nearest = distance;
                selected = part;
            }
        }
        // ドラゴン本体の箱の中心は実部位の外にあるため、射出時に選んだ部位を追う。
        return selected;
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        var owner = CombatOwnerResolver.resolveCombatOwner(level(), getOwner(), combatOwnerUuid);
        var resolved = CombatTools.resolutePartEntity(entity);
        return !isBursting() && resolved.isAlive() && !resolved.isRemoved()
                && super.canHitEntity(entity) && CombatTools.isValidCombatTarget(resolved, owner);
    }

    public int getArrivalTicks() { return arrivalTicks; }

    public boolean isBursting() { return entityData.get(DATA_BURST); }

    public float getBurstCubeScale(float partialTicks) {
        float progress = Mth.clamp((clientBurstTicks + partialTicks) / 4.0f, 0, 1);
        float eased = 1 - (1 - progress) * (1 - progress) * (1 - progress);
        return Mth.lerp(eased, 0.45f, Math.max(0.45f, entityData.get(DATA_RADIUS) * 2));
    }

    public float getBurstCubeAlpha(float partialTicks) {
        float progress = Mth.clamp((clientBurstTicks + partialTicks - 5) / (BURST_TICKS - 5.0f), 0, 1);
        return 0.95f * (1 - progress * progress * progress);
    }

    public float getBurstSpinDegrees(float partialTicks) {
        return (clientBurstTicks + partialTicks) * 22 * (getId() % 2 == 0 ? 1 : -1);
    }

    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPushedByFluid(@NotNull FluidType type) { return false; }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        super.onHitEntity(hit);

        var level = level();
        if (level.isClientSide || isRemoved() || isBursting()) {
            return;
        }

        var owner = CombatOwnerResolver.resolveCombatOwner(level(), getOwner(), combatOwnerUuid);
        if (CombatTools.isValidCombatTarget(hit.getEntity(), owner)) {
            onImpact(hit.getLocation(), CombatTools.resolutePartEntity(hit.getEntity()));
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        super.onHitBlock(hit);

        var level = level();
        if (!level.isClientSide && !isRemoved() && !isBursting()) {
            onImpact(hit.getLocation(), null);
        }
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (level().isClientSide || isRemoved() || isBursting()) {
            return;
        }

        fizzleByAntiMagic();
    }

    private void onImpact(Vec3 center, @Nullable Entity directHitTarget) {
        entityData.set(DATA_BURST, true);
        burstTicks = 0;
        target = null;
        aimTarget = null;
        setPos(center);
        setDeltaMovement(Vec3.ZERO);
        hasImpulse = true;
        applyAreaDamage(center, directHitTarget);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0);
            server.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 0.95f, 1.15f + level().random.nextFloat() * 0.12f);
        }
    }

    private void applyAreaDamage(Vec3 center, @Nullable Entity directHitTarget) {
        var owner = CombatOwnerResolver.resolveCombatOwner(level(), getOwner(), combatOwnerUuid);
        var targets = new LinkedHashSet<Entity>();
        // 直撃も範囲ダメージと同じ集合で処理し、半径ゼロや部位の重なりでも一回だけ与える。
        if (directHitTarget != null) targets.add(directHitTarget);
        var area = new AABB(center, center).inflate(radius);
        for (var raw : level().getEntities(this, area, Entity::isAlive)) {
            // 本体の大きな箱だけで判定すると、実部位のない空間まで巻き込んでしまう。
            if (raw.isMultipartEntity()) continue;
            targets.add(CombatTools.resolutePartEntity(raw));
        }
        var source = CombatOwnerResolver.createDamageSource(level(), this, getOwner(), combatOwnerUuid, DamageTypes.FLY_SWATTER);
        for (var candidate : targets) {
            if (!candidate.isAlive() || candidate.isRemoved() || !CombatTools.isValidCombatTarget(candidate, owner)) continue;
            boolean multipart = candidate.isMultipartEntity();
            // 部位の接触地点から本体の目への遮蔽は、ドラゴン等の露出部位への命中まで拒否する。
            if (!multipart && candidate != directHitTarget && isBlockedByWall(center, candidate)) continue;
            CombatTools.applyDamage(candidate, damage, source, SpellRegistry.FLY_SWATTER.get().getSchoolType(),
                    multipart ? CombatTools.KnockbackTypes.NO_KNOCKBACK : CombatTools.KnockbackTypes.DEFAULT);
        }
    }

    private boolean isBlockedByWall(Vec3 center, Entity target) {
        var point = target instanceof LivingEntity living ? living.getEyePosition() : target.getBoundingBox().getCenter();
        return level().clip(new ClipContext(center, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() == HitResult.Type.BLOCK;
    }

    private void fizzleByAntiMagic() {
        var position = position();
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, position.x, position.y, position.z,
                    18, 0.25, 0.18, 0.25, 0.08);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, position.x, position.y, position.z,
                    8, 0.2, 0.12, 0.2, 0.01);
            server.playSound(null, BlockPos.containing(position), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 0.7f, 0.9f + level().random.nextFloat() * 0.2f);
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        tag.putFloat("radius", radius);
        saveCombatOwnerUuid(tag);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("damage");
        radius = tag.getFloat("radius");
        entityData.set(DATA_RADIUS, radius);
        loadCombatOwnerUuid(tag);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        return super.getAddEntityPacket(entity);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(isBursting() ? Math.max(4, entityData.get(DATA_RADIUS) * 1.5) : 4);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        double max = 128.0;
        return distanceSqr < max * max;
    }

    public void setDamage(float newDamage) {
        damage = newDamage;
    }

    public void setRadius(float newRadius) {
        radius = Math.max(0, newRadius);
        entityData.set(DATA_RADIUS, radius);
    }

    @Override
    public @Nullable UUID getCombatOwnerUuid() {
        return combatOwnerUuid;
    }

    @Override
    public void setCombatOwnerUuid(@Nullable UUID combatOwnerUuid) {
        this.combatOwnerUuid = combatOwnerUuid;
    }
}
