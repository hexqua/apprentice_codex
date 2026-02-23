package jp.aquafactory.apprenticecodex.spell.phalanxcharge;

import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.util.Mth;
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

public class PhalanxWeaponryEntity extends SummonWeaponEntity implements GeoEntity {
    private static final int SPAWN_POSE_STAY_TICK = 2;
    private static final int GUARD_FLASH_DURATION_TICKS = 6;

    private static final EntityDataAccessor<Integer> ANIMATION_STATE =
            SynchedEntityData.defineId(PhalanxWeaponryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GUARD_FLASH_SERIAL =
            SynchedEntityData.defineId(PhalanxWeaponryEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation ANIM_SPAWN = RawAnimation.begin().thenPlayAndHold("spawn");
    private static final RawAnimation ANIM_GUARD_STANCE = RawAnimation.begin().thenPlayAndHold("guard_stance");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
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
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        followTargetPosition(getStandbyPosition());
        setYRot(owner.getYRot());
        setXRot(0.0f);
        setRot(getYRot(), getXRot());
        hasImpulse = true;

        if (tickCount >= SPAWN_POSE_STAY_TICK && AnimationState.of(entityData.get(ANIMATION_STATE)) == AnimationState.SPAWN) {
            entityData.set(ANIMATION_STATE, AnimationState.GUARD_STANCE.id);
        }
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return RotationTools.calculateBehindPosition(owner, 0, 0, -0.25);
        }

        return Vec3.ZERO;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
                this, "main", 0,
                state -> {
                    var animationState = AnimationState.of(entityData.get(ANIMATION_STATE));
                    if (animationState == AnimationState.SPAWN) {
                        state.setAnimation(ANIM_SPAWN);
                    } else {
                        state.setAnimation(ANIM_GUARD_STANCE);
                    }
                    return PlayState.CONTINUE;
                }
        ));
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
        // fade-out に対する easeOutCubic: 1 - easeOut(progress) = (1 - progress)^3
        return inverse * inverse * inverse;
    }

    private enum AnimationState {
        SPAWN(0),
        GUARD_STANCE(1);

        private final int id;

        AnimationState(int id) {
            this.id = id;
        }

        private static AnimationState of(int rawId) {
            return rawId == GUARD_STANCE.id ? GUARD_STANCE : SPAWN;
        }
    }
}
