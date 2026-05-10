package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MagicArmorServerConfig {
    private final ForgeConfigSpec.DoubleValue apprenticeMageRobeSpellPowerBonusPerPiece;
    private final ForgeConfigSpec.DoubleValue enchantressRobeSpellPowerBonusPerPiece;
    private final ForgeConfigSpec.DoubleValue chromaticMagiaDressSpellPowerBonusPerPiece;
    private final ForgeConfigSpec.DoubleValue stealthRuneArmorSpellPowerBonusPerPiece;

    private MagicArmorServerConfig(
            ForgeConfigSpec.DoubleValue apprenticeMageRobeSpellPowerBonusPerPiece,
            ForgeConfigSpec.DoubleValue enchantressRobeSpellPowerBonusPerPiece,
            ForgeConfigSpec.DoubleValue chromaticMagiaDressSpellPowerBonusPerPiece,
            ForgeConfigSpec.DoubleValue stealthRuneArmorSpellPowerBonusPerPiece
    ) {
        this.apprenticeMageRobeSpellPowerBonusPerPiece = apprenticeMageRobeSpellPowerBonusPerPiece;
        this.enchantressRobeSpellPowerBonusPerPiece = enchantressRobeSpellPowerBonusPerPiece;
        this.chromaticMagiaDressSpellPowerBonusPerPiece = chromaticMagiaDressSpellPowerBonusPerPiece;
        this.stealthRuneArmorSpellPowerBonusPerPiece = stealthRuneArmorSpellPowerBonusPerPiece;
    }

    public static MagicArmorServerConfig define(ForgeConfigSpec.Builder builder) {
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

    private static ForgeConfigSpec.DoubleValue defineSpellPowerBonusPerPiece(
            ForgeConfigSpec.Builder builder,
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
