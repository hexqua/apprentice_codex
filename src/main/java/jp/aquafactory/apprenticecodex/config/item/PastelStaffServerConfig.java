package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class PastelStaffServerConfig {
    private final ForgeConfigSpec.IntValue amplifyTintedMagicPercent;

    private PastelStaffServerConfig(ForgeConfigSpec.IntValue amplifyTintedMagicPercent) {
        this.amplifyTintedMagicPercent = amplifyTintedMagicPercent;
    }

    public static PastelStaffServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("PastelStaff");
        var amplifyTintedMagicPercent = builder.defineInRange("amplify_tinted_magic", 20, 0, 1000);
        builder.pop();

        return new PastelStaffServerConfig(amplifyTintedMagicPercent);
    }

    public double amplifyTintedMagicMultiplier() {
        return amplifyTintedMagicPercent.get() / 100.0d;
    }
}
