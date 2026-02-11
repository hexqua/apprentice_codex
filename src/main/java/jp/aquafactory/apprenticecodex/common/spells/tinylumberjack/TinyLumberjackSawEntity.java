package jp.aquafactory.apprenticecodex.common.spells.tinylumberjack;

import jp.aquafactory.apprenticecodex.common.entity.spell.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.common.utility.RotationTools;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TinyLumberjackSawEntity extends SummonWeaponEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation ROTATE = RawAnimation.begin().thenLoop("spin");

    private static final double TARGET_RANGE = 4.0;
    private static final double TARGET_RAYCAST_WIDTH = 0.5;
    private static final double BREAK_START_DISTANCE = 0.2;
    private static final float BASE_BREAK_SPEED = 1.0f / 40.0f;
    private static final int MOVE_DURATION_TICKS = 20;
    private static final double MOVE_TARGET_EPSILON_SQR = 0.0025;
    private static final int LOGS_PER_TICK = 2;

    private @Nullable BlockPos breakTargetPos;
    private @Nullable Vec3 breakTargetHitPos;
    private float breakProgress;
    private boolean isBreaking;
    private boolean returningToStandby;
    private @Nullable Vec3 moveStartPos;
    private @Nullable Vec3 moveTargetPos;
    private int moveTick;
    private @Nullable TinyLumberjackJob currentJob;

    public TinyLumberjackSawEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public TinyLumberjackSawEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData() {
        // do nothing.
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

        if (returningToStandby) {
            moveToStandby(owner);
            if (position().distanceTo(getStandbyPosition()) <= BREAK_START_DISTANCE) {
                returningToStandby = false;
            }
            return;
        }

        if (isBreaking) {
            continueBreaking(owner);
            return;
        }

        var result = RaycastTools.raycastFromEye(owner, TARGET_RANGE, TARGET_RAYCAST_WIDTH, e -> e instanceof LivingEntity);
        if (result.hitType() == RaycastTools.TargetType.LIVING_ENTITY) {
            moveToTarget(owner, result.hitPosition());
            return;
        }

        if (result.hitType() == RaycastTools.TargetType.BLOCK) {
            var targetPos = result.hitBlock();
            var state = level.getBlockState(targetPos);
            if (state.is(BlockTags.LOGS)) {
                var hitPosition = result.hitPosition();
                var distanceBeforeMove = position().distanceTo(hitPosition);
                if (distanceBeforeMove <= BREAK_START_DISTANCE) {
                    startBreaking(level, targetPos, hitPosition);
                    continueBreaking(owner);
                } else {
                    moveToTarget(owner, hitPosition);
                }
                return;
            }
        }

        moveToStandby(owner);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return calculateAxePosition(owner);
        }

        return Vec3.ZERO;
    }

    private static Vec3 calculateAxePosition(LivingEntity owner) {
        return RotationTools.calculateBehindPosition(owner, -0.4, 0.6, -0.4);
    }

    private void moveToStandby(LivingEntity owner) {
        moveToTarget(owner, getStandbyPosition());
    }

    private void moveToTarget(LivingEntity owner, Vec3 targetPos) {
        updateMoveTarget(targetPos);
        applyMovement(owner);
    }

    private void updateMoveTarget(Vec3 targetPos) {
        if (moveTargetPos == null || moveTargetPos.distanceToSqr(targetPos) > MOVE_TARGET_EPSILON_SQR) {
            moveStartPos = position();
            moveTargetPos = targetPos;
            moveTick = 0;
        }
    }

    private void applyMovement(LivingEntity owner) {
        if (moveTargetPos == null || moveStartPos == null) {
            moveStartPos = position();
            moveTargetPos = position();
            moveTick = 0;
        }

        moveTick = Math.min(moveTick + 1, MOVE_DURATION_TICKS);
        var t = moveTick / (double) MOVE_DURATION_TICKS;
        var eased = 1.0 - (1.0 - t) * (1.0 - t);
        var delta = moveTargetPos.subtract(moveStartPos).scale(eased);
        var newPos = moveStartPos.add(delta);
        setPos(newPos.x, newPos.y, newPos.z);
        setYRot(owner.getYRot());
        setXRot(owner.getXRot());
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    private void startBreaking(Level level, BlockPos targetPos, Vec3 hitPosition) {
        breakTargetPos = targetPos;
        breakTargetHitPos = hitPosition;
        breakProgress = 0.0f;
        isBreaking = true;
    }

    private void continueBreaking(LivingEntity owner) {
        if (breakTargetPos == null) {
            isBreaking = false;
            returningToStandby = true;
            return;
        }

        var level = level();
        var targetPos = breakTargetHitPos != null ? breakTargetHitPos : Vec3.atCenterOf(breakTargetPos);
        moveToTarget(owner, targetPos);

        if (currentJob != null) {
            if (currentJob.isComplete()) {
                finishJob();
            }
            return;
        }

        var state = level.getBlockState(breakTargetPos);
        if (!state.is(BlockTags.LOGS)) {
            cancelChop(level);
            returningToStandby = true;
            return;
        }

        var hardness = state.getDestroySpeed(level, breakTargetPos);
        if (hardness < 0.0f) {
            cancelChop(level);
            returningToStandby = true;
            return;
        }

        breakProgress = Math.min(1.0f, breakProgress + getBreakIncrement(hardness));
        var stage = Math.min(9, (int) (breakProgress * 10.0f));
        level.destroyBlockProgress(getId(), breakTargetPos, stage);

        if (breakProgress >= 1.0f) {
            level.destroyBlock(breakTargetPos, true, owner);
            level.destroyBlockProgress(getId(), breakTargetPos, -1);
            startJob(level, breakTargetPos);
        }
    }

    private void startJob(Level level, BlockPos targetPos) {
        if (level instanceof ServerLevel serverLevel) {
            currentJob = new TinyLumberjackJob(targetPos, LOGS_PER_TICK);
            TinyLumberjackJobManager.submit(serverLevel, currentJob);
        }
        breakProgress = 0.0f;
    }

    private void finishJob() {
        currentJob = null;
        breakTargetPos = null;
        breakTargetHitPos = null;
        breakProgress = 0.0f;
        isBreaking = false;
        returningToStandby = true;
    }

    private void cancelChop(Level level) {
        if (!level.isClientSide && breakTargetPos != null) {
            level.destroyBlockProgress(getId(), breakTargetPos, -1);
        }
        breakTargetPos = null;
        breakTargetHitPos = null;
        breakProgress = 0.0f;
        isBreaking = false;
    }

    private static float getBreakIncrement(float hardness) {
        return BASE_BREAK_SPEED / Math.max(0.1f, hardness);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && currentJob == null && breakTargetPos != null) {
            level().destroyBlockProgress(getId(), breakTargetPos, -1);
        }
        super.remove(reason);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
                this, "main", 0,
                state -> {
                    state.setAnimation(ROTATE);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
