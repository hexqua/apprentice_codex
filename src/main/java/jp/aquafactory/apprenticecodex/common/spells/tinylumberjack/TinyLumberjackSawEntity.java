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
import org.jetbrains.annotations.NotNull;
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

    private static final double BREAK_START_DISTANCE = 0.5;
    private static final int LOGS_PER_TICK = 2;

    private float damage;
    private float toolSpeed;
    private int reachSpeed;

    private @Nullable BlockPos breakTargetPos;
    private @Nullable Vec3 breakTargetHitPos;
    private float breakProgress;
    private boolean isBreaking;
    private boolean returningToStandby;
    private @Nullable Vec3 moveStartPos;
    private @Nullable Vec3 moveTargetPos;
    private int moveTick;
    private RaycastTools.TargetType targetType = RaycastTools.TargetType.NONE;
    private Vec3 ownerTargetHitPos = Vec3.ZERO;
    private @Nullable BlockPos ownerTargetBlockPos;

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
        @SuppressWarnings("resource") var level = level();
        super.tick();

        if (level.isClientSide) {
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        if (returningToStandby) {
            moveToTarget(owner, getStandbyPosition());
            if (position().distanceTo(getStandbyPosition()) <= BREAK_START_DISTANCE) {
                returningToStandby = false;
            }
            return;
        }

        if (isBreaking) {
            handleChopping(owner);
            return;
        }

        if (targetType == RaycastTools.TargetType.LIVING_ENTITY) {
            moveToTarget(owner, ownerTargetHitPos);
            return;
        }

        if (targetType == RaycastTools.TargetType.BLOCK && ownerTargetBlockPos != null) {
            var targetPos = ownerTargetBlockPos;
            var state = level.getBlockState(targetPos);
            if (state.is(BlockTags.LOGS)) {
                var hitPosition = ownerTargetHitPos;
                var distanceBeforeMove = position().distanceTo(hitPosition);
                if (distanceBeforeMove <= BREAK_START_DISTANCE) {
                    breakTargetPos = targetPos;
                    breakTargetHitPos = hitPosition;
                    breakProgress = 0.0f;
                    isBreaking = true;
                } else {
                    moveToTarget(owner, hitPosition);
                }
                return;
            }
        }

        moveToTarget(owner, getStandbyPosition());
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return RotationTools.calculateBehindPosition(owner, -0.4, 0.6, -0.7);
        }

        return Vec3.ZERO;
    }

    public void updateOwnerTarget(RaycastTools.TargetResult result) {
        targetType = result.hitType();
        ownerTargetHitPos = result.hitPosition();
        ownerTargetBlockPos = result.hitBlock();
    }

    private void moveToTarget(LivingEntity owner, Vec3 targetPos) {
        if (moveTargetPos == null || moveTargetPos.distanceToSqr(targetPos) > 0.0025) {
            moveStartPos = position();
            moveTargetPos = targetPos;
            moveTick = 0;
        }

        if (moveTargetPos == null || moveStartPos == null) {
            moveStartPos = position();
            moveTargetPos = position();
            moveTick = 0;
        }

        moveTick = Math.min(moveTick + 1, reachSpeed);
        var t = moveTick / (double) Math.max(reachSpeed, 1);
        var eased = 1.0 - (1.0 - t) * (1.0 - t);
        var delta = moveTargetPos.subtract(moveStartPos).scale(eased);
        var newPos = moveStartPos.add(delta);
        setPos(newPos.x, newPos.y, newPos.z);
        setYRot(owner.getYRot());
        setXRot(owner.getXRot());
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    private void handleChopping(LivingEntity owner) {
        if (breakTargetPos == null) {
            isBreaking = false;
            returningToStandby = true;
            return;
        }

        var level = level();
        var targetPos = breakTargetHitPos != null ? breakTargetHitPos : Vec3.atCenterOf(breakTargetPos);
        moveToTarget(owner, targetPos);

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

        // ブロックの採掘速度は正しいツールなら硬さ×30.
        var toolBreakDelta = toolSpeed / (hardness * 30);

        breakProgress = breakProgress + toolBreakDelta;
        var stage = Math.min(9, (int) (breakProgress * 10.0f));
        level.destroyBlockProgress(getId(), breakTargetPos, stage);

        if (breakProgress >= 1.0f) {
            breakProgress = 0.0f;
            level.destroyBlock(breakTargetPos, true, owner);
            level.destroyBlockProgress(getId(), breakTargetPos, -1);

            // 木こりジョブを開始する.
            if (level instanceof ServerLevel serverLevel) {
                // 続けて木こりできるようにジョブは保持しないで管理クラスにわたすだけ.
                TinyLumberjackJobManager.submit(serverLevel, new TinyLumberjackJob(breakTargetPos, LOGS_PER_TICK));
            }
        }
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

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setReachSpeed(int reachSpeed) {
        this.reachSpeed = reachSpeed;
    }

    public void setToolSpeed(float toolSpeed) {
        this.toolSpeed = toolSpeed;
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        @SuppressWarnings("resource") var level = level();
        if (!level.isClientSide && breakTargetPos != null) {
            level.destroyBlockProgress(getId(), breakTargetPos, -1);
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
