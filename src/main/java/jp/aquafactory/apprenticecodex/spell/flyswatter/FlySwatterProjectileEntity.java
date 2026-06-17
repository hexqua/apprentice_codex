package jp.aquafactory.apprenticecodex.spell.flyswatter;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerResolver;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerUuidHolder;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.Minecraft;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FlySwatterProjectileEntity extends Projectile implements AntiMagicSusceptible, CombatOwnerUuidHolder {
    private static final EntityDataAccessor<Integer> STANDBY_TICK =
            SynchedEntityData.defineId(FlySwatterProjectileEntity.class, EntityDataSerializers.INT);

    private static final int LIFE_TICKS = 20 * 10;
    private static final RandomSource RNG = RandomSource.create();
    private static final double SPEED_BASE = 1.5;
    private static final double SPEED_MAX = 2.5;
    private static final double SPEED_UP_PER_TICK = 0.01;
    private static final double ROTATION_DEG = 4;
    private static final double EXPLOSION_KNOCKBACK = 0.5;
    private static final double EXPLOSION_KNOCKBACK_UP = 0.2;
    private static final int OPEN_AIR_MODE_TICK = 20;

    private float damage;
    private float radius;
    private double speed;
    private int standbyTick;
    private Entity target;
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

    public void setProjectileVelocity(Vec3 rotation) {
        speed = SPEED_BASE;
        setDeltaMovement(rotation.scale(speed));
        ProjectileUtil.rotateTowardsMovement(this, 1);
    }

    public void setOpenAirMode() {
        setDeltaMovement(getDeltaMovement().scale(0.5));
        standbyTick = OPEN_AIR_MODE_TICK;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STANDBY_TICK, 0);
    }

    @Override
    public void tick() {
        var level = level();
        super.tick();

        if (!level.isClientSide) {
            if (tickCount > LIFE_TICKS) {
                discard();
            }

            var hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hitresult.getType() != HitResult.Type.MISS) {
                onHit(hitresult);
            }

            if(standbyTick > 0) {
                --standbyTick;
                entityData.set(STANDBY_TICK, standbyTick);

                if(standbyTick == 0) {
                    var targetPos = target.getBoundingBox().getCenter();
                    var targetVec = targetPos.subtract(position()).normalize();
                    setDeltaMovement(targetVec.scale(speed));
                } else if(target == null || target.isRemoved() || !target.isAlive()){
                    setDeltaMovement(getDeltaMovement().normalize().scale(speed));
                    standbyTick = 0;
                    entityData.set(STANDBY_TICK, standbyTick);
                } else {
                    var speed = Mth.lerp(standbyTick / (float)OPEN_AIR_MODE_TICK, 0.0f, 1.0f);
                    var angle = getDeltaMovement().normalize();
                    setDeltaMovement(angle.scale(speed));
                }
            } else {
                speed = Mth.clamp(speed + SPEED_UP_PER_TICK, SPEED_BASE, SPEED_MAX);
                if (target != null && !target.isRemoved() && target.isAlive()) {
                    var targetPos = target.getBoundingBox().getCenter();
                    var targetVec = targetPos.subtract(position()).normalize();
                    var newAngle = RotationTools.steerTowards(getDeltaMovement(), targetVec, ROTATION_DEG);
                    setDeltaMovement(newAngle.scale(speed));
                }
            }

            move(MoverType.SELF, getDeltaMovement());
            ProjectileUtil.rotateTowardsMovement(this, 1);
        }

        // 軌跡はクライアントでのみ.
        if (level.isClientSide && entityData.get(STANDBY_TICK) == 0 && tickCount > 2) {
            var camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            if (camPos.distanceToSqr(position()) < 32.0 * 32.0) {
                var count = 8;
                for (var i = 0; i < count; i++) {
                    var pos = position().subtract(getDeltaMovement().scale(RNG.nextDouble()));
                    EffectTools.createParticle(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, pos, 0, 0.001);
                }
            }
        }
    }

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

            server.playSound(null, BlockPos.containing(position), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 1.0f, 0.9f + level.random.nextFloat() * 0.2f);
        }


        // 判定.
        var aabb = new AABB(position, position).inflate(radius);
        var r2 = radius * radius;
        var targets = level.getEntitiesOfClass(Entity.class, aabb, e -> {
            if (!e.isAlive()) {
                return false;
            }

            // 直撃させた対象は爆風ダメージからは除外.
            if (e == directHitTarget) {
                return false;
            }

            // 自爆をさせるため、自分自身は判定に含められるようにする.
            return CombatTools.isValidCombatTarget(e, null);
        });

        var owner = CombatOwnerResolver.resolveCombatOwner(level, getOwner(), combatOwnerUuid);
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
            var hit = level.clip(new ClipContext(position, eye,ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            if (hit.getType() != HitResult.Type.MISS) {
                scale *= 0.5;
            }

            var finalDamage = (float)(damage * scale);
            CombatTools.applyDamage(e, finalDamage, source, SpellRegistry.FLY_SWATTER.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);

            // 爆風で吹き飛ばす.
            var dir = e.position().subtract(position);
            if (dir.lengthSqr() > 1.0e-6) {
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
        tag.putInt("standbyTick", standbyTick);
        saveCombatOwnerUuid(tag);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("damage");
        radius = tag.getFloat("radius");
        standbyTick = tag.getInt("standbyTick");
        loadCombatOwnerUuid(tag);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        return super.getAddEntityPacket(entity);
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

    public void setTarget(Entity target) {
        this.target = target;
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


