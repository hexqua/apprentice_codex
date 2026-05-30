package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SpellgunServerConfig {
    private static final int MAX_CONFIGURED_TICKS = 72000;

    private final TierConfig iron;
    private final TierConfig copper;
    private final TierConfig gold;
    private final TierConfig diamond;
    private Values override;

    private SpellgunServerConfig(TierConfig iron, TierConfig copper, TierConfig gold, TierConfig diamond) {
        this.iron = iron;
        this.copper = copper;
        this.gold = gold;
        this.diamond = diamond;
    }

    public static SpellgunServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("Spellgun");
        var iron = defineTier(builder, "Iron", 20 * 5, 10);
        var copper = defineTier(builder, "Copper", 20 * 10, 20);
        var gold = defineTier(builder, "Gold", 20 * 20, 40);
        var diamond = defineTier(builder, "Diamond", 20 * 30, 80);
        builder.pop();

        return new SpellgunServerConfig(iron, copper, gold, diamond);
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

    public int goldMaxInstantImbueCooldownTicks() {
        return values().goldMaxInstantImbueCooldownTicks();
    }

    public int goldOverriddenSpellCooldownTicks() {
        return values().goldOverriddenSpellCooldownTicks();
    }

    public int diamondMaxInstantImbueCooldownTicks() {
        return values().diamondMaxInstantImbueCooldownTicks();
    }

    public int diamondOverriddenSpellCooldownTicks() {
        return values().diamondOverriddenSpellCooldownTicks();
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
                gold.maxInstantImbueCooldownTicks(),
                gold.overriddenSpellCooldownTicks(),
                diamond.maxInstantImbueCooldownTicks(),
                diamond.overriddenSpellCooldownTicks()
        );
    }

    public void setForGameTest(Values values) {
        override = values;
    }

    private static TierConfig defineTier(
            ForgeConfigSpec.Builder builder,
            String tierName,
            int defaultMaxInstantImbueCooldownTicks,
            int defaultOverriddenSpellCooldownTicks
    ) {
        builder.push(tierName);
        var maxInstantImbueCooldownTicks = builder
                .comment("Maximum spell cooldown ticks allowed for this Spellgun's imbue restriction. 0 disables this limit.")
                .defineInRange("maxInstantImbueCooldownTicks", defaultMaxInstantImbueCooldownTicks, 0, MAX_CONFIGURED_TICKS);
        var overriddenSpellCooldownTicks = builder
                .comment("Cooldown ticks applied after this Spellgun casts. 0 forces a 0-tick cooldown instead of using the original spell cooldown.")
                .defineInRange("overriddenSpellCooldownTicks", defaultOverriddenSpellCooldownTicks, 0, MAX_CONFIGURED_TICKS);
        builder.pop();

        return new TierConfig(maxInstantImbueCooldownTicks, overriddenSpellCooldownTicks);
    }

    public record Values(
            int ironMaxInstantImbueCooldownTicks,
            int ironOverriddenSpellCooldownTicks,
            int copperMaxInstantImbueCooldownTicks,
            int copperOverriddenSpellCooldownTicks,
            int goldMaxInstantImbueCooldownTicks,
            int goldOverriddenSpellCooldownTicks,
            int diamondMaxInstantImbueCooldownTicks,
            int diamondOverriddenSpellCooldownTicks
    ) {
        public Values {
            ironMaxInstantImbueCooldownTicks = clampTicks(ironMaxInstantImbueCooldownTicks);
            ironOverriddenSpellCooldownTicks = clampTicks(ironOverriddenSpellCooldownTicks);
            copperMaxInstantImbueCooldownTicks = clampTicks(copperMaxInstantImbueCooldownTicks);
            copperOverriddenSpellCooldownTicks = clampTicks(copperOverriddenSpellCooldownTicks);
            goldMaxInstantImbueCooldownTicks = clampTicks(goldMaxInstantImbueCooldownTicks);
            goldOverriddenSpellCooldownTicks = clampTicks(goldOverriddenSpellCooldownTicks);
            diamondMaxInstantImbueCooldownTicks = clampTicks(diamondMaxInstantImbueCooldownTicks);
            diamondOverriddenSpellCooldownTicks = clampTicks(diamondOverriddenSpellCooldownTicks);
        }
    }

    private record TierConfig(
            ForgeConfigSpec.IntValue maxInstantImbueCooldownTicksValue,
            ForgeConfigSpec.IntValue overriddenSpellCooldownTicksValue
    ) {
        int maxInstantImbueCooldownTicks() {
            return maxInstantImbueCooldownTicksValue.get();
        }

        int overriddenSpellCooldownTicks() {
            return overriddenSpellCooldownTicksValue.get();
        }
    }

    private static int clampTicks(int ticks) {
        return Math.max(0, Math.min(MAX_CONFIGURED_TICKS, ticks));
    }
}
