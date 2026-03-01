package jp.aquafactory.apprenticecodex.spell.slashblade;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.GeoBonePoseCache;
import jp.aquafactory.apprenticecodex.renderer.ISwordTrailEntity;
import jp.aquafactory.apprenticecodex.utility.*;
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
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SlashBladeKatanaEntity extends SummonWeaponEntity implements GeoEntity, ISwordTrailEntity {

    private static final int STAY_SLASHED_TICK = 10;
    public static final String BLADE_CACHE_KEY = "default";
    public static final String SCABBARD_CACHE_KEY = "scabbard";

    private static final EntityDataAccessor<Boolean> SHOW_TRAIL =
            SynchedEntityData.defineId(SlashBladeKatanaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> ANIMATION_SPEED =
            SynchedEntityData.defineId(SlashBladeKatanaEntity.class, EntityDataSerializers.FLOAT);

    public static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenPlayAndHold("idle");
    public static final RawAnimation ANIM_TO_STANDBY = RawAnimation.begin().thenPlayAndHold("to_standby");
    public static final RawAnimation ANIM_QUICKDRAW = RawAnimation.begin().thenPlayAndHold("quickdraw");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float damage;
    private int lifeTick = 0;
    private boolean isSlashed = false;
    private boolean isStandby = false;

    public SlashBladeKatanaEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public SlashBladeKatanaEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SHOW_TRAIL, false);
        builder.define(ANIMATION_SPEED, 1.0f);
    }
    @Override
    public void onClientRemoval() {
        spawnRemovalLineParticle(BLADE_CACHE_KEY);
        spawnRemovalLineParticle(SCABBARD_CACHE_KEY);
        GeoBonePoseCache.remove(getUUID());

        super.onClientRemoval();
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
        EffectTools.createLineParticle(rootWorld, tipWorld, 0.25, 0.1, 0.01, ParticleTypes.END_ROD, level());
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

        followTargetPosition(getStandbyPosition());
        setYRot(owner.getYRot());
        setXRot(0);
        setRot(getYRot(), getXRot());
    }

    public void setStandby(){
        triggerAnim("main", "standby");
        entityData.set(ANIMATION_SPEED, 1.5f);
        entityData.set(SHOW_TRAIL, false);
        isStandby = true;
    }

    public boolean isStandby(){
        return isStandby;
    }

    public void slash(Level level){
        triggerAnim("main", "quickdraw");
        entityData.set(ANIMATION_SPEED, 5.0f);
        entityData.set(SHOW_TRAIL, true);
        lifeTick = STAY_SLASHED_TICK;
        isSlashed = true;

        if ((getOwner() instanceof LivingEntity owner)) {
            var point = getLookAngle().normalize().scale(0.75);
            var source = CombatTools.getDamageSource(level, this, owner, DamageTypes.SLASH_BLADE);
            var hitResult = RaycastTools.hitsSphere(level,
                    position().add(point),
                    2.5,
                    e -> e != owner && CombatTools.isValidCombatTarget(e, owner)
            );
            AudioTools.playSoundFromEntity(level, this, SoundRegistry.KATANA_SLASH.get(), SoundSource.PLAYERS);
            AudioTools.playSoundFromEntity(level, this, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS);
            for (var hit : hitResult){
                CombatTools.applyDamage(hit, damage, source, SpellRegistry.SLASH_BLADE.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
            }
        }
    }

    public void setDamage(float damage){
        this.damage = damage;
    }

    @Override
    public boolean isTrailActive() {
        return entityData.get(SHOW_TRAIL);
    }

    @Override
    public int getTrailColorARGB() {
        return 0xFFDDAAFF;
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
        // 鞘と刀身でセットのため、中央に出す.
        if (getOwner() instanceof LivingEntity owner) {
            return RotationTools.calculateBehindPosition(owner, 0, 0, -0.75);
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

