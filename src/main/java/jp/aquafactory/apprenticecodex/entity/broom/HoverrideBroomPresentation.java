package jp.aquafactory.apprenticecodex.entity.broom;

public enum HoverrideBroomPresentation {
    NORMAL,
    ACCELERATING,
    BRAKING,
    GLIDING;

    private static final double SPEED_EFFECT_START_RATIO = 0.15D;

    public static HoverrideBroomPresentation resolve(
            float forwardInput,
            boolean gliding,
            boolean accelerationAllowed
    ) {
        if (gliding) {
            return GLIDING;
        }
        if (forwardInput < -1.0e-4F) {
            return BRAKING;
        }
        if (forwardInput > 1.0e-4F && accelerationAllowed) {
            return ACCELERATING;
        }
        return NORMAL;
    }

    public static HoverrideBroomPresentation fromId(int id) {
        var values = values();
        return id >= 0 && id < values.length ? values[id] : NORMAL;
    }

    public static float speedEffectIntensity(double horizontalSpeedRatio) {
        var normalized = (horizontalSpeedRatio - SPEED_EFFECT_START_RATIO)
                / (1.0D - SPEED_EFFECT_START_RATIO);
        var clamped = Math.clamp(normalized, 0.0D, 1.0D);
        return (float)(clamped * clamped * (3.0D - 2.0D * clamped));
    }
}
