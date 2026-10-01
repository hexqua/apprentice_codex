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
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FlySwatterLauncherEntity extends SummonWeaponEntity {

    private static final int FIRE_START_DELAY_TICK = 10;
    private static final int FIRE_INTERVAL_TICK = 3;
    private static final byte EVENT_FIRE = 61;
    private static final float ELEVATED_PITCH_DEG = -60f;
    private static final double ELEVATED_TARGET_HEIGHT = 8;

    private float damage;
    private float radius;
    private final List<Entity> lockOnEntityList = new ArrayList<>();
    private int fireIntervalTick;
    private int fireDelayTick;
    private boolean isFiring;
    private boolean isReleased;
    private boolean elevatedLaunch;
    private float baseXRot;
    private int shotIndex;
    private int clientRecoilStartTick = -1;

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
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
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
    public void handleEntityEvent(byte event) {
        if (event == EVENT_FIRE) {
            // 発射ごとに再開し、連射しても描画上の反動を蓄積させない。
            clientRecoilStartTick = tickCount;
        } else {
            super.handleEntityEvent(event);
        }
    }

    public float getClientRecoilTicks(float partialTicks) {
        return clientRecoilStartTick < 0 ? -1.0f : tickCount - clientRecoilStartTick + partialTicks;
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

        var locatePosition = getAimingPosition(owner);
        followTargetPosition(locatePosition);
        if (isFiring) {
            if (fireDelayTick > 0) --fireDelayTick;
            float targetPitch = elevatedLaunch ? ELEVATED_PITCH_DEG : 0;
            setYRot(owner.getYRot());
            setXRot(Mth.lerp(fireDelayTick / (float) FIRE_START_DELAY_TICK, targetPitch, baseXRot));
        } else if (!isReleased) {
            setYRot(owner.getYRot());
            setXRot(owner.getXRot());
        }
        setRot(getYRot(), getXRot());
        hasImpulse = true;

        if (isFiring) {
            --fireIntervalTick;
            if (fireIntervalTick <= 0) {
                fireIntervalTick = FIRE_INTERVAL_TICK;
                if (!lockOnEntityList.isEmpty()) {
                    var target = lockOnEntityList.removeFirst();
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
    }

    private void fire(Level level, Entity target){
        if (!(getOwner() instanceof LivingEntity owner)){
            return;
        }

        var projectile = new FlySwatterProjectileEntity(EntityRegistry.FLY_SWATTER_PROJECTILE.get(),level, owner);
        projectile.setDamage(damage);
        projectile.setRadius(radius);
        projectile.launch(position(), position().add(getLookAngle()), getLookAngle(), target, shotIndex++);
        if (!projectile.isRemoved()) level.addFreshEntity(projectile);
        // 発射直後に壁へ着弾しても発射の反動は再生する。無効な捕捉枠ではここへ来ない。
        level.broadcastEntityEvent(this, EVENT_FIRE);
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
        // 天井ではなく、捕捉した敵の高度で構えを選ぶ。通せる軌道は各射出時に選択する。
        followTargetPosition(getAimingPosition(owner));
        var muzzle = position().add(getLookAngle());
        elevatedLaunch = lockOnEntityList.stream().anyMatch(target ->
                target.getBoundingBox().getCenter().y - muzzle.y >= ELEVATED_TARGET_HEIGHT);
        fireIntervalTick = FIRE_START_DELAY_TICK;
        fireDelayTick = FIRE_START_DELAY_TICK;
        isFiring = true;
        baseXRot = getXRot();
    }

    private static Vec3 getAimingPosition(LivingEntity owner) {
        return RotationTools.calculateBehindPosition(owner, -0.3, -0.9, 0.2);
    }
}
