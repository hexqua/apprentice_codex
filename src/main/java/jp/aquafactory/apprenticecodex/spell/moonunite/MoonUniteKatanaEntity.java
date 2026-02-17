package jp.aquafactory.apprenticecodex.spell.moonunite;

import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.nbt.CompoundTag;
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

public class MoonUniteKatanaEntity extends SummonWeaponEntity implements GeoEntity {

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
        // do nothing.
    }
    @Override
    public void onClientRemoval() {
        // todo:消失はちゃんと軸合わせをする.
        super.onClientRemoval();
    }

    @Override
    public void tick() {
        var level = level();

        super.tick();

        if (level.isClientSide) {
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        // todo:お試しにアニメ再生.
        if (tickCount == 5){
            triggerAnim("idle", "A");
        }
        if (tickCount == 40){
            triggerAnim("idle", "B");
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

        controllerRegistrar.add(new AnimationController<>(this, "idle", state -> {
            state.getController().setAnimation(ANIM_IDLE);
            return PlayState.CONTINUE;
        })
                .triggerableAnim("A", ANIM_TO_STANDBY)
                .triggerableAnim("B", ANIM_QUICKDRAW)
                .setAnimationSpeed(6.0f));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
