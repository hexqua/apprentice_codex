package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SpellThrowableCardServerConfig {
    private static final int MIN_CRAFT_COUNT = 1;
    private static final int MAX_CRAFT_COUNT = 64;

    private final ForgeConfigSpec.IntValue spellInvokeCardCraftCount;
    private final ForgeConfigSpec.IntValue spellAutonomyCardCraftCount;

    private Values override;

    private SpellThrowableCardServerConfig(
            ForgeConfigSpec.IntValue spellInvokeCardCraftCount,
            ForgeConfigSpec.IntValue spellAutonomyCardCraftCount
    ) {
        this.spellInvokeCardCraftCount = spellInvokeCardCraftCount;
        this.spellAutonomyCardCraftCount = spellAutonomyCardCraftCount;
    }

    public static SpellThrowableCardServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("SpellThrowableCards");
        var spellInvokeCardCraftCount = builder
                .comment("Craft output count for Spell Invoke Card. The same count is required for paper/card base ingredients.")
                .defineInRange("spellInvokeCardCraftCount", 16, MIN_CRAFT_COUNT, MAX_CRAFT_COUNT);
        var spellAutonomyCardCraftCount = builder
                .comment("Craft output count for Spell Autonomy Card. The same count is required for paper/card base ingredients.")
                .defineInRange("spellAutonomyCardCraftCount", 8, MIN_CRAFT_COUNT, MAX_CRAFT_COUNT);
        builder.pop();

        return new SpellThrowableCardServerConfig(spellInvokeCardCraftCount, spellAutonomyCardCraftCount);
    }

    public int spellInvokeCardCraftCount() {
        return values().spellInvokeCardCraftCount();
    }

    public int spellAutonomyCardCraftCount() {
        return values().spellAutonomyCardCraftCount();
    }

    public Values values() {
        if (override != null) {
            return override;
        }
        return new Values(spellInvokeCardCraftCount.get(), spellAutonomyCardCraftCount.get());
    }

    public void setForGameTest(Values values) {
        this.override = values;
    }

    public record Values(int spellInvokeCardCraftCount, int spellAutonomyCardCraftCount) {
        public Values {
            spellInvokeCardCraftCount = clampCraftCount(spellInvokeCardCraftCount);
            spellAutonomyCardCraftCount = clampCraftCount(spellAutonomyCardCraftCount);
        }
    }

    private static int clampCraftCount(int value) {
        return Math.max(MIN_CRAFT_COUNT, Math.min(MAX_CRAFT_COUNT, value));
    }
}
