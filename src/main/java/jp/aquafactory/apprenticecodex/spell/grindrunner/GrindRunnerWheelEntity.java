package jp.aquafactory.apprenticecodex.spell.grindrunner;

import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
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

public class GrindRunnerWheelEntity extends SummonWeaponEntity implements GeoEntity {

    private enum WheelState {
        DROPPING,
        STANDBY,
        LAUNCHED
    }

    private static final double LOOK_TARGET_RANGE = 64.0;
    private static final double LOOK_TARGET_HITBOX_WIDTH = 0.5;
    private static final double LOOK_UPDATE_SUPPRESS_DISTANCE_SQR = 1.0;
    private static final double LAUNCH_GRAVITY = 0.08;
    private static final double GROUND_FRICTION = 0.91;
    private static final double STOP_SPEED_THRESHOLD_SQR = 0.01;
    private static final double STOP_VERTICAL_SPEED_THRESHOLD = 0.1;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation GRIND = RawAnimation.begin().thenLoop("grind");

    private @Nullable Vec3 summonGroundPosition;
    private @Nullable Vec3 dropStartPosition;
    private int dropDurationTick = 1;
    private int dropProgressTick = 0;
    private WheelState state = WheelState.STANDBY;

    private boolean hasAimInitialized = false;
    private boolean wasAimUpdateSuppressed = false;

    private double launchSpeed = 1.2;
    private int slowdownStartTick = 12;
    private double slowdownFactor = 0.9;
    private int launchedTick = 0;

    public GrindRunnerWheelEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public GrindRunnerWheelEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData() {
        // do nothing.
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
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        if (state == WheelState.LAUNCHED) {
            tickLaunched();
            return;
        }

        if (summonGroundPosition == null) {
            summonGroundPosition = owner.position();
        }

        if (state == WheelState.DROPPING) {
            tickDropping();
            // 出現演出中は向き更新を止める.
            return;
        }

        holdSummonPosition();
        updateAimByOwnerLook(owner);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (summonGroundPosition != null) {
            return summonGroundPosition;
        }

        if (getOwner() instanceof LivingEntity owner) {
            return owner.position();
        }

        return position();
    }

    @Override
    public void releaseWeapon() {
        if (state == WheelState.LAUNCHED) {
            return;
        }

        var launchDir = flattenDirection(getLookAngle());
        if (launchDir.lengthSqr() < 1.0E-6 && getOwner() instanceof LivingEntity owner) {
            launchDir = flattenDirection(owner.getViewVector(1.0F));
        }
        if (launchDir.lengthSqr() < 1.0E-6) {
            launchDir = new Vec3(0, 0, 1);
        }

        state = WheelState.LAUNCHED;
        launchedTick = 0;
        setNoGravity(false);
        setMaxUpStep(1.0f);
        setDeltaMovement(launchDir.normalize().scale(launchSpeed));
        hasImpulse = true;
    }

    public void setSummonSettings(Vec3 summonGroundPosition, double dropHeight, int dropDurationTick) {
        this.summonGroundPosition = summonGroundPosition;
        this.dropStartPosition = summonGroundPosition.add(0, Math.max(0, dropHeight), 0);
        this.dropDurationTick = Math.max(1, dropDurationTick);
        this.dropProgressTick = 0;
        this.state = WheelState.DROPPING;
        this.hasAimInitialized = false;
        this.wasAimUpdateSuppressed = false;

        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        setPos(dropStartPosition.x, dropStartPosition.y, dropStartPosition.z);

        // 初期向きは発動者の yaw を採用する.
        if (getOwner() instanceof LivingEntity owner) {
            var yaw = resolveInitialYaw(owner);
            setYRot(yaw);
            setXRot(0);
            setRot(getYRot(), getXRot());
            hasImpulse = true;
            hasAimInitialized = true;
        }
    }

    public void setLaunchSettings(double launchSpeed, int slowdownStartTick, double slowdownFactor) {
        this.launchSpeed = Math.max(0.05, launchSpeed);
        this.slowdownStartTick = Math.max(1, slowdownStartTick);
        this.slowdownFactor = Mth.clamp(slowdownFactor, 0.5, 0.999);
    }

    private void tickDropping() {
        if (dropStartPosition == null || summonGroundPosition == null) {
            state = WheelState.STANDBY;
            return;
        }

        dropProgressTick = Math.min(dropProgressTick + 1, dropDurationTick);
        var t = dropProgressTick / (double) dropDurationTick;
        var nextPos = dropStartPosition.lerp(summonGroundPosition, t);
        setPos(nextPos.x, nextPos.y, nextPos.z);

        if (dropProgressTick >= dropDurationTick) {
            state = WheelState.STANDBY;
            holdSummonPosition();
        }
    }

    private void holdSummonPosition() {
        var standby = getStandbyPosition();
        setPos(standby.x, standby.y, standby.z);
        setDeltaMovement(Vec3.ZERO);
    }

    private void updateAimByOwnerLook(LivingEntity owner) {
        var aimResult = RaycastTools.raycastFromEye(owner, LOOK_TARGET_RANGE, LOOK_TARGET_HITBOX_WIDTH, e -> e != owner);
        var targetPos = aimResult.hitPosition();
        var shouldSuppress = targetPos.distanceToSqr(position()) <= LOOK_UPDATE_SUPPRESS_DISTANCE_SQR;

        if (shouldSuppress) {
            if (!hasAimInitialized) {
                // 初回だけ発動者の水平向きにそろえて、近距離抑制中の姿勢崩れを防ぐ.
                var ownerLook = owner.getViewVector(1.0F);
                var flatLook = new Vec3(ownerLook.x, 0, ownerLook.z);
                if (flatLook.lengthSqr() < 1.0E-6) {
                    flatLook = new Vec3(0, 0, 1);
                }
                applyFacing(flatLook);
                hasAimInitialized = true;
            } else if (!wasAimUpdateSuppressed) {
                // 抑制に入る瞬間にだけピッチを戻して、地面めり込み見えを防ぐ.
                setXRot(0);
                setRot(getYRot(), getXRot());
                hasImpulse = true;
            }

            wasAimUpdateSuppressed = true;
            return;
        }

        applyFacing(targetPos.subtract(position()));
        hasAimInitialized = true;
        wasAimUpdateSuppressed = false;
    }

    private void tickLaunched() {
        launchedTick++;

        var velocity = getDeltaMovement().add(0, -LAUNCH_GRAVITY, 0);
        if (launchedTick >= slowdownStartTick) {
            velocity = new Vec3(velocity.x * slowdownFactor, velocity.y, velocity.z * slowdownFactor);
        }

        setDeltaMovement(velocity);
        move(MoverType.SELF, getDeltaMovement());

        // 1ブロック段差は自前で登坂し、2ブロック相当の壁に当たった時のみ消す.
        if (horizontalCollision) {
            var stepped = tryStepUpOneBlock(getDeltaMovement());
            if (stepped) {
                var moved = getDeltaMovement();
                setDeltaMovement(moved.x, Math.max(0.0, moved.y), moved.z);
            } else if (isLargeObstacleAhead(getDeltaMovement())) {
                discard();
                return;
            }
        }

        var moved = getDeltaMovement();
        if (onGround()) {
            moved = new Vec3(moved.x * GROUND_FRICTION, moved.y * 0.9, moved.z * GROUND_FRICTION);
            setDeltaMovement(moved);
        }

        if (launchedTick >= slowdownStartTick
                && onGround()
                && getDeltaMovement().horizontalDistanceSqr() <= STOP_SPEED_THRESHOLD_SQR
                && Math.abs(getDeltaMovement().y) <= STOP_VERTICAL_SPEED_THRESHOLD) {
            discard();
            return;
        }

        updateRotationByMovement();
    }

    private void applyFacing(Vec3 direction) {
        var flat = flattenDirection(direction);
        if (flat.lengthSqr() < 1.0E-6) {
            return;
        }

        var yawPitch = RotationTools.calculateYawPitchByDirection(flat);
        setYRot(yawPitch.yaw());
        setXRot(0);
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    private boolean tryStepUpOneBlock(Vec3 velocity) {
        var horizontal = flattenDirection(velocity);
        if (horizontal.lengthSqr() < 1.0E-6) {
            return false;
        }

        var level = level();
        var ahead = position().add(horizontal.scale(Math.max(getBbWidth(), 0.5)));
        var feetY = getBoundingBox().minY + 0.01;
        var frontPos = BlockPos.containing(ahead.x, feetY, ahead.z);
        if (!isSolidCollision(level, frontPos)) {
            return false;
        }

        var upperPos = frontPos.above();
        if (isSolidCollision(level, upperPos)) {
            return false;
        }

        var forward = horizontal.scale(0.2);
        var stepY = frontPos.getY() + 1.0 + 0.001;
        setPos(getX() + forward.x, Math.max(getY(), stepY), getZ() + forward.z);
        return true;
    }

    private boolean isLargeObstacleAhead(Vec3 velocity) {
        var horizontal = flattenDirection(velocity);
        if (horizontal.lengthSqr() < 1.0E-6) {
            return false;
        }

        var level = level();
        var ahead = position().add(horizontal.scale(Math.max(getBbWidth(), 0.5)));
        var feetY = getBoundingBox().minY + 0.01;
        var frontPos = BlockPos.containing(ahead.x, feetY, ahead.z);
        return isSolidCollision(level, frontPos) && isSolidCollision(level, frontPos.above());
    }

    private static boolean isSolidCollision(Level level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private void updateRotationByMovement() {
        var horizontal = flattenDirection(getDeltaMovement());
        if (horizontal.lengthSqr() < 1.0E-6) {
            return;
        }

        var yawPitch = RotationTools.calculateYawPitchByDirection(horizontal);
        setYRot(yawPitch.yaw());
        setXRot(0);
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    private static Vec3 flattenDirection(Vec3 direction) {
        var flat = new Vec3(direction.x, 0, direction.z);
        if (flat.lengthSqr() < 1.0E-6) {
            return Vec3.ZERO;
        }
        return flat.normalize();
    }

    private static float resolveInitialYaw(LivingEntity owner) {
        var yaw = owner.getYRot();
        if (Float.isFinite(yaw)) {
            return yaw;
        }

        // 極端な姿勢時のフォールバック.
        if (Float.isFinite(owner.yBodyRot)) {
            return owner.yBodyRot;
        }
        return 0.0f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
                this, "main", 0,
                state -> {
                    state.setAnimation(GRIND);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
