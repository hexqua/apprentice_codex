package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ChargecastCatalystbookServerConfig {
    private final ForgeConfigSpec.IntValue castTimeTicks;
    private final ForgeConfigSpec.DoubleValue spellPowerMultiplier;
    private final ForgeConfigSpec.DoubleValue silverRingCastTimeBonusFactor;
    private Values override;

    private ChargecastCatalystbookServerConfig(
            ForgeConfigSpec.IntValue castTimeTicks,
            ForgeConfigSpec.DoubleValue spellPowerMultiplier,
            ForgeConfigSpec.DoubleValue silverRingCastTimeBonusFactor
    ) {
        this.castTimeTicks = castTimeTicks;
        this.spellPowerMultiplier = spellPowerMultiplier;
        this.silverRingCastTimeBonusFactor = silverRingCastTimeBonusFactor;
    }

    public static ChargecastCatalystbookServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("ChargecastCatalystbook");
        var castTimeTicks = builder
                .comment("Base casting time added to instant spells. 20 ticks = 1 second.")
                .defineInRange("castTimeTicks", 30, 1, 72000);
        var spellPowerMultiplier = builder
                .comment("Final spell power multiplier applied when the charged cast completes.")
                .defineInRange("spellPowerMultiplier", 1.2D, 1.0D, 100.0D);
        var silverRingCastTimeBonusFactor = builder
                .comment("Extra final spell power gained from cast time reduction when a Silver Ring is installed.")
                .defineInRange("silverRingCastTimeBonusFactor", 0.2D, 0.0D, 100.0D);
        builder.pop();
        return new ChargecastCatalystbookServerConfig(
                castTimeTicks,
                spellPowerMultiplier,
                silverRingCastTimeBonusFactor
        );
    }

    public Values values() {
        return override != null
                ? override
                : new Values(castTimeTicks.get(), spellPowerMultiplier.get(), silverRingCastTimeBonusFactor.get());
    }

    public void setForGameTest(Values values) {
        override = values;
    }

    public record Values(int castTimeTicks, double spellPowerMultiplier, double silverRingCastTimeBonusFactor) {
        public static final Values DEFAULT = new Values(30, 1.2D, 0.2D);
    }
}
