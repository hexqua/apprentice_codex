package jp.aquafactory.apprenticecodex.spell.flyswatter;

import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRayCastData;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.ArrayList;
import java.util.List;

public class FlySwatterLauncherEntity extends SummonWeaponEntity {

    private static final int FIRE_START_DELAY_TICK = 10;
    private static final int FIRE_INTERVAL_TICK = 5;
    private static final float OPEN_AIR_PITCH_DEG = -60f;

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
    protected void defineSynchedData() {
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

        super.tick();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved() || owner.level() != level) {
            discard();
            return;
        }

        if (isFiring) {
            --fireIntervalTick;
            if (fireIntervalTick <= 0) {
                fireIntervalTick = FIRE_INTERVAL_TICK;
                if (!lockOnEntityList.isEmpty()) {
                    var target = lockOnEntityList.get(0);
                    lockOnEntityList.remove(0);
                    // 捕捉済みの枠は払い戻さず、無効な対象への射出だけを省く。
                    if (LockOnRayCastData.isLoadedTarget(level, target) && CombatTools.isValidCombatTarget(target, owner)) {
                        fire(level, target);
                    }
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

    public void setDamage(float damage){
        this.damage = damage;
    }

    public void setRadius(float radius){
        this.radius = radius;
    }

    public void addLockOnTarget(Entity target) {
        lockOnEntityList.add(CombatTools.resolutePartEntity(target));
    }

    public boolean isReleased() {
        return isReleased;
    }

    public int getLockOnCount() {
        return lockOnEntityList.size();
    }

    public void startFiring(Level level, LivingEntity owner) {
        if (isReleased || isFiring) return;
        lockOnEntityList.removeIf(target -> !(level instanceof ServerLevel server)
                || !LockOnRayCastData.isLoadedTarget(server, target) || !CombatTools.isValidCombatTarget(target, owner));
        if (lockOnEntityList.isEmpty()) {
            discard();
            return;
        }
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
