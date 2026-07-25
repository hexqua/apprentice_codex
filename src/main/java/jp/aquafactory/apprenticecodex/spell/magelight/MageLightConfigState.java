package jp.aquafactory.apprenticecodex.spell.magelight;

import jp.aquafactory.apprenticecodex.config.spell.MageLightServerConfig;

public final class MageLightConfigState {
    private static double maxRange = MageLightServerConfig.DEFAULT_MAX_RANGE;

    private MageLightConfigState() {
    }

    public static double maxRange() {
        return maxRange;
    }

    public static void set(double maxRange) {
        MageLightConfigState.maxRange = Math.max(
                MageLightServerConfig.MIN_MAX_RANGE,
                Math.min(MageLightServerConfig.MAX_MAX_RANGE, maxRange)
        );
    }

    public static void reset() {
        set(MageLightServerConfig.DEFAULT_MAX_RANGE);
    }
}
