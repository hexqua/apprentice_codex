package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PastelStaffServerConfig {
    private final ModConfigSpec.IntValue amplifyTintedMagicPercent;

    private PastelStaffServerConfig(ModConfigSpec.IntValue amplifyTintedMagicPercent) {
        this.amplifyTintedMagicPercent = amplifyTintedMagicPercent;
    }

    public static PastelStaffServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("PastelStaff");
        var amplifyTintedMagicPercent = builder.defineInRange("amplify_tinted_magic", 20, 0, 1000);
        builder.pop();

        return new PastelStaffServerConfig(amplifyTintedMagicPercent);
    }

    public double amplifyTintedMagicMultiplier() {
        return amplifyTintedMagicPercent.get() / 100.0d;
    }
}

