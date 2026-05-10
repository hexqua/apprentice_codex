package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ChromaticMagiaDressServerConfig {
    private final ModConfigSpec.DoubleValue schoolSpellPowerBonusPerHistory;

    private ChromaticMagiaDressServerConfig(ModConfigSpec.DoubleValue schoolSpellPowerBonusPerHistory) {
        this.schoolSpellPowerBonusPerHistory = schoolSpellPowerBonusPerHistory;
    }

    public static ChromaticMagiaDressServerConfig define(ModConfigSpec.Builder builder) {
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
