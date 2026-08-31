package jp.aquafactory.apprenticecodex.spell.thermalslice;

import io.redspace.ironsspellbooks.util.ParticleHelper;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.effect.ThermalSundered;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.GeoBonePoseCache;
import jp.aquafactory.apprenticecodex.renderer.ISwordTrailEntity;
import jp.aquafactory.apprenticecodex.utility.*;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ThermalSliceKatanaEntity extends SummonWeaponEntity implements GeoEntity, ISwordTrailEntity {

    private static final int STAY_SLASHED_TICK = 12;
    private static final double ATTACK_HALF_WIDTH = 0.5D;
    private static final double ATTACK_HALF_HEIGHT = 1.5D;
    private static final double ATTACK_DEPTH = 4.0D;
    private static final double FOLLOW_FORWARD_OFFSET = 0.2D;
    public static final String BLADE_CACHE_KEY = "default";

    private static final EntityDataAccessor<Boolean> SHOW_TRAIL =
            SynchedEntityData.defineId(ThermalSliceKatanaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> ANIMATION_SPEED =
            SynchedEntityData.defineId(ThermalSliceKatanaEntity.class, EntityDataSerializers.FLOAT);

    public static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenPlayAndHold("idle");
    public static final RawAnimation ANIM_TO_STANDBY = RawAnimation.begin().thenPlayAndHold("to_standby");
    public static final RawAnimation ANIM_QUICKDRAW = RawAnimation.begin().thenPlayAndHold("quickdraw");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float damage;
    private int thermalSunderedAmplifier;
    private int lifeTick = 0;
    private boolean isSlashed = false;
    private boolean isStandby = false;

    public ThermalSliceKatanaEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ThermalSliceKatanaEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
        moveToCollisionLimitedStandbyPosition(owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SHOW_TRAIL, false);
        builder.define(ANIMATION_SPEED, 1.0f);
    }
    @Override
    public void onClientRemoval() {
        spawnRemovalLineParticle(BLADE_CACHE_KEY);
        GeoBonePoseCache.remove(getUUID());

        super.onClientRemoval();
    }

    static public double getAttackDepth(){
        return ATTACK_DEPTH + FOLLOW_FORWARD_OFFSET;
    }

    private void spawnRemovalLineParticle(String cacheKey) {
        var pose = GeoBonePoseCache.getPrev(getUUID(), cacheKey);
        if (pose == null) {
            return;
        }

        // キャッシュはヨーを考慮できていないので補正をかける.
        // todo:多分共通化した方がよい.
        var yawDeg = RotationTools.calculateYawPitchByEntity(this, 1.0f).yaw();
        var yawRad = -yawDeg * Mth.DEG_TO_RAD;
        var rootLocal = pose.root().subtract(position());
        var tipLocal = pose.tip().subtract(position());
        var rootWorld = rootLocal.yRot(yawRad).add(position());
        var tipWorld = tipLocal.yRot(yawRad).add(position());
        EffectTools.createLineParticle(rootWorld, tipWorld, 0.25, 0.1, 0.01, ParticleHelper.FIRE, level());
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        if (isSlashed) {
            --lifeTick;
            if (lifeTick <= 0) {
                discard();
                return;
            }
        }

        refreshAttackPose(owner);
    }

    public void setStandby(){
        setStandby(1.0f);
    }

    public void setStandby(float castTimeSpeedScale){
        triggerAnim("main", "standby");
        entityData.set(ANIMATION_SPEED, 1.5f * Math.max(1.0f, castTimeSpeedScale));
        entityData.set(SHOW_TRAIL, false);
        isStandby = true;
    }

    public boolean isStandby(){
        return isStandby;
    }

    public float getAnimationSpeedForGameTest() {
        return entityData.get(ANIMATION_SPEED);
    }

    public void slash(Level level){
        if (isSlashed) {
            return;
        }
        if (!(getOwner() instanceof LivingEntity owner)) {
            return;
        }

        // 詠唱中の移動と旋回を抜刀時の攻撃範囲へ反映する。
        refreshAttackPose(owner);
        triggerAnim("main", "quickdraw");
        entityData.set(ANIMATION_SPEED, 1.5f);
        entityData.set(SHOW_TRAIL, true);
        lifeTick = STAY_SLASHED_TICK;
        isSlashed = true;

        var attackBox = new RaycastTools.HorizontalOrientedBox(
                position(),
                getLookAngle(),
                ATTACK_HALF_WIDTH,
                ATTACK_HALF_HEIGHT,
                ATTACK_DEPTH
        );
        var source = createCombatDamageSource(DamageTypes.THERMAL_SLICE);
        var hitResult = RaycastTools.hitsHorizontalOrientedBox(
                level,
                this,
                attackBox,
                e -> e != owner && CombatTools.isValidCombatTarget(e, owner)
        );
        AudioTools.playSoundFromEntity(level, this, SoundRegistry.THERMAL_SLICE.get(), SoundSource.PLAYERS);
        AudioTools.playSoundFromEntity(level, this, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS);
        for (var hit : hitResult){
            if (hit.blockOccluded()) {
                continue;
            }
            var applied = CombatTools.applyDamage(
                    hit.entity(),
                    damage,
                    source,
                    SpellRegistry.THERMAL_SLICE.get().getSchoolType(),
                    CombatTools.KnockbackTypes.DEFAULT
            );
            if (applied && hit.entity() instanceof LivingEntity livingTarget) {
                ThermalSunderedLogic.applyFromThermalSlice(livingTarget, thermalSunderedAmplifier);
            }
        }
    }

    public boolean isSlashed() {
        return isSlashed;
    }

    private void refreshAttackPose(LivingEntity owner) {
        moveToCollisionLimitedStandbyPosition(owner);
        setYRot(owner.getYRot());
        setXRot(0);
        setRot(getYRot(), getXRot());
    }

    private void moveToCollisionLimitedStandbyPosition(LivingEntity owner) {
        // 術者から攻撃起点までの薄い遮蔽物を飛び越さないよう、術者側から衝突を伴って移動する。
        var casterSidePosition = RotationTools.calculateBehindPosition(owner, 0.0D, 0.0D, -0.75D);
        var standbyPosition = getStandbyPosition();
        setPos(casterSidePosition.x, casterSidePosition.y, casterSidePosition.z);
        move(MoverType.SELF, standbyPosition.subtract(casterSidePosition));
    }

    public void setDamage(float damage){
        this.damage = damage;
    }

    public float getDamageForGameTest() {
        return damage;
    }

    public void setThermalSunderedAmplifier(int amplifier) {
        thermalSunderedAmplifier = ThermalSundered.clampAmplifier(amplifier);
    }

    public int getThermalSunderedAmplifierForGameTest() {
        return thermalSunderedAmplifier;
    }

    @Override
    public boolean isTrailActive() {
        return entityData.get(SHOW_TRAIL);
    }

    @Override
    public int getTrailColorARGB() {
        return 0xFFFFAA88;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        damage = pCompound.getFloat("Damage");
        setThermalSunderedAmplifier(pCompound.getInt("ThermalSunderedAmplifier"));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Damage", damage);
        pCompound.putInt("ThermalSunderedAmplifier", thermalSunderedAmplifier);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            // ピッチ追従は操作感を損ねるため意図的に行わず、水平方向の攻撃として扱う.
            // back方向なのでマイナスでforward方向になる.
            return RotationTools.calculateBehindPosition(owner, -FOLLOW_FORWARD_OFFSET, 0, -0.75);
        }

        return Vec3.ZERO;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

        controllerRegistrar.add(new AnimationController<>(this, "main", state -> {
                state.getController().setAnimation(ANIM_IDLE);
                return PlayState.CONTINUE;
            })
            .triggerableAnim("standby", ANIM_TO_STANDBY)
            .triggerableAnim("quickdraw", ANIM_QUICKDRAW)
            .setAnimationSpeedHandler(e -> (double)e.entityData.get(ANIMATION_SPEED)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
