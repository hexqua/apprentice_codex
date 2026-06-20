package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MagicArmorServerConfig {
    private final ModConfigSpec.DoubleValue apprenticeMageRobeSpellPowerBonusPerPiece;
    private final ModConfigSpec.DoubleValue enchantressRobeSpellPowerBonusPerPiece;
    private final ModConfigSpec.DoubleValue chromaticMagiaDressSpellPowerBonusPerPiece;
    private final ModConfigSpec.DoubleValue elementMaidenRobeSpellPowerBonus;
    private final ModConfigSpec.DoubleValue elementMaidenRobeSchoolSpellPowerBonus;
    private final ModConfigSpec.DoubleValue magiAgentSuitSpellPowerBonus;
    private final ModConfigSpec.DoubleValue magiAgentSuitSchoolSpellPowerBonus;
    private final ModConfigSpec.DoubleValue magiAgentSuitAmmoNoConsumeChance;
    private final ModConfigSpec.BooleanValue magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed;
    private final ModConfigSpec.DoubleValue stealthRuneArmorSpellPowerBonusPerPiece;

    private Double elementMaidenRobeSchoolSpellPowerBonusOverride;
    private Double magiAgentSuitAmmoNoConsumeChanceOverride;
    private Boolean magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumedOverride;

    private MagicArmorServerConfig(
            ModConfigSpec.DoubleValue apprenticeMageRobeSpellPowerBonusPerPiece,
            ModConfigSpec.DoubleValue enchantressRobeSpellPowerBonusPerPiece,
            ModConfigSpec.DoubleValue chromaticMagiaDressSpellPowerBonusPerPiece,
            ModConfigSpec.DoubleValue elementMaidenRobeSpellPowerBonus,
            ModConfigSpec.DoubleValue elementMaidenRobeSchoolSpellPowerBonus,
            ModConfigSpec.DoubleValue magiAgentSuitSpellPowerBonus,
            ModConfigSpec.DoubleValue magiAgentSuitSchoolSpellPowerBonus,
            ModConfigSpec.DoubleValue magiAgentSuitAmmoNoConsumeChance,
            ModConfigSpec.BooleanValue magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed,
            ModConfigSpec.DoubleValue stealthRuneArmorSpellPowerBonusPerPiece
    ) {
        this.apprenticeMageRobeSpellPowerBonusPerPiece = apprenticeMageRobeSpellPowerBonusPerPiece;
        this.enchantressRobeSpellPowerBonusPerPiece = enchantressRobeSpellPowerBonusPerPiece;
        this.chromaticMagiaDressSpellPowerBonusPerPiece = chromaticMagiaDressSpellPowerBonusPerPiece;
        this.elementMaidenRobeSpellPowerBonus = elementMaidenRobeSpellPowerBonus;
        this.elementMaidenRobeSchoolSpellPowerBonus = elementMaidenRobeSchoolSpellPowerBonus;
        this.magiAgentSuitSpellPowerBonus = magiAgentSuitSpellPowerBonus;
        this.magiAgentSuitSchoolSpellPowerBonus = magiAgentSuitSchoolSpellPowerBonus;
        this.magiAgentSuitAmmoNoConsumeChance = magiAgentSuitAmmoNoConsumeChance;
        this.magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed =
                magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed;
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
        var magiAgentSuitSpellPowerBonus = defineSpellPowerBonus(
                builder,
                "MagiAgentSuit",
                0.05D
        );
        var magiAgentSuitSchoolSpellPowerBonus = defineSchoolRuneSpellPowerBonus(
                builder,
                "MagiAgentSuit",
                0.10D
        );
        var magiAgentSuitAmmoNoConsumeChance = defineMagiAgentSuitAmmoNoConsumeChance(builder);
        var magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed =
                defineMagiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed(builder);
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
                magiAgentSuitSpellPowerBonus,
                magiAgentSuitSchoolSpellPowerBonus,
                magiAgentSuitAmmoNoConsumeChance,
                magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed,
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

    public double magiAgentSuitSpellPowerBonus() {
        return magiAgentSuitSpellPowerBonus.get();
    }

    public double magiAgentSuitSchoolSpellPowerBonus() {
        return magiAgentSuitSchoolSpellPowerBonus.get();
    }

    public double magiAgentSuitAmmoNoConsumeChance() {
        return magiAgentSuitAmmoNoConsumeChanceOverride == null
                ? magiAgentSuitAmmoNoConsumeChance.get()
                : magiAgentSuitAmmoNoConsumeChanceOverride;
    }

    public boolean magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed() {
        return magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumedOverride == null
                ? magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed.get()
                : magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumedOverride;
    }

    public double stealthRuneArmorSpellPowerBonusPerPiece() {
        return stealthRuneArmorSpellPowerBonusPerPiece.get();
    }

    public void setElementMaidenRobeSchoolSpellPowerBonusForGameTest(double value) {
        elementMaidenRobeSchoolSpellPowerBonusOverride = value;
    }

    public void setMagiAgentSuitAmmoConfigForGameTest(
            double ammoNoConsumeChance,
            boolean skipStaffrifleManaCostWhenAmmoNotConsumed
    ) {
        magiAgentSuitAmmoNoConsumeChanceOverride = ammoNoConsumeChance;
        magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumedOverride =
                skipStaffrifleManaCostWhenAmmoNotConsumed;
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

    private static ModConfigSpec.DoubleValue defineSpellPowerBonus(
            ModConfigSpec.Builder builder,
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

    private static ModConfigSpec.DoubleValue defineSchoolSpellPowerBonus(
            ModConfigSpec.Builder builder,
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

    private static ModConfigSpec.DoubleValue defineSchoolRuneSpellPowerBonus(
            ModConfigSpec.Builder builder,
            String sectionName,
            double defaultValue
    ) {
        builder.comment("School spell power bonus amount from a school rune inserted at the Spell Calibration Bench. 0.01 = +1%.")
                .push(sectionName);
        var value = builder.defineInRange(
                "schoolRuneSpellPowerBonus",
                defaultValue,
                0.0D,
                10.0D
        );
        builder.pop();
        return value;
    }

    private static ModConfigSpec.DoubleValue defineMagiAgentSuitAmmoNoConsumeChance(ModConfigSpec.Builder builder) {
        builder.comment("Chance for Magi Agent Suit Hood to prevent Spellgun and Staffrifle ammo consumption. 1.0 = always, 0.0 = disabled.")
                .push("MagiAgentSuit");
        var value = builder.defineInRange(
                "ammoNoConsumeChance",
                0.5D,
                0.0D,
                1.0D
        );
        builder.pop();
        return value;
    }

    private static ModConfigSpec.BooleanValue defineMagiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed(
            ModConfigSpec.Builder builder
    ) {
        builder.comment("When Magi Agent Suit Hood prevents Staffrifle ammo consumption, also prevent that Staffrifle mana cost.")
                .push("MagiAgentSuit");
        var value = builder.define(
                "skipStaffrifleManaCostWhenAmmoNotConsumed",
                true
        );
        builder.pop();
        return value;
    }
}
