package jp.aquafactory.apprenticecodex.spell.bulletstream;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.particle.MuzzleFlashParticleOptions;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.*;
import net.minecraft.core.particles.ParticleTypes;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BulletStreamMinigunEntity extends SummonWeaponEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation ROTATE = RawAnimation.begin().thenLoop("spin");
    private static final float MAX_SPIN_ANIMATION_SPEED = 4.0f;

    private static final EntityDataAccessor<Boolean> IS_RECOIL_TICK =
            SynchedEntityData.defineId(BulletStreamMinigunEntity.class, EntityDataSerializers.BOOLEAN);

    // クライアント処理に流すために公開.
    public static final EntityDataAccessor<Boolean> IS_SOUND_LOOP_MODE =
            SynchedEntityData.defineId(BulletStreamMinigunEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Float> SPIN_ANIMATION_SPEED =
            SynchedEntityData.defineId(BulletStreamMinigunEntity.class, EntityDataSerializers.FLOAT);

    private float damage;
    private float range;
    private int spellLevel;

    private int currentTick;
    private int currentWarmUpDelayTick;
    private int warmUpBaseDelay;
    private int warmUpStartTick;
    private int warmUpFinishTick;
    private boolean isStarted;
    private boolean isReleased;
    private int releasedTick;

    public BulletStreamMinigunEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    public BulletStreamMinigunEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(IS_RECOIL_TICK, false);
        entityData.define(IS_SOUND_LOOP_MODE, false);
        entityData.define(SPIN_ANIMATION_SPEED, 0.0f);
    }

    @Override
    public void onClientRemoval() {
        var level = level();
        EffectTools.createStickParticle(
                position(),
                getLookAngle(),
                1.5,
                12,
                0.1f,
                0.02,
                ParticleTypes.END_ROD,
                level
        );

        super.onClientRemoval();
    }

    @Override
    public void tick() {
        var level = level();

        // 射出時パーティクル(再ログインで消えるので制御不要)
        if (level.isClientSide && firstTick) {
            EffectTools.createRingParticle(
                    position(),
                    getLookAngle(),
                    0.2f,
                    8,
                    0.01f,
                    0.01,
                    ParticleTypes.END_ROD,
                    level
            );
        }

        super.tick();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (isReleased) {
            --releasedTick;
            if (entityData.get(IS_SOUND_LOOP_MODE) || isStarted) {
                isStarted = false;
                entityData.set(IS_SOUND_LOOP_MODE, false);
                AudioTools.playSoundFromEntity(level, this, SoundRegistry.MINIGUN_FINISH.get(), SoundSource.PLAYERS);
            }
            if (releasedTick <= 0) {
                discard();
            }
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        if (!isStarted){
            AudioTools.playSoundFromEntity(level, this, SoundRegistry.MINIGUN_WARMUP.get(), SoundSource.PLAYERS);
            isStarted = true;
        }

        var locatePosition = getStandbyPosition();
        followTargetPosition(locatePosition);

        // 常に視線先を狙う.
        var aimResult = RaycastTools.raycastFromEye(owner, 64, 1, e -> CombatTools.isValidCombatTarget(e, this));
        var targetVec = aimResult.hitPosition().subtract(position());
        var yawPitch = RotationTools.calculateYawPitchByDirection(targetVec);
        setYRot(yawPitch.yaw());
        setXRot(yawPitch.pitch());
        hasImpulse = true;

        ++currentTick;
        if (currentTick >= warmUpStartTick){
            if (currentTick >= warmUpFinishTick){
                if (!entityData.get(IS_RECOIL_TICK)) {
                    entityData.set(IS_RECOIL_TICK, true);
                }
                if (!entityData.get(IS_SOUND_LOOP_MODE)) {
                    entityData.set(IS_SOUND_LOOP_MODE, true);
                }
                fire(level, true);
            } else if (currentWarmUpDelayTick <= 0) {
                fire(level, entityData.get(IS_SOUND_LOOP_MODE));
                var warmUpTick = currentTick - warmUpStartTick;
                var warmUpDuration = warmUpFinishTick - warmUpStartTick;
                var t = Mth.clamp(warmUpTick, 0, warmUpDuration) / (float) warmUpDuration;
                currentWarmUpDelayTick = Mth.lerpInt(1-t, 0, warmUpBaseDelay);
                entityData.set(IS_RECOIL_TICK, true);
                if (currentWarmUpDelayTick <= 2 && !entityData.get(IS_SOUND_LOOP_MODE)) {
                    entityData.set(IS_SOUND_LOOP_MODE, true);
                }
            } else {
                --currentWarmUpDelayTick;
                if (entityData.get(IS_RECOIL_TICK)) {
                    entityData.set(IS_RECOIL_TICK, false);
                }
            }
        }

        if (currentTick <= warmUpFinishTick) {
            var currentSpinAnimationSpeed = Mth.lerp(currentTick / (float) warmUpFinishTick, 0.0f, MAX_SPIN_ANIMATION_SPEED);
            entityData.set(SPIN_ANIMATION_SPEED, currentSpinAnimationSpeed);
        }
    }

    @Override
    public void releaseWeapon(){
        isReleased = true;
        releasedTick = 10;
        entityData.set(IS_RECOIL_TICK, false);
    }

    private void fire(Level level, boolean isHighSpeedMode) {
        var owner = getOwner();
        var hitResult = RaycastTools.raycastFromEye(owner, range, 0.5, e -> CombatTools.isValidCombatTarget(e, this) && e != owner);
        if (hitResult.hitEntity() != null) {
            var target = hitResult.hitEntity();
            var source = CombatTools.getDamageSource(level, this, owner, DamageTypes.BULLET_STREAM);
            CombatTools.applyDamage(target, resolveCurrentDamage(owner), source, SpellRegistry.BULLET_STREAM.get().getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
        }

        if (level instanceof ServerLevel server) {
            if (!isHighSpeedMode || (tickCount % 2 == 0)) {
                var firePosition = position().add(getLookAngle().normalize().scale(1));
                server.sendParticles(new MuzzleFlashParticleOptions(1f), firePosition.x, firePosition.y, firePosition.z, 1, .05, .05, .05, 0);
            }

            var hitPosition = hitResult.hitPosition();
            if (hitResult.hitType() == RaycastTools.TargetType.LIVING_ENTITY) {
                server.sendParticles(ParticleTypes.CRIT, hitPosition.x, hitPosition.y, hitPosition.z, 1, .1, .1, .1, 0);
            }
            if (hitResult.hitType() == RaycastTools.TargetType.BLOCK && tickCount % 2 == 0) {
                server.sendParticles(ParticleTypes.SMOKE, hitPosition.x, hitPosition.y, hitPosition.z, 1, .1, .1, .1, 0);
            }
        }

        if (!isHighSpeedMode) {
            AudioTools.playSoundFromEntity(level, this, SoundRegistry.MINIGUN_SINGLE.get(), SoundSource.PLAYERS);
        }
    }

    @Override
    public Vec3 getStandbyPosition() {
        if ((getOwner() instanceof LivingEntity owner)) {
            return RotationTools.calculateBehindPosition(owner, -0.3, 1.0, -0.7);
        }

        return Vec3.ZERO;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }
    public void setSpellLevel(int spellLevel) {
        this.spellLevel = spellLevel;
    }
    public void setRange(float range) {
        this.range = range;
    }
    public void setTickSettings(int warmUpBaseDelay, int warmUpStartTick, int warmUpFinishTick) {
        this.warmUpBaseDelay = warmUpBaseDelay;
        this.warmUpStartTick = warmUpStartTick;
        this.warmUpFinishTick = warmUpFinishTick;
    }
    public boolean getIsRecoilTick() {
        return entityData.get(IS_RECOIL_TICK);
    }

    private float resolveCurrentDamage(net.minecraft.world.entity.Entity owner) {
        if (owner instanceof LivingEntity livingOwner && spellLevel > 0) {
            return BulletStream.getDamage(SpellRegistry.BULLET_STREAM.get().getSpellPower(spellLevel, livingOwner));
        }

        return damage;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
                this, "main", 0,
                state -> {
                    state.setAnimation(ROTATE);
                    state.getController().setAnimationSpeed(entityData.get(SPIN_ANIMATION_SPEED));
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

