package jp.aquafactory.apprenticecodex.item.focusstaffbow;

public final class FocusStaffbowChargeLogic {
    public static final int MINIMUM_OVERCHARGE_BASELINE_TICKS = 20;
    public static final int CONTINUOUS_CHARGE_UPDATE_INTERVAL_TICKS = 10;
    public static final double MAX_PENDING_CHARGE_MULTIPLIER = 3.0D;
    public static final double MAX_CONTINUOUS_CHARGE_MULTIPLIER = 2.0D;
    public static final int CONTINUOUS_STAGE_ONE_TICKS = 100;
    public static final int CONTINUOUS_STAGE_TWO_TICKS = 150;
    public static final int CONTINUOUS_MAX_CHARGE_TICKS = CONTINUOUS_STAGE_ONE_TICKS + CONTINUOUS_STAGE_TWO_TICKS;

    private FocusStaffbowChargeLogic() {
    }

    public static int normalizePendingRequiredCastTicks(int effectiveCastTicks) {
        return Math.max(0, effectiveCastTicks);
    }

    public static int normalizePendingChargeBaselineTicks(int effectiveCastTicks) {
        return normalizePendingChargeBaselineTicks(effectiveCastTicks, FocusStaffbowChargeSettings.DEFAULT);
    }

    public static int normalizePendingChargeBaselineTicks(int effectiveCastTicks, FocusStaffbowChargeSettings settings) {
        return Math.max(settings.minimumOverchargeBaselineTicks(), Math.max(0, effectiveCastTicks));
    }

    public static double clampChargeMultiplier(double rawMultiplier, double maxChargeMultiplier) {
        var clampedMaxChargeMultiplier = Math.max(1.0D, maxChargeMultiplier);
        return Math.min(clampedMaxChargeMultiplier, Math.max(1.0D, rawMultiplier));
    }

    public static double computePendingChargeMultiplier(long totalCastTicks, int baselineCastTicks) {
        return computePendingChargeMultiplier(totalCastTicks, baselineCastTicks, FocusStaffbowChargeSettings.DEFAULT);
    }

    public static double computePendingChargeMultiplier(long totalCastTicks, int baselineCastTicks,
                                                        FocusStaffbowChargeSettings settings) {
        var normalizedBaselineTicks = Math.max(settings.minimumOverchargeBaselineTicks(), baselineCastTicks);
        var normalizedCastTicks = Math.max(0L, totalCastTicks);
        if (normalizedCastTicks <= normalizedBaselineTicks) {
            return 1.0D;
        }

        var maxMultiplier = Math.max(1.0D, settings.pendingMaxChargeMultiplier());
        var overchargeTicks = normalizedCastTicks - normalizedBaselineTicks;
        var firstStageTicks = Math.max(1L, normalizedBaselineTicks * 2L);
        var firstStageTarget = Math.min(2.0D, maxMultiplier);
        if (overchargeTicks <= firstStageTicks || maxMultiplier <= 2.0D) {
            var progress = Math.min(1.0D, overchargeTicks / (double) firstStageTicks);
            return clampChargeMultiplier(
                    1.0D + (firstStageTarget - 1.0D) * progress,
                    maxMultiplier
            );
        }

        var secondStageTicks = Math.max(1L, normalizedBaselineTicks * 3L);
        var secondStageOverchargeTicks = overchargeTicks - firstStageTicks;
        var progress = Math.min(1.0D, secondStageOverchargeTicks / (double) secondStageTicks);
        return clampChargeMultiplier(
                2.0D + (maxMultiplier - 2.0D) * progress,
                maxMultiplier
        );
    }

    public static double computeContinuousChargeMultiplier(long totalCastTicks) {
        return computeContinuousChargeMultiplier(totalCastTicks, FocusStaffbowChargeSettings.DEFAULT);
    }

    public static double computeContinuousChargeMultiplier(long totalCastTicks, FocusStaffbowChargeSettings settings) {
        var normalizedCastTicks = Math.max(0L, totalCastTicks);
        var maxMultiplier = Math.max(1.0D, settings.continuousMaxChargeMultiplier());
        var midpointMultiplier = 1.0D + (maxMultiplier - 1.0D) * 0.5D;
        if (normalizedCastTicks <= CONTINUOUS_STAGE_ONE_TICKS) {
            return clampChargeMultiplier(
                    1.0D + (midpointMultiplier - 1.0D) * normalizedCastTicks / (double) CONTINUOUS_STAGE_ONE_TICKS,
                    maxMultiplier
            );
        }

        var secondStageTicks = normalizedCastTicks - CONTINUOUS_STAGE_ONE_TICKS;
        return clampChargeMultiplier(
                midpointMultiplier + (maxMultiplier - midpointMultiplier) * secondStageTicks / (double) CONTINUOUS_STAGE_TWO_TICKS,
                maxMultiplier
        );
    }

    public static float computeContinuousChargeProgress(long totalCastTicks) {
        return (float) Math.min(1.0D, Math.max(0.0D, Math.max(0L, totalCastTicks) / (double) CONTINUOUS_MAX_CHARGE_TICKS));
    }

    public static int computeScaledManaCost(int baseManaCost, double chargeMultiplier) {
        return computeScaledManaCost(baseManaCost, chargeMultiplier, FocusStaffbowChargeSettings.DEFAULT);
    }

    public static int computeScaledManaCost(int baseManaCost, double chargeMultiplier, FocusStaffbowChargeSettings settings) {
        if (baseManaCost <= 0) {
            return 0;
        }

        return (int) Math.floor(baseManaCost
                * settings.chargeManaCostMultiplier()
                * Math.pow(Math.max(1.0D, chargeMultiplier), settings.chargeManaCostExponent()));
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

}
