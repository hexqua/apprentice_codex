package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class FocusStaffbowServerConfig {
    private final ForgeConfigSpec.DoubleValue maxChargeMultiplier;

    private FocusStaffbowServerConfig(ForgeConfigSpec.DoubleValue maxChargeMultiplier) {
        this.maxChargeMultiplier = maxChargeMultiplier;
    }

    public static FocusStaffbowServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("FocusStaffbow");
        var maxChargeMultiplier = builder.defineInRange("max_charge_multiplier", 10.0D, 1.0D, 100.0D);
        builder.pop();

        return new FocusStaffbowServerConfig(maxChargeMultiplier);
    }

    public double maxChargeMultiplier() {
        return maxChargeMultiplier.get();
    }
}
