package jp.aquafactory.apprenticecodex.spell.phalanxcharge;

import jp.aquafactory.apprenticecodex.effect.PhalanxStance;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.renderer.GeoBonePoseCache;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PhalanxWeaponryEntity extends SummonWeaponEntity implements GeoEntity {
    public static final String SHIELD_CENTER_CACHE_KEY = "shield_center";
    public static final String SPEAR_LINE_CACHE_KEY = "spear_line";

    private static final int SPAWN_POSE_STAY_TICK = 2;
    private static final int GUARD_FLASH_DURATION_TICKS = 6;
    private static final int GUARD_EFFECT_REFRESH_TICK = 5;
    private static final float GUARD_MOVE_SPEED_MULTIPLIER = 0.85f;

    private static final int ATTACK_STANDBY_DURATION_TICKS = 8;
    private static final int ATTACK_ROTATION_BLEND_TICKS = 5;
    private static final int THRUST_BEAM_DELAY_TICKS = 2;
    private static final int THRUST_STAY_TICKS = 8;

    private static final EntityDataAccessor<Integer> ANIMATION_STATE =
            SynchedEntityData.defineId(PhalanxWeaponryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GUARD_FLASH_SERIAL =
            SynchedEntityData.defineId(PhalanxWeaponryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ANIMATION_SPEED =
            SynchedEntityData.defineId(PhalanxWeaponryEntity.class, EntityDataSerializers.FLOAT);

    private static final RawAnimation ANIM_SPAWN = RawAnimation.begin().thenPlayAndHold("spawn");
    private static final RawAnimation ANIM_GUARD_STANCE = RawAnimation.begin().thenPlayAndHold("guard_stance");
    private static final RawAnimation ANIM_ATTACK_STANDBY = RawAnimation.begin().thenPlayAndHold("attack_standby");
    private static final RawAnimation ANIM_THRUST = RawAnimation.begin().thenPlayAndHold("thrust");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float damage;
    private float thrustBeamLength;
    private int attackStandbyTick;
    private int thrustStateTick;
    private int thrustLifeTick;
    private boolean thrustResolved;
    private boolean playMaxChargeThrustSound;
    private float attackBlendStartYaw;
    private float attackBlendStartPitch;

    private int clientLastFlashSerial = 0;
    private float clientFlashStartTick = -1.0f;

    public PhalanxWeaponryEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public PhalanxWeaponryEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(ANIMATION_STATE, AnimationState.SPAWN.id);
        entityData.define(GUARD_FLASH_SERIAL, 0);
        entityData.define(ANIMATION_SPEED, 1.0f);
    }

    @Override
    public void onClientRemoval() {
        spawnShieldRemovalParticle();
        spawnSpearRemovalParticle();
        GeoBonePoseCache.remove(getUUID());
        super.onClientRemoval();
    }

    private void spawnShieldRemovalParticle() {
        var pose = GeoBonePoseCache.getPrev(getUUID(), SHIELD_CENTER_CACHE_KEY);
        if (pose == null) {
            return;
        }

        var shieldCenterWorld = toWorldPositionFromCache(pose.tip());
        EffectTools.createSphereParticle(
                shieldCenterWorld,
                0.5,
                32,
                0.02,
                ParticleTypes.END_ROD,
                level()
        );
    }

    private void spawnSpearRemovalParticle() {
        var pose = GeoBonePoseCache.getPrev(getUUID(), SPEAR_LINE_CACHE_KEY);
        if (pose == null) {
            return;
        }

        var spearBottomWorld = toWorldPositionFromCache(pose.root());
        var spearTopWorld = toWorldPositionFromCache(pose.tip());
        EffectTools.createLineParticle(
                spearBottomWorld,
                spearTopWorld,
                0.2,
                0.08,
                0.02,
                ParticleTypes.END_ROD,
                level()
        );
    }

    private Vec3 toWorldPositionFromCache(Vec3 cachedPosition) {
        var yawDeg = RotationTools.calculateYawPitchByEntity(this, 1.0f).yaw();
        var yawRad = -yawDeg * Mth.DEG_TO_RAD;
        var localPosition = cachedPosition.subtract(position());
        return localPosition.yRot(yawRad).add(position());
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        followTargetPosition(getStandbyPosition());
        var animationState = AnimationState.of(entityData.get(ANIMATION_STATE));

        switch (animationState) {
            case SPAWN -> {
                applyGuardRotation(owner);
                if (tickCount >= SPAWN_POSE_STAY_TICK) {
                    entityData.set(ANIMATION_STATE, AnimationState.GUARD_STANCE.id);
                }
            }
            case GUARD_STANCE -> applyGuardRotation(owner);
            case ATTACK_STANDBY -> tickAttackStandby(level, owner);
            case THRUST -> tickThrust(level, owner);
        }

        hasImpulse = true;
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return RotationTools.calculateBehindPosition(owner, 0, 0, -0.25);
        }

        return Vec3.ZERO;
    }

    public void startThrustSequence(float damage, float thrustBeamLength, boolean playMaxChargeThrustSound) {
        this.damage = damage;
        this.thrustBeamLength = thrustBeamLength;
        this.playMaxChargeThrustSound = playMaxChargeThrustSound;
        attackStandbyTick = 0;
        thrustStateTick = 0;
        thrustLifeTick = THRUST_STAY_TICKS;
        thrustResolved = false;
        attackBlendStartYaw = getYRot();
        attackBlendStartPitch = getXRot();

        entityData.set(ANIMATION_STATE, AnimationState.ATTACK_STANDBY.id);
        entityData.set(ANIMATION_SPEED, 1.0f);
    }

    private void tickAttackStandby(Level level, LivingEntity owner) {
        attackStandbyTick++;
        applyAttackStandbyRotation(owner);
        applyGuardState(owner);

        if (attackStandbyTick < ATTACK_STANDBY_DURATION_TICKS) {
            return;
        }

        owner.removeEffect(EffectRegistry.PHALANX_STANCE.get());
        entityData.set(ANIMATION_STATE, AnimationState.THRUST.id);
        entityData.set(ANIMATION_SPEED, 4.0f);
        thrustStateTick = 0;
        thrustResolved = false;
        playThrustEntrySounds(level, playMaxChargeThrustSound);
    }

    private void tickThrust(Level level, LivingEntity owner) {
        setYRot(owner.getYRot());
        setXRot(owner.getXRot());
        setRot(getYRot(), getXRot());

        thrustStateTick++;
        if (!thrustResolved && thrustStateTick >= THRUST_BEAM_DELAY_TICKS) {
            spawnThrustBeam(level, owner);
            thrustResolved = true;
        }

        thrustLifeTick--;
        if (thrustLifeTick <= 0) {
            discard();
        }
    }

    private void applyGuardRotation(LivingEntity owner) {
        setYRot(owner.getYRot());
        setXRot(0.0f);
        setRot(getYRot(), getXRot());
    }

    private void applyAttackStandbyRotation(LivingEntity owner) {
        if (attackStandbyTick > ATTACK_ROTATION_BLEND_TICKS) {
            setYRot(owner.getYRot());
            setXRot(owner.getXRot());
            setRot(getYRot(), getXRot());
            return;
        }

        var blendProgress = Mth.clamp(attackStandbyTick / (float) ATTACK_ROTATION_BLEND_TICKS, 0.0f, 1.0f);
        var blendedYaw = Mth.rotLerp(blendProgress, attackBlendStartYaw, owner.getYRot());
        var blendedPitch = Mth.lerp(blendProgress, attackBlendStartPitch, owner.getXRot());
        setYRot(blendedYaw);
        setXRot(blendedPitch);
        setRot(getYRot(), getXRot());
    }

    private void applyGuardState(LivingEntity owner) {
        var amplifier = PhalanxStance.toAmplifier(GUARD_MOVE_SPEED_MULTIPLIER);
        owner.addEffect(new MobEffectInstance(
                EffectRegistry.PHALANX_STANCE.get(),
                GUARD_EFFECT_REFRESH_TICK,
                amplifier,
                false,
                false,
                true
        ));
    }

    private void spawnThrustBeam(Level level, LivingEntity owner) {
        var look = getLookAngle().normalize();
        var up = new Vec3(0.0, 1.0, 0.0);
        var right = look.cross(up);
        if (right.lengthSqr() < 1.0e-6) {
            right = Vec3.directionFromRotation(0.0f, getYRot()).normalize().cross(up);
        }

        var beam = new PhalanxChargeBeamEntity(EntityRegistry.PHALANX_CHARGE_BEAM.get(), level, owner);
        var beamStart = position().add(getLookAngle().scale(1.0)).add(right.normalize().scale(0.25)).add(up.scale(0.1));

        beam.moveTo(beamStart.x, beamStart.y, beamStart.z, getYRot(), getXRot());
        beam.setup(level, Math.max(0.1f, thrustBeamLength), 0.5f, damage);
        level.addFreshEntity(beam);
    }

    private void playThrustEntrySounds(Level level, boolean playThrustSound) {
        AudioTools.playSoundFromEntity(level, this, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS);
        if (playThrustSound) {
            AudioTools.playSoundFromEntity(level, this, SoundRegistry.THRUST.get(), SoundSource.PLAYERS);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        damage = pCompound.getFloat("Damage");
        thrustBeamLength = pCompound.getFloat("ThrustBeamLength");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Damage", damage);
        pCompound.putFloat("ThrustBeamLength", thrustBeamLength);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
                this, "main", 0,
                state -> {
                    var animationState = AnimationState.of(entityData.get(ANIMATION_STATE));
                    switch (animationState) {
                        case SPAWN -> state.setAnimation(ANIM_SPAWN);
                        case GUARD_STANCE -> state.setAnimation(ANIM_GUARD_STANCE);
                        case ATTACK_STANDBY -> state.setAnimation(ANIM_ATTACK_STANDBY);
                        case THRUST -> state.setAnimation(ANIM_THRUST);
                    }
                    return PlayState.CONTINUE;
                }
        ).setAnimationSpeedHandler(e -> (double) e.entityData.get(ANIMATION_SPEED)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        //noinspection resource
        if (!level().isClientSide || !GUARD_FLASH_SERIAL.equals(key)) {
            return;
        }

        var serial = entityData.get(GUARD_FLASH_SERIAL);
        if (serial == clientLastFlashSerial) {
            return;
        }

        clientLastFlashSerial = serial;
        clientFlashStartTick = tickCount;
    }

    public void triggerGuardFlash(Level level) {
        if (!level.isClientSide) {
            entityData.set(GUARD_FLASH_SERIAL, entityData.get(GUARD_FLASH_SERIAL) + 1);
        }
    }

    public float getGuardFlashStrength(float partialTick) {
        //noinspection resource
        if (!level().isClientSide || clientFlashStartTick < 0.0f) {
            return 0.0f;
        }

        var elapsed = (tickCount + partialTick) - clientFlashStartTick;
        var progress = Mth.clamp(elapsed / GUARD_FLASH_DURATION_TICKS, 0.0f, 1.0f);
        var inverse = 1.0f - progress;
        return inverse * inverse * inverse;
    }

    private enum AnimationState {
        SPAWN(0),
        GUARD_STANCE(1),
        ATTACK_STANDBY(2),
        THRUST(3);

        private final int id;

        AnimationState(int id) {
            this.id = id;
        }

        private static AnimationState of(int rawId) {
            return switch (rawId) {
                case 1 -> GUARD_STANCE;
                case 2 -> ATTACK_STANDBY;
                case 3 -> THRUST;
                default -> SPAWN;
            };
        }
    }
}
