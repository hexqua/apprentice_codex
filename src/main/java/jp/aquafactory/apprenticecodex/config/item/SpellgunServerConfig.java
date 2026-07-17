package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SpellgunServerConfig {
    private static final int MAX_CONFIGURED_TICKS = 72000;

    private final TierConfig iron;
    private final TierConfig copper;
    private final GoldConfig gold;
    private Values override;

    private SpellgunServerConfig(TierConfig iron, TierConfig copper, GoldConfig gold) {
        this.iron = iron;
        this.copper = copper;
        this.gold = gold;
    }

    public static SpellgunServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("Spellgun");
        var iron = defineTier(builder, "Iron", 20 * 5, 4);
        var copper = defineTier(builder, "Copper", 20 * 20, 20);
        var gold = defineGold(builder);
        builder.pop();

        return new SpellgunServerConfig(iron, copper, gold);
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
                gold.cooldownReductionTicks()
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
        builder.pop();

        return new TierConfig(
                maxInstantImbueCooldownTicks,
                defaultMaxInstantImbueCooldownTicks,
                overriddenSpellCooldownTicks,
                defaultOverriddenSpellCooldownTicks
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
        builder.pop();

        return new GoldConfig(reducedCooldownMinimumTicks, 10, cooldownReductionTicks, 200);
    }

    public record Values(
            int ironMaxInstantImbueCooldownTicks,
            int ironOverriddenSpellCooldownTicks,
            int copperMaxInstantImbueCooldownTicks,
            int copperOverriddenSpellCooldownTicks,
            int goldReducedCooldownMinimumTicks,
            int goldCooldownReductionTicks
    ) {
        public Values {
            ironMaxInstantImbueCooldownTicks = clampTicks(ironMaxInstantImbueCooldownTicks);
            ironOverriddenSpellCooldownTicks = clampTicks(ironOverriddenSpellCooldownTicks);
            copperMaxInstantImbueCooldownTicks = clampTicks(copperMaxInstantImbueCooldownTicks);
            copperOverriddenSpellCooldownTicks = clampTicks(copperOverriddenSpellCooldownTicks);
            goldReducedCooldownMinimumTicks = clampTicks(goldReducedCooldownMinimumTicks);
            goldCooldownReductionTicks = clampTicks(goldCooldownReductionTicks);
        }
    }

    private record TierConfig(
            ModConfigSpec.IntValue maxInstantImbueCooldownTicksValue,
            int defaultMaxInstantImbueCooldownTicks,
            ModConfigSpec.IntValue overriddenSpellCooldownTicksValue,
            int defaultOverriddenSpellCooldownTicks
    ) {
        int maxInstantImbueCooldownTicks() {
            return configuredTicks(maxInstantImbueCooldownTicksValue, defaultMaxInstantImbueCooldownTicks);
        }

        int overriddenSpellCooldownTicks() {
            return configuredTicks(overriddenSpellCooldownTicksValue, defaultOverriddenSpellCooldownTicks);
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
            int defaultCooldownReductionTicks
    ) {
        int reducedCooldownMinimumTicks() {
            return configuredTicks(reducedCooldownMinimumTicksValue, defaultReducedCooldownMinimumTicks);
        }

        int cooldownReductionTicks() {
            return configuredTicks(cooldownReductionTicksValue, defaultCooldownReductionTicks);
        }
    }

    private static int clampTicks(int ticks) {
        return Math.max(0, Math.min(MAX_CONFIGURED_TICKS, ticks));
    }
}
