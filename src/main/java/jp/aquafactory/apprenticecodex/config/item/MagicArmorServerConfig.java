package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MagicArmorServerConfig {
    private final ModConfigSpec.DoubleValue apprenticeMageRobeSpellPowerBonusPerPiece;
    private final ModConfigSpec.DoubleValue enchantressRobeSpellPowerBonusPerPiece;
    private final ModConfigSpec.DoubleValue chromaticMagiaDressSpellPowerBonusPerPiece;
    private final ModConfigSpec.DoubleValue stealthRuneArmorSpellPowerBonusPerPiece;

    private MagicArmorServerConfig(
            ModConfigSpec.DoubleValue apprenticeMageRobeSpellPowerBonusPerPiece,
            ModConfigSpec.DoubleValue enchantressRobeSpellPowerBonusPerPiece,
            ModConfigSpec.DoubleValue chromaticMagiaDressSpellPowerBonusPerPiece,
            ModConfigSpec.DoubleValue stealthRuneArmorSpellPowerBonusPerPiece
    ) {
        this.apprenticeMageRobeSpellPowerBonusPerPiece = apprenticeMageRobeSpellPowerBonusPerPiece;
        this.enchantressRobeSpellPowerBonusPerPiece = enchantressRobeSpellPowerBonusPerPiece;
        this.chromaticMagiaDressSpellPowerBonusPerPiece = chromaticMagiaDressSpellPowerBonusPerPiece;
        this.stealthRuneArmorSpellPowerBonusPerPiece = stealthRuneArmorSpellPowerBonusPerPiece;
    }

    public static MagicArmorServerConfig define(ModConfigSpec.Builder builder) {
        var apprenticeMageRobeSpellPowerBonusPerPiece = defineSpellPowerBonusPerPiece(
                builder,
                "ApprenticeMageRobe",
                0.05D
        );
        var enchantressRobeSpellPowerBonusPerPiece = defineSpellPowerBonusPerPiece(
                builder,
                "EnchantressRobe",
                0.10D
        );
        var chromaticMagiaDressSpellPowerBonusPerPiece = defineSpellPowerBonusPerPiece(
                builder,
                "ChromaticMagiaDress",
                0.10D
        );
        var stealthRuneArmorSpellPowerBonusPerPiece = defineSpellPowerBonusPerPiece(
                builder,
                "StealthRuneArmor",
                0.05D
        );

        return new MagicArmorServerConfig(
                apprenticeMageRobeSpellPowerBonusPerPiece,
                enchantressRobeSpellPowerBonusPerPiece,
                chromaticMagiaDressSpellPowerBonusPerPiece,
                stealthRuneArmorSpellPowerBonusPerPiece
        );
    }

    public double apprenticeMageRobeSpellPowerBonusPerPiece() {
        return apprenticeMageRobeSpellPowerBonusPerPiece.get();
    }

    public double enchantressRobeSpellPowerBonusPerPiece() {
        return enchantressRobeSpellPowerBonusPerPiece.get();
    }

    public double chromaticMagiaDressSpellPowerBonusPerPiece() {
        return chromaticMagiaDressSpellPowerBonusPerPiece.get();
    }

    public double stealthRuneArmorSpellPowerBonusPerPiece() {
        return stealthRuneArmorSpellPowerBonusPerPiece.get();
    }

    private static ModConfigSpec.DoubleValue defineSpellPowerBonusPerPiece(
            ModConfigSpec.Builder builder,
            String sectionName,
            double defaultValue
    ) {
        builder.comment("Global spell power attribute modifier amount per armor piece. 0.01 = +1%.")
                .push(sectionName);
        var value = builder.defineInRange(
                "spellPowerBonusPerPiece",
                defaultValue,
                0.0D,
                10.0D
        );
        builder.pop();
        return value;
    }
}
