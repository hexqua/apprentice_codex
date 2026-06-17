package jp.aquafactory.apprenticecodex.spell.dualacrobat;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.particle.MuzzleFlashParticleOptions;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class DualAcrobatSmgEntity extends SummonWeaponEntity {
    public static final double SIDE_OFFSET = 0.7;
    public static final double RENDER_Y_OFFSET = -0.15;
    public static final int SHOOTING_START_DELAY_TICKS = 10;
    public static final int SHOOTING_START_SETTLE_TICKS = 5;
    public static final float SPIN_DEGREES_PER_TICK = 72.0f;

    private static final double BACK_OFFSET = -0.5;
    private static final double Y_OFFSET = -0.25;
    private static final int FIRE_INTERVAL_TICKS = 2;
    private static final int RECOIL_DURATION_TICKS = 3;
    private static final int DISCARD_AFTER_EMPTY_TICKS = 10;

    private static final EntityDataAccessor<Boolean> IS_CHARGING =
            SynchedEntityData.defineId(DualAcrobatSmgEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> FORMATION_YAW =
            SynchedEntityData.defineId(DualAcrobatSmgEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> RIGHT_RECOIL_TICKS =
            SynchedEntityData.defineId(DualAcrobatSmgEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LEFT_RECOIL_TICKS =
            SynchedEntityData.defineId(DualAcrobatSmgEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHOOTING_START_DELAY_REMAINING =
            SynchedEntityData.defineId(DualAcrobatSmgEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SHOOTING_START_SPIN_DEGREES =
            SynchedEntityData.defineId(DualAcrobatSmgEntity.class, EntityDataSerializers.FLOAT);

    private float damage;
    private float range;
    private float loadAmmoCountSpeed;
    private int maximumLoadAmmoCount;
    private float loadedAmmoCount;
    private int remainingAmmoCount;
    private int shootingTick;
    private int discardTick;
    private boolean shooting;
    private boolean waitingDiscard;
    private boolean nextShotRight = true;
    private boolean nextLoadSoundRight = true;

    public DualAcrobatSmgEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public DualAcrobatSmgEntity(EntityType<?> entityType, Level level, LivingEntity owner) {
        super(entityType, level, owner);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(IS_CHARGING, true);
        entityData.define(FORMATION_YAW, 0.0f);
        entityData.define(RIGHT_RECOIL_TICKS, 0);
        entityData.define(LEFT_RECOIL_TICKS, 0);
        entityData.define(SHOOTING_START_DELAY_REMAINING, 0);
        entityData.define(SHOOTING_START_SPIN_DEGREES, 0.0f);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        range = tag.getFloat("Range");
        loadAmmoCountSpeed = tag.getFloat("LoadAmmoCountSpeed");
        maximumLoadAmmoCount = tag.getInt("MaximumLoadAmmoCount");
        loadedAmmoCount = tag.getFloat("LoadedAmmoCount");
        remainingAmmoCount = tag.getInt("RemainingAmmoCount");
        shootingTick = tag.getInt("ShootingTick");
        discardTick = tag.getInt("DiscardTick");
        shooting = tag.getBoolean("Shooting");
        waitingDiscard = tag.getBoolean("WaitingDiscard");
        nextShotRight = !tag.contains("NextShotRight") || tag.getBoolean("NextShotRight");
        nextLoadSoundRight = !tag.contains("NextLoadSoundRight") || tag.getBoolean("NextLoadSoundRight");
        entityData.set(IS_CHARGING, !shooting && !waitingDiscard);
        entityData.set(FORMATION_YAW, tag.getFloat("FormationYaw"));
        entityData.set(SHOOTING_START_DELAY_REMAINING, tag.getInt("ShootingStartDelayRemaining"));
        entityData.set(SHOOTING_START_SPIN_DEGREES, tag.getFloat("ShootingStartSpinDegrees"));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("Range", range);
        tag.putFloat("LoadAmmoCountSpeed", loadAmmoCountSpeed);
        tag.putInt("MaximumLoadAmmoCount", maximumLoadAmmoCount);
        tag.putFloat("LoadedAmmoCount", loadedAmmoCount);
        tag.putInt("RemainingAmmoCount", remainingAmmoCount);
        tag.putInt("ShootingTick", shootingTick);
        tag.putInt("DiscardTick", discardTick);
        tag.putBoolean("Shooting", shooting);
        tag.putBoolean("WaitingDiscard", waitingDiscard);
        tag.putBoolean("NextShotRight", nextShotRight);
        tag.putBoolean("NextLoadSoundRight", nextLoadSoundRight);
        tag.putFloat("FormationYaw", entityData.get(FORMATION_YAW));
        tag.putInt("ShootingStartDelayRemaining", getShootingStartDelayRemaining());
        tag.putFloat("ShootingStartSpinDegrees", entityData.get(SHOOTING_START_SPIN_DEGREES));
    }

    @Override
    public void onClientRemoval() {
        createStickParticleAtSide(true);
        createStickParticleAtSide(false);
        super.onClientRemoval();
    }

    @Override
    public void tick() {
        var level = level();
        if (level.isClientSide && firstTick) {
            createRingParticleAtSide(true);
            createRingParticleAtSide(false);
        }

        super.tick();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        tickRecoil();

        if (waitingDiscard) {
            ++discardTick;
            if (discardTick >= DISCARD_AFTER_EMPTY_TICKS) {
                discard();
            }
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        updateFormation(owner);
        if (!shooting) {
            setYRot(owner.getYRot());
            setXRot(0.0f);
            setRot(getYRot(), getXRot());
            hasImpulse = true;
            return;
        }

        var aimResult = RaycastTools.raycastFromEye(owner, range, 0.5, e -> CombatTools.isValidCombatTarget(e, this));
        faceTargetFrom(position(), aimResult.hitPosition());
        if (tickShootingStartDelay()) {
            return;
        }

        if (remainingAmmoCount <= 0) {
            beginDiscardCountdown();
            return;
        }

        if (shootingTick % FIRE_INTERVAL_TICKS == 0) {
            fire(level, owner, nextShotRight);
            nextShotRight = !nextShotRight;
            --remainingAmmoCount;
            if (remainingAmmoCount <= 0) {
                beginDiscardCountdown();
            }
        }
        ++shootingTick;
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return calculateCenterPosition(owner);
        }

        return Vec3.ZERO;
    }

    @Override
    public void releaseWeapon() {
        // 特殊挙動をさせない.
        discard();
    }

    public void setDamage(float newDamage) {
        damage = newDamage;
    }

    public void setRange(float newRange) {
        range = newRange;
    }

    public void setLoadAmmoCountSpeed(float newLoadAmmoCountSpeed) {
        loadAmmoCountSpeed = newLoadAmmoCountSpeed;
    }

    public void setMaximumLoadAmmoCount(int newMaximumLoadAmmoCount) {
        maximumLoadAmmoCount = Math.max(0, newMaximumLoadAmmoCount);
    }

    public void loadAmmo() {
        if (loadedAmmoCount >= maximumLoadAmmoCount) {
            loadedAmmoCount = maximumLoadAmmoCount;
            return;
        }

        loadedAmmoCount += loadAmmoCountSpeed / 20.0f;
        if (loadedAmmoCount >= maximumLoadAmmoCount) {
            loadedAmmoCount = maximumLoadAmmoCount;
            return;
        }

        if (tickCount % 2 == 0) {
            AudioTools.playSoundFromPosition(
                    level(),
                    getSidePosition(nextLoadSoundRight),
                    SoundRegistry.VANILLA_FEED_AMMO.get(),
                    SoundSource.PLAYERS,
                    0.8f,
                    1.0f
            );
            nextLoadSoundRight = !nextLoadSoundRight;
        }
    }

    public void startShooting() {
        shooting = true;
        remainingAmmoCount = Math.max(0, (int) loadedAmmoCount);
        shootingTick = 0;
        nextShotRight = true;
        entityData.set(IS_CHARGING, false);
        entityData.set(SHOOTING_START_DELAY_REMAINING, SHOOTING_START_DELAY_TICKS);
        entityData.set(SHOOTING_START_SPIN_DEGREES, Mth.positiveModulo(tickCount * SPIN_DEGREES_PER_TICK, 360.0f));
    }

    public void startCounterspellInterruptedShooting() {
        loadedAmmoCount = 0.0f;
        startShooting();
    }

    public boolean isCharging() {
        return entityData.get(IS_CHARGING);
    }

    public float getFormationYaw() {
        return entityData.get(FORMATION_YAW);
    }

    public float getLoadedAmmoCount() {
        return loadedAmmoCount;
    }

    public int getRemainingAmmoCount() {
        return remainingAmmoCount;
    }

    public int getRecoilTicks(boolean rightSide) {
        return entityData.get(rightSide ? RIGHT_RECOIL_TICKS : LEFT_RECOIL_TICKS);
    }

    public int getShootingStartDelayRemaining() {
        return entityData.get(SHOOTING_START_DELAY_REMAINING);
    }

    public float getShootingStartSpinDegrees() {
        return entityData.get(SHOOTING_START_SPIN_DEGREES);
    }

    private void fire(Level level, LivingEntity owner, boolean rightSide) {
        setRecoilTicks(rightSide);
        var firePosition = getSidePosition(rightSide);
        var aimResult = RaycastTools.raycastFromEye(owner, range, 0.5, e -> CombatTools.isValidCombatTarget(e, this));

        if (aimResult.hitEntity() != null) {
            var target = CombatTools.resolutePartEntity(aimResult.hitEntity());
            var source = createCombatDamageSource(DamageTypes.DUAL_ACROBAT);
            CombatTools.applyDamage(target, damage, source, SpellRegistry.DUAL_ACROBAT.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
        }

        if (level instanceof ServerLevel server) {
            var target = aimResult.hitPosition();
            var muzzlePosition = firePosition.add(getLookAngle().scale(0.5));
            server.sendParticles(new MuzzleFlashParticleOptions(0.5f), muzzlePosition.x, muzzlePosition.y, muzzlePosition.z, 0, 0, 0, 0, 0);

            switch (aimResult.hitType()) {
                case NONE:
                    break;
                case BLOCK:
                    server.sendParticles(ParticleTypes.SMOKE, target.x, target.y, target.z, 2, .05, .05, .05, .05);
                    break;
                case LIVING_ENTITY:
                    server.sendParticles(ParticleTypes.ENCHANTED_HIT, target.x, target.y, target.z, 6, .15, .15, .15, .1);
                    break;
            }
        }

        AudioTools.playSoundFromPosition(level, firePosition, SoundRegistry.SMG.get(), SoundSource.PLAYERS, 1.0f);
    }

    private void updateFormation(LivingEntity owner) {
        var center = calculateCenterPosition(owner);
        setDeltaMovement(Vec3.ZERO);
        setPos(center.x, center.y, center.z);
        entityData.set(FORMATION_YAW, owner.getYRot());
    }

    private Vec3 calculateCenterPosition(LivingEntity owner) {
        return RotationTools.calculateBehindPosition(owner, BACK_OFFSET, 0.0, Y_OFFSET);
    }

    public Vec3 getRenderedSidePosition(boolean rightSide) {
        return getSidePosition(rightSide).add(0.0, RENDER_Y_OFFSET, 0.0);
    }

    private Vec3 getSidePosition(boolean rightSide) {
        var sideSign = rightSide ? 1.0 : -1.0;
        return position().add(calculateSideOffset(entityData.get(FORMATION_YAW), sideSign));
    }

    public static Vec3 calculateSideOffset(float formationYaw, double sideSign) {
        var yawAngle = formationYaw * Mth.DEG_TO_RAD;
        var forwardX = -Mth.sin(yawAngle);
        var forwardZ = Mth.cos(yawAngle);
        var back = new Vec3(-forwardX, 0.0, -forwardZ).normalize();
        var right = new Vec3(back.z, 0.0, -back.x).normalize();
        return right.scale(SIDE_OFFSET * sideSign);
    }

    private void beginDiscardCountdown() {
        shooting = false;
        waitingDiscard = true;
        discardTick = 0;
        entityData.set(IS_CHARGING, false);
        entityData.set(SHOOTING_START_DELAY_REMAINING, 0);
        setDeltaMovement(Vec3.ZERO);
    }

    private void faceTargetFrom(Vec3 origin, Vec3 target) {
        var targetVec = target.subtract(origin);
        if (targetVec.lengthSqr() <= 1.0e-6) {
            return;
        }

        var yawPitch = RotationTools.calculateYawPitchByDirection(targetVec);
        setYRot(yawPitch.yaw());
        setXRot(yawPitch.pitch());
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    private void tickRecoil() {
        int rightTicks = entityData.get(RIGHT_RECOIL_TICKS);
        if (rightTicks > 0) {
            entityData.set(RIGHT_RECOIL_TICKS, rightTicks - 1);
        }

        int leftTicks = entityData.get(LEFT_RECOIL_TICKS);
        if (leftTicks > 0) {
            entityData.set(LEFT_RECOIL_TICKS, leftTicks - 1);
        }
    }

    private void setRecoilTicks(boolean rightSide) {
        entityData.set(rightSide ? RIGHT_RECOIL_TICKS : LEFT_RECOIL_TICKS, RECOIL_DURATION_TICKS);
    }

    private boolean tickShootingStartDelay() {
        var delayRemaining = getShootingStartDelayRemaining();
        if (delayRemaining <= 0) {
            return false;
        }

        entityData.set(SHOOTING_START_DELAY_REMAINING, delayRemaining - 1);
        return true;
    }

    private void createRingParticleAtSide(boolean rightSide) {
        EffectTools.createRingParticle(
                getRenderedSidePosition(rightSide),
                getLookAngle(),
                0.25f,
                10,
                0.01f,
                0.01,
                ParticleTypes.END_ROD,
                level()
        );
    }

    private void createStickParticleAtSide(boolean rightSide) {
        EffectTools.createStickParticle(
                getRenderedSidePosition(rightSide),
                getLookAngle(),
                0.7,
                10,
                0.04f,
                0.01,
                ParticleTypes.END_ROD,
                level()
        );
    }
}
