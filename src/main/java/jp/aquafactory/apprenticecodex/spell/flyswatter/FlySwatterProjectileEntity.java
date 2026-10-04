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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FlySwatterProjectileEntity extends Projectile implements AntiMagicSusceptible, CombatOwnerUuidHolder {
    private static final int LIFE_TICKS = 20 * 10;
    private static final double EXPLOSION_KNOCKBACK = 0.5;
    private static final double EXPLOSION_KNOCKBACK_UP = 0.2;

    private float damage;
    private float radius;
    private Entity target;
    private int arrivalTicks;
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
            else if (!ForgeEventFactory.onProjectileImpact(this, hit)) onHit(hit);
            if (isRemoved()) return;
        }
        setPos(muzzle);
        this.target = target;
        var plan = FlySwatterTrajectory.select(level(), this, muzzle, direction, target.getBoundingBox().getCenter(), shot);
        arrivalTicks = plan.arrivalTicks();
        velocity = plan.curve().startTangent().scale(1.0 / arrivalTicks);
        arrivalVelocity = plan.curve().endTangent().scale(1.0 / arrivalTicks);
        setDeltaMovement(velocity);
        ProjectileUtil.rotateTowardsMovement(this, 1);
    }

    @Override
    protected void defineSynchedData() {
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
        if (!(level() instanceof ServerLevel server)) return;
        if (tickCount > LIFE_TICKS) { discard(); return; }
        var start = position();
        var owner = CombatOwnerResolver.resolveCombatOwner(server, getOwner(), combatOwnerUuid);
        LockOnRayCurve step;
        if (LockOnRayCastData.isLoadedTarget(server, target) && CombatTools.isValidCombatTarget(target, owner)
                && tickCount <= arrivalTicks) {
            int remaining = arrivalTicks - tickCount + 1;
            var curve = new LockOnRayCurve(start, target.getBoundingBox().getCenter(),
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
        if (!isRemoved()) {
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
            var entity = ProjectileUtil.getEntityHitResult(level(), this, from, entityEnd,
                    new AABB(from, entityEnd).inflate(getBbWidth() * 0.5 + 0.3), this::canHitEntity);
            HitResult hit = entity != null ? entity : block == null ? null : block.hit();
            if (hit != null && !ForgeEventFactory.onProjectileImpact(this, hit)) {
                setPos(hit.getLocation());
                onHit(hit);
                if (isRemoved()) {
                    double part = from.distanceTo(to) < 1.0e-12 ? 0 : from.distanceTo(hit.getLocation()) / from.distanceTo(to);
                    return previousFraction + (sample.fraction() - previousFraction) * part;
                }
            }
            from = to;
            previousFraction = sample.fraction();
        }
        return 1;
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        var owner = CombatOwnerResolver.resolveCombatOwner(level(), getOwner(), combatOwnerUuid);
        return super.canHitEntity(entity) && CombatTools.isValidCombatTarget(CombatTools.resolutePartEntity(entity), owner);
    }

    public int getArrivalTicks() { return arrivalTicks; }

    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPushedByFluid(@NotNull FluidType type) { return false; }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        super.onHitEntity(hit);

        var level = level();
        if (level.isClientSide) {
            return;
        }

        var owner = CombatOwnerResolver.resolveCombatOwner(level(), getOwner(), combatOwnerUuid);
        if (CombatTools.isValidCombatTarget(hit.getEntity(), owner)) {
            var target = CombatTools.resolutePartEntity(hit.getEntity());
            var source = CombatOwnerResolver.createDamageSource(level(), this, getOwner(), combatOwnerUuid, DamageTypes.FLY_SWATTER);
            CombatTools.applyDamage(target, damage, source, SpellRegistry.FLY_SWATTER.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
            onImpact(level, target);
            discard();
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        super.onHitBlock(hit);

        var level = level();
        if (!level.isClientSide) {
            onImpact(level, null);
            discard();
        }
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (level().isClientSide || isRemoved()) {
            return;
        }

        fizzleByAntiMagic();
    }

    private void onImpact(Level level, Entity directHitTarget){
        var position = position();

        // パーティクルと音.
        if (level instanceof ServerLevel server){
            server.sendParticles(ParticleTypes.EXPLOSION, position.x, position.y, position.z,
                    1, 0.0, 0.0, 0.0, 0.0);
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, position.x, position.y, position.z,
                    1, 0.0, 0.0, 0.0, 0.0);

            var smokeSpread = radius * 0.35;
            server.sendParticles(ParticleTypes.LARGE_SMOKE, position.x, position.y, position.z,
                    25, smokeSpread, smokeSpread * 0.6, smokeSpread, 0.02);
            var poofSpread = radius * 0.2;
            server.sendParticles(ParticleTypes.POOF, position.x, position.y, position.z,
                    18, poofSpread, poofSpread * 0.4, poofSpread, 0.12);

            server.playSound(null, BlockPos.containing(position), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS, 1.0f, 0.9f + level.random.nextFloat() * 0.2f);
        }


        // 判定.
        var aabb = new AABB(position, position).inflate(radius);
        var r2 = radius * radius;
        var owner = CombatOwnerResolver.resolveCombatOwner(level, getOwner(), combatOwnerUuid);
        var targets = level.getEntitiesOfClass(Entity.class, aabb, e -> {
            if (!e.isAlive()) {
                return false;
            }

            // 直撃させた対象は爆風ダメージからは除外.
            if (e == directHitTarget) {
                return false;
            }

            return CombatTools.isValidCombatTarget(e, owner);
        });

        var source = CombatOwnerResolver.createDamageSource(level, this, getOwner(), combatOwnerUuid, DamageTypes.FLY_SWATTER);
        for (var e : targets) {
            var dist2 = e.distanceToSqr(position);
            if (dist2 > r2) {
                continue;
            }

            var dist = Math.sqrt(dist2);
            var t = dist / radius;
            var scale = 1.0 - t * t;
            if (scale <= 0) {
                continue;
            }

            var eye = e.getEyePosition();
            var hit = level.clip(new ClipContext(position, eye,ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    owner != null ? owner : this));
            if (hit.getType() != HitResult.Type.MISS) {
                scale *= 0.5;
            }

            var finalDamage = (float)(damage * scale);
            var damaged = CombatTools.applyDamage(e, finalDamage, source,
                    SpellRegistry.FLY_SWATTER.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);

            // 爆風で吹き飛ばす.
            var dir = e.position().subtract(position);
            if (damaged && dir.lengthSqr() > 1.0e-6) {
                dir = dir.normalize();
                e.push(dir.x * EXPLOSION_KNOCKBACK * scale, EXPLOSION_KNOCKBACK_UP * scale, dir.z * EXPLOSION_KNOCKBACK * scale);
            }
        }
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
        loadCombatOwnerUuid(tag);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4.0);
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
        radius = newRadius;
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
