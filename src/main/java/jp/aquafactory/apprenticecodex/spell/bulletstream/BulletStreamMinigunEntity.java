package jp.aquafactory.apprenticecodex.spell.bulletstream;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.GunSpellTracerPacket;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BulletStreamMinigunEntity extends SummonWeaponEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation SPIN_UP_TO_FIRE = RawAnimation.begin().thenPlay("spinup").thenLoop("spining");
    private static final RawAnimation SPIN_FINISH = RawAnimation.begin().thenPlayAndHold("spin_finish");
    private static final int FIRING_SOUND_INTERVAL_TICKS = 10;
    private static final int RELEASE_DURATION_TICKS = 10;
    private static final float TRACER_SPEED_BLOCKS_PER_TICK = 24.0F;
    private static final float TRACER_LENGTH = 8.0F;

    private static final EntityDataAccessor<Boolean> IS_RECOIL_TICK =
            SynchedEntityData.defineId(BulletStreamMinigunEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_SPINNING_DOWN =
            SynchedEntityData.defineId(BulletStreamMinigunEntity.class, EntityDataSerializers.BOOLEAN);

    private float damage;
    private float range;
    private int spellLevel;

    private int currentTick;
    private int firingTick;
    private int spinUpDelayTick;
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_RECOIL_TICK, false);
        builder.define(IS_SPINNING_DOWN, false);
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
        if (currentTick >= spinUpDelayTick) {
            entityData.set(IS_RECOIL_TICK, true);
            fire(level);
            if (firingTick % FIRING_SOUND_INTERVAL_TICKS == 0) {
                AudioTools.playSoundFromEntity(level, this, SoundRegistry.MINIGUN_FIRING.get(), SoundSource.PLAYERS);
            }
            ++firingTick;
        }
    }

    @Override
    public void releaseWeapon(){
        if (isReleased) {
            return;
        }

        isReleased = true;
        releasedTick = RELEASE_DURATION_TICKS;
        entityData.set(IS_RECOIL_TICK, false);
        if (firingTick > 0) {
            entityData.set(IS_SPINNING_DOWN, true);
            if (level() instanceof ServerLevel serverLevel) {
                AudioTools.playSoundFromEntity(serverLevel, this, SoundRegistry.MINIGUN_FINISH.get(), SoundSource.PLAYERS);
            }
        }
    }

    private void fire(Level level) {
        var owner = getOwner();
        var hitResult = RaycastTools.raycastFromEye(owner, range, 0.5, e -> CombatTools.isValidCombatTarget(e, this) && e != owner);
        if (hitResult.hitEntity() != null) {
            var target = hitResult.hitEntity();
            var source = createCombatDamageSource(DamageTypes.BULLET_STREAM);
            CombatTools.applyDamage(target, resolveCurrentDamage(owner), source, SpellRegistry.BULLET_STREAM.get().getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
        }

        if (level instanceof ServerLevel server) {
            var firePosition = position().add(getLookAngle().normalize().scale(1));
            if (tickCount % 2 == 0) {
                server.sendParticles(new MuzzleFlashParticleOptions(1f), firePosition.x, firePosition.y, firePosition.z, 1, .05, .05, .05, 0);
            }

            var hitPosition = hitResult.hitPosition();
            if (hitResult.hitType() == RaycastTools.TargetType.LIVING_ENTITY) {
                server.sendParticles(ParticleTypes.CRIT, hitPosition.x, hitPosition.y, hitPosition.z, 1, .1, .1, .1, 0);
            }
            if (hitResult.hitType() == RaycastTools.TargetType.BLOCK && tickCount % 2 == 0) {
                server.sendParticles(ParticleTypes.SMOKE, hitPosition.x, hitPosition.y, hitPosition.z, 1, .1, .1, .1, 0);
            }

            // 完全一致で違和感が強いため、0.05ブロック範囲でランダムにずらす.
            // XYZ全てでやることで平面投影などの計算は省く. あくまで軽量にシンプルに.
            // 終着点もずらさないとズレたのにズレを考慮して収束するという違和感が出るため、演出だけもあり着弾点も見た目だけずらす.
            var tracerRecoilPosition = getRandomPositionRange(0.05f);
            Networks.sendToTrackingEntityAndSelf(owner, new GunSpellTracerPacket(
                    firePosition.add(tracerRecoilPosition),
                    hitPosition.add(tracerRecoilPosition),
                    TRACER_SPEED_BLOCKS_PER_TICK,
                    TRACER_LENGTH
            ));
        }
    }

    private Vec3 getRandomPositionRange(float range) {
        return new Vec3(getRandomFloatRange(range), getRandomFloatRange(range), getRandomFloatRange(range));
    }

    private float getRandomFloatRange(float range) {
        return (random.nextFloat() * 2 * range) - range;
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
    public void setSpinUpDelayTick(int spinUpDelayTick) {
        this.spinUpDelayTick = spinUpDelayTick;
    }
    public boolean getIsRecoilTick() {
        return entityData.get(IS_RECOIL_TICK);
    }
    public boolean isSpinningDown() {
        return entityData.get(IS_SPINNING_DOWN);
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
                    state.setAnimation(entityData.get(IS_SPINNING_DOWN) ? SPIN_FINISH : SPIN_UP_TO_FIRE);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}


