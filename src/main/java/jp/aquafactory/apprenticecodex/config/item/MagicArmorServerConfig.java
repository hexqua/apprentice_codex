package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MagicArmorServerConfig {
    private final ForgeConfigSpec.DoubleValue apprenticeMageRobeSpellPowerBonusPerPiece;
    private final ForgeConfigSpec.DoubleValue enchantressRobeSpellPowerBonusPerPiece;
    private final ForgeConfigSpec.DoubleValue chromaticMagiaDressSpellPowerBonusPerPiece;
    private final ForgeConfigSpec.DoubleValue elementMaidenRobeSpellPowerBonus;
    private final ForgeConfigSpec.DoubleValue elementMaidenRobeSchoolSpellPowerBonus;
    private final ForgeConfigSpec.DoubleValue stealthRuneArmorSpellPowerBonusPerPiece;

    private Double elementMaidenRobeSchoolSpellPowerBonusOverride;

    private MagicArmorServerConfig(
            ForgeConfigSpec.DoubleValue apprenticeMageRobeSpellPowerBonusPerPiece,
            ForgeConfigSpec.DoubleValue enchantressRobeSpellPowerBonusPerPiece,
            ForgeConfigSpec.DoubleValue chromaticMagiaDressSpellPowerBonusPerPiece,
            ForgeConfigSpec.DoubleValue elementMaidenRobeSpellPowerBonus,
            ForgeConfigSpec.DoubleValue elementMaidenRobeSchoolSpellPowerBonus,
            ForgeConfigSpec.DoubleValue stealthRuneArmorSpellPowerBonusPerPiece
    ) {
        this.apprenticeMageRobeSpellPowerBonusPerPiece = apprenticeMageRobeSpellPowerBonusPerPiece;
        this.enchantressRobeSpellPowerBonusPerPiece = enchantressRobeSpellPowerBonusPerPiece;
        this.chromaticMagiaDressSpellPowerBonusPerPiece = chromaticMagiaDressSpellPowerBonusPerPiece;
        this.elementMaidenRobeSpellPowerBonus = elementMaidenRobeSpellPowerBonus;
        this.elementMaidenRobeSchoolSpellPowerBonus = elementMaidenRobeSchoolSpellPowerBonus;
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
        var elementMaidenRobeSpellPowerBonus = defineSpellPowerBonus(
                builder,
                "ElementMaidenRobe",
                0.15D
        );
        var elementMaidenRobeSchoolSpellPowerBonus = defineSchoolSpellPowerBonus(
                builder,
                "ElementMaidenRobe",
                0.20D
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
                elementMaidenRobeSpellPowerBonus,
                elementMaidenRobeSchoolSpellPowerBonus,
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

    public double elementMaidenRobeSpellPowerBonus() {
        return elementMaidenRobeSpellPowerBonus.get();
    }

    public double elementMaidenRobeSchoolSpellPowerBonus() {
        return elementMaidenRobeSchoolSpellPowerBonusOverride == null
                ? elementMaidenRobeSchoolSpellPowerBonus.get()
                : elementMaidenRobeSchoolSpellPowerBonusOverride;
    }

    public double stealthRuneArmorSpellPowerBonusPerPiece() {
        return stealthRuneArmorSpellPowerBonusPerPiece.get();
    }

    public void setElementMaidenRobeSchoolSpellPowerBonusForGameTest(double value) {
        elementMaidenRobeSchoolSpellPowerBonusOverride = value;
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

    private static ForgeConfigSpec.DoubleValue defineSpellPowerBonus(
            ForgeConfigSpec.Builder builder,
            String sectionName,
            double defaultValue
    ) {
        builder.comment("Global spell power attribute modifier amount shared by each armor piece. 0.01 = +1%.")
                .push(sectionName);
        var value = builder.defineInRange(
                "spellPowerBonus",
                defaultValue,
                0.0D,
                10.0D
        );
        builder.pop();
        return value;
    }

    private static ForgeConfigSpec.DoubleValue defineSchoolSpellPowerBonus(
            ForgeConfigSpec.Builder builder,
            String sectionName,
            double defaultValue
    ) {
        builder.comment("School spell power bonus amount distributed from the equipped spellbook while wearing Element Maiden Robe. 0.01 = +1%.")
                .push(sectionName);
        var value = builder.defineInRange(
                "schoolSpellPowerBonus",
                defaultValue,
                0.0D,
                10.0D
        );
        builder.pop();
        return value;
    }
}
