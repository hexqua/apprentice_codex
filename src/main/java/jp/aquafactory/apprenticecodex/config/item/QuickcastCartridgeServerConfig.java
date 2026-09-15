package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class QuickcastCartridgeServerConfig {
    private final ModConfigSpec.DoubleValue multiplier;
    private final ModConfigSpec.IntValue minimum;
    private final ModConfigSpec.IntValue reload;

    private QuickcastCartridgeServerConfig(ModConfigSpec.Builder builder) {
        builder.push("QuickcastScrollCartridge");
        multiplier = builder.comment("Multiplier for the full attribute-adjusted cooldown used to recover one charge.")
                .defineInRange("chargeRecoveryMultiplier", 1.0, Double.MIN_NORMAL, Double.MAX_VALUE);
        minimum = builder.comment("Minimum automatic charge recovery time in ticks.")
                .defineInRange("minimumChargeRecoveryTicks", 60, 1, Integer.MAX_VALUE);
        reload = builder.comment("Time in ticks required to finish a manual reload.")
                .defineInRange("manualReloadTicks", 60, 1, Integer.MAX_VALUE);
        builder.pop();
    }

    public static QuickcastCartridgeServerConfig define(ModConfigSpec.Builder builder) {
        return new QuickcastCartridgeServerConfig(builder);
    }

    public double multiplier() { return multiplier.get(); }
    public int minimumTicks() { return minimum.get(); }
    public int reloadTicks() { return reload.get(); }
}
