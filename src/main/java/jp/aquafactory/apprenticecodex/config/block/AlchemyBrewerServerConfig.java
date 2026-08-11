package jp.aquafactory.apprenticecodex.config.block;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

public final class AlchemyBrewerServerConfig {
    public static final Values DEFAULT_VALUES = new Values(20, 1, 250);

    private final ModConfigSpec.IntValue waterSupplyIntervalTicks;
    private final ModConfigSpec.IntValue vanillaCauldronWaterLevelIncrease;
    private final ModConfigSpec.IntValue alchemistCauldronWaterAmountMb;
    private @Nullable Values override;

    private AlchemyBrewerServerConfig(
            ModConfigSpec.IntValue waterSupplyIntervalTicks,
            ModConfigSpec.IntValue vanillaCauldronWaterLevelIncrease,
            ModConfigSpec.IntValue alchemistCauldronWaterAmountMb
    ) {
        this.waterSupplyIntervalTicks = waterSupplyIntervalTicks;
        this.vanillaCauldronWaterLevelIncrease = vanillaCauldronWaterLevelIncrease;
        this.alchemistCauldronWaterAmountMb = alchemistCauldronWaterAmountMb;
    }

    public static AlchemyBrewerServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("AlchemyBrewer");
        var waterSupplyIntervalTicks = builder
                .comment("Interval in ticks between wireless water supply attempts. Minimum is 10 ticks.")
                .defineInRange("waterSupplyIntervalTicks", DEFAULT_VALUES.waterSupplyIntervalTicks(), 10, Integer.MAX_VALUE);
        var vanillaCauldronWaterLevelIncrease = builder
                .comment("Vanilla cauldron water levels supplied per attempt. Set to 0 to exclude vanilla cauldrons.")
                .defineInRange("vanillaCauldronWaterLevelIncrease", DEFAULT_VALUES.vanillaCauldronWaterLevelIncrease(), 0, 3);
        var alchemistCauldronWaterAmountMb = builder
                .comment("Water supplied to an Alchemist Cauldron per attempt. Set to 0 to exclude it.")
                .defineInRange("alchemistCauldronWaterAmountMb", DEFAULT_VALUES.alchemistCauldronWaterAmountMb(), 0, 1000);
        builder.pop();

        return new AlchemyBrewerServerConfig(
                waterSupplyIntervalTicks,
                vanillaCauldronWaterLevelIncrease,
                alchemistCauldronWaterAmountMb
        );
    }

    public Values values() {
        return override != null
                ? override
                : new Values(
                        waterSupplyIntervalTicks.get(),
                        vanillaCauldronWaterLevelIncrease.get(),
                        alchemistCauldronWaterAmountMb.get()
                );
    }

    public void setForGameTest(@Nullable Values values) {
        override = values;
    }

    public record Values(
            int waterSupplyIntervalTicks,
            int vanillaCauldronWaterLevelIncrease,
            int alchemistCauldronWaterAmountMb
    ) {
    }
}
