package jp.aquafactory.apprenticecodex.entity.broom;

/** Floatmount Broomの移動性能値を、実行sideに依存しない形で解決する。 */
public final class FloatmountBroomMovement {
    public static final double HORIZONTAL_ACCELERATION = 0.04D;
    public static final double MAX_HORIZONTAL_SPEED = 0.35D;
    public static final double EMERGENCY_MAX_HORIZONTAL_SPEED = 0.1D;
    public static final double MAX_VERTICAL_SPEED = 0.15D;
    public static final double MAX_UNMOUNTED_FALL_SPEED = 0.1D;
    public static final double MAX_UNMOUNTED_RISE_SPEED = 0.1D;
    public static final double OVERDRIVE_HORIZONTAL_ACCELERATION_MULTIPLIER = 1.4D;
    public static final double OVERDRIVE_MAX_HORIZONTAL_SPEED_MULTIPLIER = 1.4D;

    private static final double WATER_HORIZONTAL_ACCELERATION_FACTOR = 0.5D;
    private static final double WATER_HORIZONTAL_SPEED_FACTOR = 0.6D;
    private static final double WATER_VERTICAL_SPEED_FACTOR = 0.6D;

    private FloatmountBroomMovement() {
    }

    public static double horizontalAcceleration(boolean waterPenaltyActive) {
        return horizontalAcceleration(waterPenaltyActive, false);
    }

    public static double horizontalAcceleration(boolean waterPenaltyActive, boolean overdriveEnabled) {
        return HORIZONTAL_ACCELERATION
                * (overdriveEnabled ? OVERDRIVE_HORIZONTAL_ACCELERATION_MULTIPLIER : 1.0D)
                * (waterPenaltyActive ? WATER_HORIZONTAL_ACCELERATION_FACTOR : 1.0D);
    }

    public static double maximumHorizontalSpeed(boolean forcedLanding, boolean waterPenaltyActive) {
        return maximumHorizontalSpeed(forcedLanding, waterPenaltyActive, false);
    }

    public static double maximumHorizontalSpeed(
            boolean forcedLanding,
            boolean waterPenaltyActive,
            boolean overdriveEnabled
    ) {
        var maximum = forcedLanding ? EMERGENCY_MAX_HORIZONTAL_SPEED : MAX_HORIZONTAL_SPEED;
        if (!forcedLanding && overdriveEnabled) {
            maximum *= OVERDRIVE_MAX_HORIZONTAL_SPEED_MULTIPLIER;
        }
        return maximum * (waterPenaltyActive ? WATER_HORIZONTAL_SPEED_FACTOR : 1.0D);
    }

    public static double maximumVerticalSpeed(boolean waterPenaltyActive) {
        return MAX_VERTICAL_SPEED * (waterPenaltyActive ? WATER_VERTICAL_SPEED_FACTOR : 1.0D);
    }

    public static double maximumUnoccupiedVerticalSpeed(boolean waterPenaltyActive) {
        return MAX_UNMOUNTED_RISE_SPEED * (waterPenaltyActive ? WATER_VERTICAL_SPEED_FACTOR : 1.0D);
    }
}
