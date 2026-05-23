package jp.aquafactory.apprenticecodex.item.focusstaffbow;

public record FocusStaffbowChargeSettings(
        double pendingMaxChargeMultiplier,
        double continuousMaxChargeMultiplier,
        int minimumOverchargeBaselineTicks,
        double chargeManaCostExponent,
        double chargeManaCostMultiplier
) {
    public static final FocusStaffbowChargeSettings DEFAULT = new FocusStaffbowChargeSettings(
            FocusStaffbowChargeLogic.MAX_PENDING_CHARGE_MULTIPLIER,
            FocusStaffbowChargeLogic.MAX_CONTINUOUS_CHARGE_MULTIPLIER,
            FocusStaffbowChargeLogic.MINIMUM_OVERCHARGE_BASELINE_TICKS,
            2.0D,
            1.0D
    );

    public FocusStaffbowChargeSettings {
        pendingMaxChargeMultiplier = Math.max(1.0D, pendingMaxChargeMultiplier);
        continuousMaxChargeMultiplier = Math.max(1.0D, continuousMaxChargeMultiplier);
        minimumOverchargeBaselineTicks = Math.max(0, minimumOverchargeBaselineTicks);
        chargeManaCostExponent = Math.max(0.0D, chargeManaCostExponent);
        chargeManaCostMultiplier = Math.max(0.0D, chargeManaCostMultiplier);
    }
}
