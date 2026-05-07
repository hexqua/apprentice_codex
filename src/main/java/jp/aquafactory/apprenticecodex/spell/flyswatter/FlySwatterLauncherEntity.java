package jp.aquafactory.apprenticecodex.spell.flyswatter;

import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FlySwatterLauncherEntity extends SummonWeaponEntity {

    private static final int FIRE_START_DELAY_TICK = 10;
    private static final int FIRE_INTERVAL_TICK = 5;
    private static final float OPEN_AIR_PITCH_DEG = -60f;

    private static final EntityDataAccessor<Integer> CASTING_TICK =
            SynchedEntityData.defineId(FlySwatterLauncherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> AIM_X =
            SynchedEntityData.defineId(FlySwatterLauncherEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_Y =
            SynchedEntityData.defineId(FlySwatterLauncherEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_Z =
            SynchedEntityData.defineId(FlySwatterLauncherEntity.class, EntityDataSerializers.FLOAT);

    private float damage;
    private float radius;
    private final List<Entity> lockOnEntityList = new ArrayList<>();
    private int fireIntervalTick;
    private int fireDelayTick;
    private boolean isFiring;
    private boolean isReleased;
    private boolean isOpenAir;
    private float baseXRot;

    public FlySwatterLauncherEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public FlySwatterLauncherEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if ((getOwner() instanceof LivingEntity owner)) {
            return getAimingPosition(owner);
        }

        return Vec3.ZERO;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CASTING_TICK, 0);
        builder.define(AIM_X, 0.0f);
        builder.define(AIM_Y, 0.0f);
        builder.define(AIM_Z, 0.0f);
    }

    @Override
    public void onClientRemoval(){
        var level = level();
        EffectTools.createStickParticle(
                position(),
                getLookAngle(),
                2,
                16,
                0.1f,
                0.02,
                ParticleTypes.END_ROD, level
        );

        super.onClientRemoval();
    }

    @Override
    public void releaseWeapon(){
        isReleased = true;
    }

    @Override
    public void tick() {
        var level = level();

        // 射出時パーティクル.
        if (level.isClientSide && firstTick) {
            EffectTools.createRingParticle(
                    position(),
                    getLookAngle(),
                    0.3f,
                    12,
                    0.015f,
                    0.01,
                    ParticleTypes.END_ROD,
                    level
            );
        }

        // todo:実行順の変化で見え方が変わるかもなので微調整.
        if (level.isClientSide) {
            int castingTick = entityData.get(CASTING_TICK);
            if (castingTick > 0) {
                var targetPosition = new Vec3(entityData.get(AIM_X), entityData.get(AIM_Y), entityData.get(AIM_Z));
                var targetVec = targetPosition.subtract(position());
                var reticleDistance = Math.min(4, targetVec.length() - 1);
                var reticlePosition = position().add(targetVec.normalize().scale(reticleDistance));
                EffectTools.createRingParticle(reticlePosition, targetVec, 0.25, 16, 0, 0, ParticleRegistry.RETICLE_DOT.get(), level);
            }
        }

        super.tick();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        if (isFiring) {
            --fireIntervalTick;
            if (fireIntervalTick <= 0) {
                fireIntervalTick = FIRE_INTERVAL_TICK;
                if (!lockOnEntityList.isEmpty()) {
                    var target = lockOnEntityList.get(0);
                    fire(level, target);
                    lockOnEntityList.remove(0);
                } else {
                    isFiring = false;
                }
            }
        } else if (isReleased) {
            discard();
        }

        var locatePosition = getAimingPosition(owner);
        followTargetPosition(locatePosition);

        if (isFiring && isOpenAir) {
            if(fireDelayTick > 0) {
                --fireDelayTick;
            }

            var pitch = Mth.lerp(fireDelayTick / (float) FIRE_START_DELAY_TICK, OPEN_AIR_PITCH_DEG, baseXRot);
            setYRot(owner.getYRot());
            setXRot(pitch);
        } else if (!isReleased) {
            setYRot(owner.getYRot());
            setXRot(owner.getXRot());
        }

        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    private void fire(Level level, Entity target){
        if (!(getOwner() instanceof LivingEntity owner)){
            return;
        }

        var projectile = new FlySwatterProjectileEntity(EntityRegistry.FLY_SWATTER_PROJECTILE.get(),level, owner);
        projectile.setPos(position().add(getLookAngle().scale(1f)));
        projectile.setDamage(damage);
        projectile.setRadius(radius);
        projectile.setProjectileVelocity(getLookAngle());
        projectile.setTarget(target);
        if (isOpenAir){
            projectile.setOpenAirMode();
        }

        level.addFreshEntity(projectile);
        AudioTools.playSoundFromEntity(level, this, SoundRegistry.VANILLA_PROJECTILE_SHOOT.get(), SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    public void setCastingReticleEffect(int tick, @Nullable Vec3 target) {
        if (entityData.get(CASTING_TICK) != tick) {
            entityData.set(CASTING_TICK, tick);
            if (target != null) {
                entityData.set(AIM_X, (float) target.x);
                entityData.set(AIM_Y, (float) target.y);
                entityData.set(AIM_Z, (float) target.z);
            }
        }
    }

    public void setDamage(float damage){
        this.damage = damage;
    }

    public void setRadius(float radius){
        this.radius = radius;
    }

    public void setLockOnEntityList(List<Integer> entityIdList, Level level) {
        lockOnEntityList.clear();
        for (var entityId : entityIdList) {
            var entity = level.getEntity(entityId);
            if (entity != null && entity.isAlive()){
                lockOnEntityList.add(entity);
            }
        }
    }

    public void startFiring(Level level, LivingEntity owner) {
        // ガラスの下ならちゃんと屋内判定にしないと自爆するため.
        var start = owner.getEyePosition();
        var end = start.add(0, level.getMaxBuildHeight() - start.y, 0);
        var ctx = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner);
        var hit = level.clip(ctx);
        isOpenAir = hit.getType() == HitResult.Type.MISS;
        fireIntervalTick = FIRE_START_DELAY_TICK;
        fireDelayTick = FIRE_START_DELAY_TICK;
        isFiring = true;
        baseXRot = getXRot();
    }

    private static Vec3 getAimingPosition(LivingEntity owner) {
        return RotationTools.calculateBehindPosition(owner, -0.3, -0.9, 0.2);
    }
}

