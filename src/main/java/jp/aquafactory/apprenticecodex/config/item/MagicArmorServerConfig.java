package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MagicArmorServerConfig {
    private final ForgeConfigSpec.DoubleValue apprenticeMageRobeSpellPowerBonusPerPiece;
    private final ForgeConfigSpec.DoubleValue enchantressRobeSpellPowerBonusPerPiece;
    private final ForgeConfigSpec.DoubleValue soulcollectorRobeSpellPowerBonusPerPiece;
    private final ForgeConfigSpec.DoubleValue soulcollectorRobeMagicProficiencyBonusPerPiece;
    private final ForgeConfigSpec.DoubleValue chromaticMagiaDressSpellPowerBonusPerPiece;
    private final ForgeConfigSpec.DoubleValue elementMaidenRobeSpellPowerBonus;
    private final ForgeConfigSpec.DoubleValue elementMaidenRobeSchoolSpellPowerBonus;
    private final ForgeConfigSpec.DoubleValue magiAgentSuitSpellPowerBonus;
    private final ForgeConfigSpec.DoubleValue magiAgentSuitSchoolSpellPowerBonus;
    private final ForgeConfigSpec.DoubleValue magiAgentSuitBootsCooldownMultiplier;
    private final ForgeConfigSpec.DoubleValue magiAgentSuitBootsCastTimeMultiplier;
    private final ForgeConfigSpec.DoubleValue magiAgentSuitAmmoNoConsumeChance;
    private final ForgeConfigSpec.BooleanValue magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed;
    private final ForgeConfigSpec.DoubleValue stealthRuneArmorSpellPowerBonusPerPiece;

    private Double elementMaidenRobeSchoolSpellPowerBonusOverride;
    private Double magiAgentSuitBootsCooldownMultiplierOverride;
    private Double magiAgentSuitBootsCastTimeMultiplierOverride;
    private Double magiAgentSuitAmmoNoConsumeChanceOverride;
    private Boolean magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumedOverride;

    private MagicArmorServerConfig(
            ForgeConfigSpec.DoubleValue apprenticeMageRobeSpellPowerBonusPerPiece,
            ForgeConfigSpec.DoubleValue enchantressRobeSpellPowerBonusPerPiece,
            ForgeConfigSpec.DoubleValue soulcollectorRobeSpellPowerBonusPerPiece,
            ForgeConfigSpec.DoubleValue soulcollectorRobeMagicProficiencyBonusPerPiece,
            ForgeConfigSpec.DoubleValue chromaticMagiaDressSpellPowerBonusPerPiece,
            ForgeConfigSpec.DoubleValue elementMaidenRobeSpellPowerBonus,
            ForgeConfigSpec.DoubleValue elementMaidenRobeSchoolSpellPowerBonus,
            ForgeConfigSpec.DoubleValue magiAgentSuitSpellPowerBonus,
            ForgeConfigSpec.DoubleValue magiAgentSuitSchoolSpellPowerBonus,
            ForgeConfigSpec.DoubleValue magiAgentSuitBootsCooldownMultiplier,
            ForgeConfigSpec.DoubleValue magiAgentSuitBootsCastTimeMultiplier,
            ForgeConfigSpec.DoubleValue magiAgentSuitAmmoNoConsumeChance,
            ForgeConfigSpec.BooleanValue magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed,
            ForgeConfigSpec.DoubleValue stealthRuneArmorSpellPowerBonusPerPiece
    ) {
        this.apprenticeMageRobeSpellPowerBonusPerPiece = apprenticeMageRobeSpellPowerBonusPerPiece;
        this.enchantressRobeSpellPowerBonusPerPiece = enchantressRobeSpellPowerBonusPerPiece;
        this.soulcollectorRobeSpellPowerBonusPerPiece = soulcollectorRobeSpellPowerBonusPerPiece;
        this.soulcollectorRobeMagicProficiencyBonusPerPiece = soulcollectorRobeMagicProficiencyBonusPerPiece;
        this.chromaticMagiaDressSpellPowerBonusPerPiece = chromaticMagiaDressSpellPowerBonusPerPiece;
        this.elementMaidenRobeSpellPowerBonus = elementMaidenRobeSpellPowerBonus;
        this.elementMaidenRobeSchoolSpellPowerBonus = elementMaidenRobeSchoolSpellPowerBonus;
        this.magiAgentSuitSpellPowerBonus = magiAgentSuitSpellPowerBonus;
        this.magiAgentSuitSchoolSpellPowerBonus = magiAgentSuitSchoolSpellPowerBonus;
        this.magiAgentSuitBootsCooldownMultiplier = magiAgentSuitBootsCooldownMultiplier;
        this.magiAgentSuitBootsCastTimeMultiplier = magiAgentSuitBootsCastTimeMultiplier;
        this.magiAgentSuitAmmoNoConsumeChance = magiAgentSuitAmmoNoConsumeChance;
        this.magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed =
                magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed;
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
        var soulcollectorRobeSpellPowerBonusPerPiece = defineSpellPowerBonusPerPiece(
                builder,
                "SoulcollectorRobe",
                0.10D
        );
        var soulcollectorRobeMagicProficiencyBonusPerPiece = defineMagicProficiencyBonusPerPiece(
                builder,
                "SoulcollectorRobe",
                0.15D
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
        var magiAgentSuitBootsCooldownMultiplier = defineMagiAgentSuitBootsCooldownMultiplier(builder);
        var magiAgentSuitBootsCastTimeMultiplier = defineMagiAgentSuitBootsCastTimeMultiplier(builder);
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
                soulcollectorRobeSpellPowerBonusPerPiece,
                soulcollectorRobeMagicProficiencyBonusPerPiece,
                chromaticMagiaDressSpellPowerBonusPerPiece,
                elementMaidenRobeSpellPowerBonus,
                elementMaidenRobeSchoolSpellPowerBonus,
                magiAgentSuitSpellPowerBonus,
                magiAgentSuitSchoolSpellPowerBonus,
                magiAgentSuitBootsCooldownMultiplier,
                magiAgentSuitBootsCastTimeMultiplier,
                magiAgentSuitAmmoNoConsumeChance,
                magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed,
                stealthRuneArmorSpellPowerBonusPerPiece);
    }

    public double apprenticeMageRobeSpellPowerBonusPerPiece() {
        return apprenticeMageRobeSpellPowerBonusPerPiece.get();
    }

    public double enchantressRobeSpellPowerBonusPerPiece() {
        return enchantressRobeSpellPowerBonusPerPiece.get();
    }

    public double soulcollectorRobeSpellPowerBonusPerPiece() {
        return soulcollectorRobeSpellPowerBonusPerPiece.get();
    }

    public double soulcollectorRobeMagicProficiencyBonusPerPiece() {
        return soulcollectorRobeMagicProficiencyBonusPerPiece.get();
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

    public double magiAgentSuitBootsCooldownMultiplier() {
        return magiAgentSuitBootsCooldownMultiplierOverride == null
                ? magiAgentSuitBootsCooldownMultiplier.get()
                : magiAgentSuitBootsCooldownMultiplierOverride;
    }

    public double magiAgentSuitBootsCastTimeMultiplier() {
        return magiAgentSuitBootsCastTimeMultiplierOverride == null
                ? magiAgentSuitBootsCastTimeMultiplier.get()
                : magiAgentSuitBootsCastTimeMultiplierOverride;
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

    public void setMagiAgentSuitBootsCooldownMultiplierForGameTest(double value) {
        magiAgentSuitBootsCooldownMultiplierOverride = value;
    }

    public void setMagiAgentSuitBootsCastTimeMultiplierForGameTest(double value) {
        magiAgentSuitBootsCastTimeMultiplierOverride = value;
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

    private static ForgeConfigSpec.DoubleValue defineMagicProficiencyBonusPerPiece(
            ForgeConfigSpec.Builder builder,
            String sectionName,
            double defaultValue
    ) {
        builder.comment("Lodestone magic proficiency attribute modifier amount shared by each armor piece. 0.01 = +1%.")
                .push(sectionName);
        var value = builder.defineInRange(
                "magicProficiencyBonus",
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

    private static ForgeConfigSpec.DoubleValue defineSchoolRuneSpellPowerBonus(
            ForgeConfigSpec.Builder builder,
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

    private static ForgeConfigSpec.DoubleValue defineMagiAgentSuitAmmoNoConsumeChance(ForgeConfigSpec.Builder builder) {
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

    private static ForgeConfigSpec.DoubleValue defineMagiAgentSuitBootsCooldownMultiplier(ForgeConfigSpec.Builder builder) {
        builder.push("MagiAgentSuit");
        var value = builder
                .comment("Multiplier applied by Magi Agent Suit Boots to target spell cooldowns. 0.5 = 50%. A value of 0.0 still leaves a minimum cooldown of 1 tick.")
                .defineInRange(
                        "bootsCooldownMultiplier",
                        0.5D,
                        0.0D,
                        1.0D
                );
        builder.pop();
        return value;
    }

    private static ForgeConfigSpec.DoubleValue defineMagiAgentSuitBootsCastTimeMultiplier(ForgeConfigSpec.Builder builder) {
        builder.push("MagiAgentSuit");
        var value = builder
                .comment("Multiplier applied by Magi Agent Suit Boots to target LONG spell cast times. 0.5 = 50%. A value of 0.0 still leaves a minimum cast time of 1 tick.")
                .defineInRange(
                        "bootsCastTimeMultiplier",
                        0.5D,
                        0.0D,
                        1.0D
                );
        builder.pop();
        return value;
    }

    private static ForgeConfigSpec.BooleanValue defineMagiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed(
            ForgeConfigSpec.Builder builder
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
