package jp.aquafactory.apprenticecodex.entity.broom;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class HoverrideBroomMovement {
    public static final double MAX_HORIZONTAL_SPEED = 0.55D;
    public static final double HORIZONTAL_ACCELERATION = 0.04D;
    public static final double COAST_DAMPING = 0.926D;
    public static final double PRACTICAL_STOP_SPEED = 0.025D;
    public static final double BRAKE_DECELERATION = MAX_HORIZONTAL_SPEED / 10.0D;
    public static final double DIRECTION_RESPONSE = 0.4D;
    public static final double OVERDRIVE_MOVEMENT_MULTIPLIER = 1.5D;
    public static final double OVERDRIVE_INERTIA_RELEASE_MINIMUM_SPEED_RATIO = 0.8D;
    private static final double VECTOR_EPSILON = 1.0e-8D;

    private HoverrideBroomMovement() {
    }

    public static Vec3 normalHorizontal(
            Vec3 currentMovement,
            Vec3 forwardDirection,
            float forwardInput,
            boolean accelerationAllowed
    ) {
        return normalHorizontal(currentMovement, forwardDirection, forwardInput, accelerationAllowed, false);
    }

    public static Vec3 normalHorizontal(
            Vec3 currentMovement,
            Vec3 forwardDirection,
            float forwardInput,
            boolean accelerationAllowed,
            boolean overdriveEnabled
    ) {
        var horizontal = horizontal(currentMovement);
        var maximumSpeed = maximumHorizontalSpeed(overdriveEnabled);
        var speed = Math.min(horizontal.length(), maximumSpeed);
        if (forwardInput < -1.0e-4F) {
            return withSpeed(horizontal, Math.max(0.0D, speed - brakeDeceleration(overdriveEnabled)));
        }
        if (forwardInput > 1.0e-4F && accelerationAllowed) {
            var direction = speed > VECTOR_EPSILON
                    ? rotateToward(horizontal.scale(1.0D / speed), forwardDirection, DIRECTION_RESPONSE)
                    : horizontal(forwardDirection).normalize();
            var acceleratedSpeed = Math.min(
                    maximumSpeed,
                    speed + horizontalAcceleration(overdriveEnabled) * forwardInput
            );
            return direction.scale(acceleratedSpeed);
        }

        var dampedSpeed = speed * COAST_DAMPING;
        if (dampedSpeed < PRACTICAL_STOP_SPEED) {
            return Vec3.ZERO;
        }
        var direction = rotateToward(horizontal.scale(1.0D / speed), forwardDirection, DIRECTION_RESPONSE);
        return direction.scale(dampedSpeed);
    }

    public static Vec3 releaseHorizontal(Vec3 currentMovement, Vec3 forwardDirection, double minimumSpeed) {
        return releaseHorizontal(currentMovement, forwardDirection, minimumSpeed, MAX_HORIZONTAL_SPEED);
    }

    public static Vec3 releaseHorizontal(
            Vec3 currentMovement,
            Vec3 forwardDirection,
            double minimumSpeed,
            double maximumSpeed
    ) {
        var speed = Math.max(horizontal(currentMovement).length(), Math.max(0.0D, minimumSpeed));
        if (speed <= VECTOR_EPSILON) {
            return Vec3.ZERO;
        }
        return horizontal(forwardDirection).normalize().scale(Math.min(Math.max(0.0D, maximumSpeed), speed));
    }

    public static double maximumHorizontalSpeed(boolean overdriveEnabled) {
        return MAX_HORIZONTAL_SPEED * (overdriveEnabled ? OVERDRIVE_MOVEMENT_MULTIPLIER : 1.0D);
    }

    public static double horizontalAcceleration(boolean overdriveEnabled) {
        return HORIZONTAL_ACCELERATION * (overdriveEnabled ? OVERDRIVE_MOVEMENT_MULTIPLIER : 1.0D);
    }

    public static double brakeDeceleration(boolean overdriveEnabled) {
        return BRAKE_DECELERATION * (overdriveEnabled ? OVERDRIVE_MOVEMENT_MULTIPLIER : 1.0D);
    }

    public static Vec3 rotateToward(Vec3 currentDirection, Vec3 targetDirection, double response) {
        var current = horizontal(currentDirection).normalize();
        var target = horizontal(targetDirection).normalize();
        if (current.lengthSqr() <= VECTOR_EPSILON) {
            return target;
        }
        if (target.lengthSqr() <= VECTOR_EPSILON) {
            return current;
        }

        var currentAngle = Math.atan2(current.x, current.z);
        var targetAngle = Math.atan2(target.x, target.z);
        var difference = Mth.wrapDegrees((float)Math.toDegrees(targetAngle - currentAngle)) * Mth.DEG_TO_RAD;
        var angle = currentAngle + difference * Mth.clamp(response, 0.0D, 1.0D);
        return new Vec3(Math.sin(angle), 0.0D, Math.cos(angle));
    }

    public static Vec3 horizontal(Vec3 movement) {
        return new Vec3(movement.x, 0.0D, movement.z);
    }

    private static Vec3 withSpeed(Vec3 movement, double speed) {
        return movement.lengthSqr() <= VECTOR_EPSILON || speed <= VECTOR_EPSILON
                ? Vec3.ZERO
                : movement.normalize().scale(speed);
    }
}
