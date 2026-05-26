package jp.aquafactory.apprenticecodex.config.item;

import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class SpellStainedRunicTabletServerConfig {
    private static final double MIN_BONUS_VALUE = -10000.0D;
    private static final double MAX_BONUS_VALUE = 10000.0D;
    private static final int MIN_THRESHOLD_VALUE = -10000;
    private static final int MAX_THRESHOLD_VALUE = 10000;

    private final ConfiguredRarityBonuses maxMana;
    private final ConfiguredRarityBonuses schoolSpellPower;
    private final ConfiguredRarityBonuses generalSpellPower;
    private final ModConfigSpec.IntValue cooldownReductionMinimumSchoolCount;
    private final ModConfigSpec.DoubleValue cooldownReductionPerSchool;
    private final ModConfigSpec.DoubleValue cooldownReductionFlat;
    private final ModConfigSpec.IntValue castTimeReductionMinimumDuplicateCount;
    private final ModConfigSpec.DoubleValue castTimeReductionPerDuplicate;
    private final ModConfigSpec.DoubleValue castTimeReductionFlat;

    private Values override;

    private SpellStainedRunicTabletServerConfig(
            ConfiguredRarityBonuses maxMana,
            ConfiguredRarityBonuses schoolSpellPower,
            ConfiguredRarityBonuses generalSpellPower,
            ModConfigSpec.IntValue cooldownReductionMinimumSchoolCount,
            ModConfigSpec.DoubleValue cooldownReductionPerSchool,
            ModConfigSpec.DoubleValue cooldownReductionFlat,
            ModConfigSpec.IntValue castTimeReductionMinimumDuplicateCount,
            ModConfigSpec.DoubleValue castTimeReductionPerDuplicate,
            ModConfigSpec.DoubleValue castTimeReductionFlat
    ) {
        this.maxMana = maxMana;
        this.schoolSpellPower = schoolSpellPower;
        this.generalSpellPower = generalSpellPower;
        this.cooldownReductionMinimumSchoolCount = cooldownReductionMinimumSchoolCount;
        this.cooldownReductionPerSchool = cooldownReductionPerSchool;
        this.cooldownReductionFlat = cooldownReductionFlat;
        this.castTimeReductionMinimumDuplicateCount = castTimeReductionMinimumDuplicateCount;
        this.castTimeReductionPerDuplicate = castTimeReductionPerDuplicate;
        this.castTimeReductionFlat = castTimeReductionFlat;
    }

    public static SpellStainedRunicTabletServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("SpellStainedRunicTablet");
        var maxMana = defineRarityBonuses(builder, "maxMana", new RarityBonuses(
                10.0D,
                10.0D,
                10.0D,
                15.0D,
                20.0D,
                50.0D
        ));
        var schoolSpellPower = defineRarityBonuses(builder, "schoolSpellPower", new RarityBonuses(
                0.01D,
                0.01D,
                0.01D,
                0.02D,
                0.02D,
                0.05D
        ));
        var generalSpellPower = defineRarityBonuses(builder, "generalSpellPower", new RarityBonuses(
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.01D,
                0.01D
        ));

        builder.push("cooldownReduction");
        var cooldownReductionMinimumSchoolCount = builder
                .comment("Minimum distinct school count required before Spell-stained Runic Tablet grants cooldown reduction.")
                .defineInRange("minimumSchoolCount", 5, MIN_THRESHOLD_VALUE, MAX_THRESHOLD_VALUE);
        var cooldownReductionPerSchool = builder
                .comment("Cooldown reduction added per distinct school on Spell-stained Runic Tablet. Negative values are allowed.")
                .defineInRange("perSchool", 0.02D, MIN_BONUS_VALUE, MAX_BONUS_VALUE);
        var cooldownReductionFlat = builder
                .comment("Flat cooldown reduction added when the distinct school count threshold is met. Negative values are allowed.")
                .defineInRange("flat", -0.05D, MIN_BONUS_VALUE, MAX_BONUS_VALUE);
        builder.pop();

        builder.push("castTimeReduction");
        var castTimeReductionMinimumDuplicateCount = builder
                .comment("Minimum same-school spell count required before Spell-stained Runic Tablet grants cast time reduction.")
                .defineInRange("minimumDuplicateCount", 4, MIN_THRESHOLD_VALUE, MAX_THRESHOLD_VALUE);
        var castTimeReductionPerDuplicate = builder
                .comment("Cast time reduction added per spell in the most represented school. Negative values are allowed.")
                .defineInRange("perDuplicate", 0.05D, MIN_BONUS_VALUE, MAX_BONUS_VALUE);
        var castTimeReductionFlat = builder
                .comment("Flat cast time reduction added when the same-school spell count threshold is met. Negative values are allowed.")
                .defineInRange("flat", -0.1D, MIN_BONUS_VALUE, MAX_BONUS_VALUE);
        builder.pop();
        builder.pop();

        return new SpellStainedRunicTabletServerConfig(
                maxMana,
                schoolSpellPower,
                generalSpellPower,
                cooldownReductionMinimumSchoolCount,
                cooldownReductionPerSchool,
                cooldownReductionFlat,
                castTimeReductionMinimumDuplicateCount,
                castTimeReductionPerDuplicate,
                castTimeReductionFlat
        );
    }

    public Values values() {
        if (override != null) {
            return override;
        }

        return new Values(
                maxMana.values(),
                schoolSpellPower.values(),
                generalSpellPower.values(),
                new ScalingBonus(
                        cooldownReductionMinimumSchoolCount.get(),
                        cooldownReductionPerSchool.get(),
                        cooldownReductionFlat.get()
                ),
                new ScalingBonus(
                        castTimeReductionMinimumDuplicateCount.get(),
                        castTimeReductionPerDuplicate.get(),
                        castTimeReductionFlat.get()
                )
        );
    }

    public void setForGameTest(Values values) {
        this.override = values;
    }

    private static ConfiguredRarityBonuses defineRarityBonuses(
            ModConfigSpec.Builder builder,
            String path,
            RarityBonuses defaults
    ) {
        builder.push(path);
        var common = defineBonus(builder, "common", defaults.common());
        var uncommon = defineBonus(builder, "uncommon", defaults.uncommon());
        var rare = defineBonus(builder, "rare", defaults.rare());
        var epic = defineBonus(builder, "epic", defaults.epic());
        var legendary = defineBonus(builder, "legendary", defaults.legendary());
        var mythicOver = defineBonus(builder, "mythicOver", defaults.mythicOver());
        builder.pop();
        return new ConfiguredRarityBonuses(common, uncommon, rare, epic, legendary, mythicOver);
    }

    private static ModConfigSpec.DoubleValue defineBonus(ModConfigSpec.Builder builder, String name, double defaultValue) {
        return builder
                .comment("Bonus value for this rarity tier. Negative values are allowed.")
                .defineInRange(name, defaultValue, MIN_BONUS_VALUE, MAX_BONUS_VALUE);
    }

    private record ConfiguredRarityBonuses(
            ModConfigSpec.DoubleValue common,
            ModConfigSpec.DoubleValue uncommon,
            ModConfigSpec.DoubleValue rare,
            ModConfigSpec.DoubleValue epic,
            ModConfigSpec.DoubleValue legendary,
            ModConfigSpec.DoubleValue mythicOver
    ) {
        private RarityBonuses values() {
            return new RarityBonuses(
                    common.get(),
                    uncommon.get(),
                    rare.get(),
                    epic.get(),
                    legendary.get(),
                    mythicOver.get()
            );
        }
    }

    public record Values(
            RarityBonuses maxMana,
            RarityBonuses schoolSpellPower,
            RarityBonuses generalSpellPower,
            ScalingBonus cooldownReduction,
            ScalingBonus castTimeReduction
    ) {
    }

    public record RarityBonuses(
            double common,
            double uncommon,
            double rare,
            double epic,
            double legendary,
            double mythicOver
    ) {
        public double forRarity(SpellRarity rarity) {
            if (rarity == null) {
                return common;
            }

            return switch (rarity.getValue()) {
                case 0 -> common;
                case 1 -> uncommon;
                case 2 -> rare;
                case 3 -> epic;
                case 4 -> legendary;
                default -> mythicOver;
            };
        }
    }

    public record ScalingBonus(int minimumCount, double perCount, double flat) {
        public double resolve(int count) {
            return count >= minimumCount ? count * perCount + flat : 0.0D;
        }
    }
}
