package jp.aquafactory.apprenticecodex.entity.broom;

public enum HoverrideBroomPresentation {
    NORMAL,
    ACCELERATING,
    BRAKING,
    GLIDING;

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
}
