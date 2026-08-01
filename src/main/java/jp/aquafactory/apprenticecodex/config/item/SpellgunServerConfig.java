package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SpellgunServerConfig {
    private static final int MAX_CONFIGURED_TICKS = 72000;

    private final TierConfig iron;
    private final TierConfig copper;
    private final GoldConfig gold;
    private final BooleanConfig diamondIgnoreMaxMana;
    private final MalignantConfig malignant;
    private Values override;

    private SpellgunServerConfig(
            TierConfig iron,
            TierConfig copper,
            GoldConfig gold,
            BooleanConfig diamondIgnoreMaxMana,
            MalignantConfig malignant
    ) {
        this.iron = iron;
        this.copper = copper;
        this.gold = gold;
        this.diamondIgnoreMaxMana = diamondIgnoreMaxMana;
        this.malignant = malignant;
    }

    public static SpellgunServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("Spellgun");
        var iron = defineTier(builder, "Iron", 20 * 5, 4);
        var copper = defineTier(builder, "Copper", 20 * 20, 20);
        var gold = defineGold(builder);
        builder.push("Diamond");
        var diamondIgnoreMaxMana = defineIgnoreMaxMana(builder);
        builder.pop();
        var malignant = defineMalignant(builder);
        builder.pop();

        return new SpellgunServerConfig(iron, copper, gold, diamondIgnoreMaxMana, malignant);
    }

    public int ironMaxInstantImbueCooldownTicks() {
        return values().ironMaxInstantImbueCooldownTicks();
    }

    public int ironOverriddenSpellCooldownTicks() {
        return values().ironOverriddenSpellCooldownTicks();
    }

    public int copperMaxInstantImbueCooldownTicks() {
        return values().copperMaxInstantImbueCooldownTicks();
    }

    public int copperOverriddenSpellCooldownTicks() {
        return values().copperOverriddenSpellCooldownTicks();
    }

    public int goldReducedCooldownMinimumTicks() {
        return values().goldReducedCooldownMinimumTicks();
    }

    public int goldCooldownReductionTicks() {
        return values().goldCooldownReductionTicks();
    }

    public boolean ironIgnoreMaxMana() {
        return values().ironIgnoreMaxMana();
    }

    public boolean copperIgnoreMaxMana() {
        return values().copperIgnoreMaxMana();
    }

    public boolean goldIgnoreMaxMana() {
        return values().goldIgnoreMaxMana();
    }

    public boolean diamondIgnoreMaxMana() {
        return values().diamondIgnoreMaxMana();
    }

    public double malignantForcedSpellPower() {
        return values().malignantForcedSpellPower();
    }

    public double malignantForcedSchoolSpellPower() {
        return values().malignantForcedSchoolSpellPower();
    }

    public double malignantForcedSummonDamage() {
        return values().malignantForcedSummonDamage();
    }

    public Values values() {
        if (override != null) {
            return override;
        }
        return new Values(
                iron.maxInstantImbueCooldownTicks(),
                iron.overriddenSpellCooldownTicks(),
                copper.maxInstantImbueCooldownTicks(),
                copper.overriddenSpellCooldownTicks(),
                gold.reducedCooldownMinimumTicks(),
                gold.cooldownReductionTicks(),
                iron.ignoreMaxManaEnabled(),
                copper.ignoreMaxManaEnabled(),
                gold.ignoreMaxManaEnabled(),
                diamondIgnoreMaxMana.value(),
                malignant.forcedSpellPower(),
                malignant.forcedSchoolSpellPower(),
                malignant.forcedSummonDamage()
        );
    }

    public void setForGameTest(Values values) {
        override = values;
    }

    private static TierConfig defineTier(
            ModConfigSpec.Builder builder,
            String tierName,
            int defaultMaxInstantImbueCooldownTicks,
            int defaultOverriddenSpellCooldownTicks
    ) {
        builder.push(tierName);
        var maxInstantImbueCooldownTicks = builder
                .comment("Maximum spell cooldown ticks allowed for this Spellgun's imbue restriction. 0 disables this limit.")
                .defineInRange("maxInstantImbueCooldownTicks", defaultMaxInstantImbueCooldownTicks, 0, MAX_CONFIGURED_TICKS);
        var overriddenSpellCooldownTicks = builder
                .comment("Base cooldown ticks applied after this Spellgun casts, before the player's cooldown reduction attribute. 0 forces a 0-tick cooldown instead of using the original spell cooldown.")
                .defineInRange("overriddenSpellCooldownTicks", defaultOverriddenSpellCooldownTicks, 0, MAX_CONFIGURED_TICKS);
        var ignoreMaxMana = defineIgnoreMaxMana(builder);
        builder.pop();

        return new TierConfig(
                maxInstantImbueCooldownTicks,
                defaultMaxInstantImbueCooldownTicks,
                overriddenSpellCooldownTicks,
                defaultOverriddenSpellCooldownTicks,
                ignoreMaxMana
        );
    }

    private static GoldConfig defineGold(ModConfigSpec.Builder builder) {
        builder.push("Gold");
        var reducedCooldownMinimumTicks = builder
                .comment("Minimum cooldown after Gold Spellgun reduction.")
                .defineInRange("reducedCooldownMinimumTicks", 10, 0, MAX_CONFIGURED_TICKS);
        var cooldownReductionTicks = builder
                .comment("Cooldown ticks subtracted after Gold Spellgun casts.")
                .defineInRange("cooldownReductionTicks", 200, 0, MAX_CONFIGURED_TICKS);
        var ignoreMaxMana = defineIgnoreMaxMana(builder);
        builder.pop();

        return new GoldConfig(reducedCooldownMinimumTicks, 10, cooldownReductionTicks, 200, ignoreMaxMana);
    }

    private static BooleanConfig defineIgnoreMaxMana(ModConfigSpec.Builder builder) {
        return new BooleanConfig(builder
                .comment("Whether this Spellgun may cast a spell whose mana cost exceeds the caster's maximum mana.")
                .define("ignoreMaxMana", false), false);
    }

    private static MalignantConfig defineMalignant(ModConfigSpec.Builder builder) {
        builder.push("Malignant");
        var forcedSpellPower = builder
                .comment("Generic spell power multiplier used while Malignant Spellgun activates a spell.")
                .defineInRange("forcedSpellPower", 1.0D, 0.0D, 100.0D);
        var forcedSchoolSpellPower = builder
                .comment("School spell power multiplier used while Malignant Spellgun activates a spell.")
                .defineInRange("forcedSchoolSpellPower", 1.0D, 0.0D, 100.0D);
        var forcedSummonDamage = builder
                .comment("Summon damage multiplier used only during Malignant Spellgun spell activation.")
                .defineInRange("forcedSummonDamage", 1.0D, 0.0D, 100.0D);
        builder.pop();
        return new MalignantConfig(forcedSpellPower, forcedSchoolSpellPower, forcedSummonDamage);
    }

    public record Values(
            int ironMaxInstantImbueCooldownTicks,
            int ironOverriddenSpellCooldownTicks,
            int copperMaxInstantImbueCooldownTicks,
            int copperOverriddenSpellCooldownTicks,
            int goldReducedCooldownMinimumTicks,
            int goldCooldownReductionTicks,
            boolean ironIgnoreMaxMana,
            boolean copperIgnoreMaxMana,
            boolean goldIgnoreMaxMana,
            boolean diamondIgnoreMaxMana,
            double malignantForcedSpellPower,
            double malignantForcedSchoolSpellPower,
            double malignantForcedSummonDamage
    ) {
        public Values(
                int ironMaxInstantImbueCooldownTicks,
                int ironOverriddenSpellCooldownTicks,
                int copperMaxInstantImbueCooldownTicks,
                int copperOverriddenSpellCooldownTicks,
                int goldReducedCooldownMinimumTicks,
                int goldCooldownReductionTicks
        ) {
            this(
                    ironMaxInstantImbueCooldownTicks,
                    ironOverriddenSpellCooldownTicks,
                    copperMaxInstantImbueCooldownTicks,
                    copperOverriddenSpellCooldownTicks,
                    goldReducedCooldownMinimumTicks,
                    goldCooldownReductionTicks,
                    false,
                    false,
                    false,
                    false,
                    1.0D,
                    1.0D,
                    1.0D
            );
        }

        public Values {
            ironMaxInstantImbueCooldownTicks = clampTicks(ironMaxInstantImbueCooldownTicks);
            ironOverriddenSpellCooldownTicks = clampTicks(ironOverriddenSpellCooldownTicks);
            copperMaxInstantImbueCooldownTicks = clampTicks(copperMaxInstantImbueCooldownTicks);
            copperOverriddenSpellCooldownTicks = clampTicks(copperOverriddenSpellCooldownTicks);
            goldReducedCooldownMinimumTicks = clampTicks(goldReducedCooldownMinimumTicks);
            goldCooldownReductionTicks = clampTicks(goldCooldownReductionTicks);
            malignantForcedSpellPower = clampMultiplier(malignantForcedSpellPower);
            malignantForcedSchoolSpellPower = clampMultiplier(malignantForcedSchoolSpellPower);
            malignantForcedSummonDamage = clampMultiplier(malignantForcedSummonDamage);
        }
    }

    private record TierConfig(
            ModConfigSpec.IntValue maxInstantImbueCooldownTicksValue,
            int defaultMaxInstantImbueCooldownTicks,
            ModConfigSpec.IntValue overriddenSpellCooldownTicksValue,
            int defaultOverriddenSpellCooldownTicks,
            BooleanConfig ignoreMaxMana
    ) {
        int maxInstantImbueCooldownTicks() {
            return configuredTicks(maxInstantImbueCooldownTicksValue, defaultMaxInstantImbueCooldownTicks);
        }

        int overriddenSpellCooldownTicks() {
            return configuredTicks(overriddenSpellCooldownTicksValue, defaultOverriddenSpellCooldownTicks);
        }

        boolean ignoreMaxManaEnabled() {
            return ignoreMaxMana.value();
        }
    }

    private static int configuredTicks(ModConfigSpec.IntValue value, int defaultValue) {
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            // Datapack 読み込み中の ItemStack 正規化では server config がまだロードされていない.
            return defaultValue;
        }
    }

    private record GoldConfig(
            ModConfigSpec.IntValue reducedCooldownMinimumTicksValue,
            int defaultReducedCooldownMinimumTicks,
            ModConfigSpec.IntValue cooldownReductionTicksValue,
            int defaultCooldownReductionTicks,
            BooleanConfig ignoreMaxMana
    ) {
        int reducedCooldownMinimumTicks() {
            return configuredTicks(reducedCooldownMinimumTicksValue, defaultReducedCooldownMinimumTicks);
        }

        int cooldownReductionTicks() {
            return configuredTicks(cooldownReductionTicksValue, defaultCooldownReductionTicks);
        }

        boolean ignoreMaxManaEnabled() {
            return ignoreMaxMana.value();
        }
    }

    private record BooleanConfig(ModConfigSpec.BooleanValue configuredValue, boolean defaultValue) {
        boolean value() {
            try {
                return configuredValue.get();
            } catch (IllegalStateException ignored) {
                return defaultValue;
            }
        }
    }

    private record MalignantConfig(
            ModConfigSpec.DoubleValue forcedSpellPowerValue,
            ModConfigSpec.DoubleValue forcedSchoolSpellPowerValue,
            ModConfigSpec.DoubleValue forcedSummonDamageValue
    ) {
        double forcedSpellPower() {
            return configuredMultiplier(forcedSpellPowerValue, 1.0D);
        }

        double forcedSchoolSpellPower() {
            return configuredMultiplier(forcedSchoolSpellPowerValue, 1.0D);
        }

        double forcedSummonDamage() {
            return configuredMultiplier(forcedSummonDamageValue, 1.0D);
        }
    }

    private static int clampTicks(int ticks) {
        return Math.max(0, Math.min(MAX_CONFIGURED_TICKS, ticks));
    }

    private static double configuredMultiplier(ModConfigSpec.DoubleValue value, double defaultValue) {
        try {
            return clampMultiplier(value.get());
        } catch (IllegalStateException ignored) {
            return defaultValue;
        }
    }

    private static double clampMultiplier(double value) {
        return Math.max(0.0D, Math.min(100.0D, value));
    }
}
