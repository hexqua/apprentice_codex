package jp.aquafactory.apprenticecodex.item.focusstaffbow;

public final class FocusStaffbowChargeLogic {
    public static final int MINIMUM_SPECIAL_CAST_TICKS = 40;
    public static final int CONTINUOUS_CHARGE_UPDATE_INTERVAL_TICKS = 10;

    private FocusStaffbowChargeLogic() {
    }

    public static int normalizePendingRequiredCastTicks(int effectiveCastTicks) {
        return Math.max(MINIMUM_SPECIAL_CAST_TICKS, Math.max(0, effectiveCastTicks));
    }

    public static int normalizeContinuousRequiredCastTicks(int effectiveCastTicks) {
        return Math.max(1, effectiveCastTicks);
    }

    public static double computeRawChargeMultiplier(long totalCastTicks, int requiredCastTicks) {
        if (requiredCastTicks <= 0) {
            return 1.0D;
        }

        return Math.max(0L, totalCastTicks) / (double) requiredCastTicks;
    }

    public static double clampChargeMultiplier(double rawMultiplier, double maxChargeMultiplier) {
        var clampedMaxChargeMultiplier = Math.max(1.0D, maxChargeMultiplier);
        return Math.min(clampedMaxChargeMultiplier, Math.max(1.0D, rawMultiplier));
    }

    public static long sampleElapsedTicks(long elapsedTicks, int intervalTicks) {
        var normalizedElapsedTicks = Math.max(0L, elapsedTicks);
        if (intervalTicks <= 1) {
            return normalizedElapsedTicks;
        }

        return normalizedElapsedTicks - normalizedElapsedTicks % intervalTicks;
    }

    public static boolean shouldTriggerContinuousCast(long elapsedTicks, int requiredCastTicks) {
        if (elapsedTicks <= 0L) {
            return false;
        }

        return Math.floorMod(requiredCastTicks - (int) Math.min(Integer.MAX_VALUE, elapsedTicks) + 1,
                CONTINUOUS_CHARGE_UPDATE_INTERVAL_TICKS) == 0;
    }

    public static int computeContinuousCastDurationRemaining(long elapsedTicks, int requiredCastTicks) {
        return requiredCastTicks - (int) Math.min(Integer.MAX_VALUE, Math.max(0L, elapsedTicks));
    }
}
