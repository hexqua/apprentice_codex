package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BulwarkGreatshieldServerConfig {
    private final ForgeConfigSpec.DoubleValue genericSpellResist;
    private final ForgeConfigSpec.DoubleValue schoolSpellResist;
    private Double genericSpellResistOverride;
    private Double schoolSpellResistOverride;

    private BulwarkGreatshieldServerConfig(
            ForgeConfigSpec.DoubleValue genericSpellResist,
            ForgeConfigSpec.DoubleValue schoolSpellResist
    ) {
        this.genericSpellResist = genericSpellResist;
        this.schoolSpellResist = schoolSpellResist;
    }

    public static BulwarkGreatshieldServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("BulwarkGreatshield");
        var genericSpellResist = builder
                .comment("Generic spell resistance while Bulwark Greatshield is equipped. 0.2 = +20%.")
                .defineInRange("genericSpellResist", 0.2D, 0.0D, 1.0D);
        var schoolSpellResist = builder
                .comment("Spell resistance granted per inserted School Rune. 0.2 = +20% per rune.")
                .defineInRange("schoolSpellResist", 0.2D, 0.0D, 1.0D);
        builder.pop();
        return new BulwarkGreatshieldServerConfig(genericSpellResist, schoolSpellResist);
    }

    public double genericSpellResist() {
        return genericSpellResistOverride == null ? genericSpellResist.get() : genericSpellResistOverride;
    }

    public double schoolSpellResist() {
        return schoolSpellResistOverride == null ? schoolSpellResist.get() : schoolSpellResistOverride;
    }

    public void setForGameTest(double genericSpellResist, double schoolSpellResist) {
        genericSpellResistOverride = genericSpellResist;
        schoolSpellResistOverride = schoolSpellResist;
    }
}
