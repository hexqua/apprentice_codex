package jp.aquafactory.apprenticecodex.spell.moonunite;

import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.renderer.ISwordTrailEntity;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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

public class MoonUniteKatanaEntity extends SummonWeaponEntity implements GeoEntity, ISwordTrailEntity {

    private static final EntityDataAccessor<Float> ANIMATION_SPEED =
            SynchedEntityData.defineId(MoonUniteKatanaEntity.class, EntityDataSerializers.FLOAT);

    public static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenPlayAndHold("idle");
    public static final RawAnimation ANIM_TO_STANDBY = RawAnimation.begin().thenPlayAndHold("to_standby");
    public static final RawAnimation ANIM_QUICKDRAW = RawAnimation.begin().thenPlayAndHold("quickdraw");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    public MoonUniteKatanaEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public MoonUniteKatanaEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(ANIMATION_SPEED, 1.0f);
    }
    @Override
    public void onClientRemoval() {
        // todo:消失はちゃんと軸合わせをする.
        super.onClientRemoval();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        // todo:お試しにアニメ再生.
        if (tickCount == 5){
            triggerAnim("main", "standby");
            entityData.set(ANIMATION_SPEED, 2.0f);
        }
        if (tickCount == 40){
            triggerAnim("main", "quickdraw");
            entityData.set(ANIMATION_SPEED, 7.5f);
        }

        if (tickCount == 100){
            discard();
            return;
        }

        followTargetPosition(getStandbyPosition());
        setYRot(owner.getYRot());
        setXRot(0);
        setRot(getYRot(), getXRot());
    }

    @Override
    public boolean isTrailActive() {
        // todo:お試しに斬撃アニメちょっと前ぐらいに.
        return tickCount >= 39;
    }

    @Override
    public int getTrailColorARGB() {
        return 0xFFDDAAFF;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
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
