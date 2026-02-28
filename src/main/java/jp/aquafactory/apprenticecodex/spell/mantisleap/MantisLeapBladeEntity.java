package jp.aquafactory.apprenticecodex.spell.mantisleap;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.GeoBonePoseCache;
import jp.aquafactory.apprenticecodex.renderer.ISwordTrailEntity;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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

import java.util.List;

public class MantisLeapBladeEntity extends SummonWeaponEntity implements GeoEntity, ISwordTrailEntity {
    private static final int STAY_SLASHED_TICK = 10;
    public static final String TRAIL_1_CACHE_KEY = "trail1";
    public static final String TRAIL_2_CACHE_KEY = "trail2";

    private static final EntityDataAccessor<Boolean> SHOW_TRAIL =
            SynchedEntityData.defineId(MantisLeapBladeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> ANIMATION_SPEED =
            SynchedEntityData.defineId(MantisLeapBladeEntity.class, EntityDataSerializers.FLOAT);

    public static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenPlayAndHold("idle");
    public static final RawAnimation ANIM_SLASH = RawAnimation.begin().thenPlayAndHold("slash");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float damage;
    private int lifeTick;
    private boolean slashed;

    public MantisLeapBladeEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public MantisLeapBladeEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(SHOW_TRAIL, false);
        entityData.define(ANIMATION_SPEED, 1.0f);
    }

    @Override
    public void onClientRemoval() {
        spawnRemovalLineParticle(TRAIL_1_CACHE_KEY);
        spawnRemovalLineParticle(TRAIL_2_CACHE_KEY);
        GeoBonePoseCache.remove(getUUID());
        super.onClientRemoval();
    }

    private void spawnRemovalLineParticle(String cacheKey) {
        var pose = GeoBonePoseCache.getPrev(getUUID(), cacheKey);
        if (pose == null) {
            return;
        }

        var yawDeg = RotationTools.calculateYawPitchByEntity(this, 1.0f).yaw();
        var yawRad = -yawDeg * Mth.DEG_TO_RAD;
        var rootLocal = pose.root().subtract(position());
        var tipLocal = pose.tip().subtract(position());
        var rootWorld = rootLocal.yRot(yawRad).add(position());
        var tipWorld = tipLocal.yRot(yawRad).add(position());
        EffectTools.createLineParticle(rootWorld, tipWorld, 0.25, 0.1, 0.01, ParticleTypes.END_ROD, level());
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        if (slashed) {
            --lifeTick;
            if (lifeTick <= 0) {
                discard();
            }
            return;
        }

        followTargetPosition(getStandbyPosition());
        setYRot(owner.getYRot());
        setXRot(0);
        setRot(getYRot(), getXRot());
    }

    public void slash(Level level) {
        if (slashed) {
            return;
        }

        triggerAnim("main", "slash");
        entityData.set(ANIMATION_SPEED, 7.5f);
        entityData.set(SHOW_TRAIL, true);
        lifeTick = STAY_SLASHED_TICK;
        slashed = true;

        if (getOwner() instanceof LivingEntity owner) {
            var point = getLookAngle().normalize().scale(1.5);
            var source = CombatTools.getDamageSource(level, this, owner, DamageTypes.MANTIS_LEAP);
            var hitResult = RaycastTools.hitsSphere(
                    level,
                    position().add(point),
                    3.0,
                    e -> e != owner && CombatTools.isValidCombatTarget(e, owner)
            );
            AudioTools.playSoundFromEntity(level, this, SoundRegistry.KATANA_SLASH.get(), SoundSource.PLAYERS);
            AudioTools.playSoundFromEntity(level, this, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS);
            for (var hit : hitResult) {
                CombatTools.applyDamage(hit, damage, source, SpellRegistry.MANTIS_LEAP.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
            }
        }
    }

    public boolean isSlashed() {
        return slashed;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public boolean isTrailActive() {
        return entityData.get(SHOW_TRAIL);
    }

    @Override
    public int getTrailColorARGB() {
        return 0xFF4488FF;
    }

    @Override
    public List<TrailBonePair> getTrailBonePairs() {
        return List.of(
                new TrailBonePair(TRAIL_1_CACHE_KEY, "trail_tip1", "trail_root1"),
                new TrailBonePair(TRAIL_2_CACHE_KEY, "trail_tip2", "trail_root2")
        );
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        damage = pCompound.getFloat("Damage");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Damage", damage);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return RotationTools.calculateBehindPosition(owner, 0.0, 0.0, -0.35);
        }

        return Vec3.ZERO;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
                new AnimationController<>(this, "main", state -> {
                    state.getController().setAnimation(ANIM_IDLE);
                    return PlayState.CONTINUE;
                })
                        .triggerableAnim("slash", ANIM_SLASH)
                        .setAnimationSpeedHandler(e -> (double) e.entityData.get(ANIMATION_SPEED))
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
