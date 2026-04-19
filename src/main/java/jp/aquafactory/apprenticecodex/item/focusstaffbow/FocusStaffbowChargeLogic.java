package jp.aquafactory.apprenticecodex.item.focusstaffbow;

public final class FocusStaffbowChargeLogic {
    public static final int MINIMUM_SPECIAL_CAST_TICKS = 40;

    private FocusStaffbowChargeLogic() {
    }

    public static int normalizeRequiredCastTicks(int effectiveCastTicks) {
        return Math.max(MINIMUM_SPECIAL_CAST_TICKS, Math.max(0, effectiveCastTicks));
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
}
