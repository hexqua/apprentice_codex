package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.config.item.FloatmountBroomServerConfig;

public final class FloatmountBroomConfigState {
    private static int normalFlightManaThreshold =
            FloatmountBroomServerConfig.DEFAULT_NORMAL_FLIGHT_MANA_THRESHOLD;

    private FloatmountBroomConfigState() {
    }

    public static int normalFlightManaThreshold() {
        return normalFlightManaThreshold;
    }

    public static void setNormalFlightManaThreshold(int normalFlightManaThreshold) {
        FloatmountBroomConfigState.normalFlightManaThreshold = Math.max(1, normalFlightManaThreshold);
    }

    public static void reset() {
        setNormalFlightManaThreshold(FloatmountBroomServerConfig.DEFAULT_NORMAL_FLIGHT_MANA_THRESHOLD);
    }
}
