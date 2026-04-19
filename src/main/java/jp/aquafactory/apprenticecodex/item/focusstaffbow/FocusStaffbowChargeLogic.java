package jp.aquafactory.apprenticecodex.item.focusstaffbow;

public final class FocusStaffbowChargeLogic {
    public static final int MINIMUM_OVERCHARGE_BASELINE_TICKS = 20;
    public static final int CONTINUOUS_CHARGE_UPDATE_INTERVAL_TICKS = 10;
    public static final double MAX_PENDING_CHARGE_MULTIPLIER = 3.0D;
    public static final double MAX_CONTINUOUS_CHARGE_MULTIPLIER = 3.0D;

    private FocusStaffbowChargeLogic() {
    }

    public static int normalizePendingRequiredCastTicks(int effectiveCastTicks) {
        return Math.max(0, effectiveCastTicks);
    }

    public static int normalizePendingChargeBaselineTicks(int effectiveCastTicks) {
        return Math.max(MINIMUM_OVERCHARGE_BASELINE_TICKS, Math.max(0, effectiveCastTicks));
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

    public static double computePendingChargeMultiplier(long totalCastTicks, int baselineCastTicks) {
        var normalizedBaselineTicks = Math.max(MINIMUM_OVERCHARGE_BASELINE_TICKS, baselineCastTicks);
        var normalizedCastTicks = Math.max(0L, totalCastTicks);
        if (normalizedCastTicks <= normalizedBaselineTicks) {
            return 1.0D;
        }

        var overchargeTicks = normalizedCastTicks - normalizedBaselineTicks;
        var secondStageThresholdTicks = normalizedBaselineTicks * 2L;
        if (overchargeTicks <= secondStageThresholdTicks) {
            return Math.min(
                    MAX_PENDING_CHARGE_MULTIPLIER,
                    1.0D + overchargeTicks / (double) (normalizedBaselineTicks * 2L)
            );
        }

        var secondStageOverchargeTicks = overchargeTicks - secondStageThresholdTicks;
        return Math.min(
                MAX_PENDING_CHARGE_MULTIPLIER,
                2.0D + secondStageOverchargeTicks / (double) (normalizedBaselineTicks * 3L)
        );
    }

    public static int computeScaledManaCost(int baseManaCost, double chargeMultiplier) {
        if (baseManaCost <= 0) {
            return 0;
        }

        return (int) Math.floor(baseManaCost * chargeMultiplier * chargeMultiplier);
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
