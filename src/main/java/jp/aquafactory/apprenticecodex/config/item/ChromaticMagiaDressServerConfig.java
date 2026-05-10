package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ChromaticMagiaDressServerConfig {
    private final ForgeConfigSpec.DoubleValue schoolSpellPowerBonusPerHistory;

    private ChromaticMagiaDressServerConfig(ForgeConfigSpec.DoubleValue schoolSpellPowerBonusPerHistory) {
        this.schoolSpellPowerBonusPerHistory = schoolSpellPowerBonusPerHistory;
    }

    public static ChromaticMagiaDressServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.comment("Spell power attribute modifier amount per recorded school history. 0.01 = +1%.")
                .push("ChromaticMagiaDress");

        var schoolSpellPowerBonusPerHistory = builder.defineInRange(
                "schoolSpellPowerBonusPerHistory",
                0.01d,
                0.0d,
                10.0d
        );

        builder.pop();
        return new ChromaticMagiaDressServerConfig(schoolSpellPowerBonusPerHistory);
    }

    public double schoolSpellPowerBonusPerHistory() {
        return schoolSpellPowerBonusPerHistory.get();
    }
}
