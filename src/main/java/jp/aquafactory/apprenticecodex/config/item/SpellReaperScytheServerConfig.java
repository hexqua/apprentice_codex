package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SpellReaperScytheServerConfig {
    private static final int MAX_COOLDOWN_TICKS = 200;

    private final ModConfigSpec.IntValue ascensionBaseManaCost;
    private final ModConfigSpec.IntValue ascensionManaCostReductionPerLevel;
    private final ModConfigSpec.IntValue ascensionCooldownTicks;
    private Values override;

    private SpellReaperScytheServerConfig(
            ModConfigSpec.IntValue ascensionBaseManaCost,
            ModConfigSpec.IntValue ascensionManaCostReductionPerLevel,
            ModConfigSpec.IntValue ascensionCooldownTicks
    ) {
        this.ascensionBaseManaCost = ascensionBaseManaCost;
        this.ascensionManaCostReductionPerLevel = ascensionManaCostReductionPerLevel;
        this.ascensionCooldownTicks = ascensionCooldownTicks;
    }

    public static SpellReaperScytheServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("SpellReaperScythe");
        var ascensionBaseManaCost = builder
                .comment("Base mana consumed when Spell Reaper Scythe activates Malum Ascension.")
                .defineInRange("ascensionBaseManaCost", 200, 0, Integer.MAX_VALUE);
        var ascensionManaCostReductionPerLevel = builder
                .comment("Mana cost reduction for each Malum Ascension level above level 1.")
                .defineInRange("ascensionManaCostReductionPerLevel", 40, 0, Integer.MAX_VALUE);
        var ascensionCooldownTicks = builder
                .comment("Fixed cooldown after Spell Reaper Scythe activates Malum Ascension. 20 ticks = 1 second.")
                .defineInRange("ascensionCooldownTicks", 10, 0, MAX_COOLDOWN_TICKS);
        builder.pop();

        return new SpellReaperScytheServerConfig(
                ascensionBaseManaCost,
                ascensionManaCostReductionPerLevel,
                ascensionCooldownTicks
        );
    }

    public Values values() {
        if (override != null) {
            return override;
        }
        return new Values(
                ascensionBaseManaCost.get(),
                ascensionManaCostReductionPerLevel.get(),
                ascensionCooldownTicks.get()
        );
    }

    public void setForGameTest(Values values) {
        override = values;
    }

    public record Values(int ascensionBaseManaCost, int ascensionManaCostReductionPerLevel, int ascensionCooldownTicks) {
        public Values {
            ascensionBaseManaCost = Math.max(0, ascensionBaseManaCost);
            ascensionManaCostReductionPerLevel = Math.max(0, ascensionManaCostReductionPerLevel);
            ascensionCooldownTicks = Math.max(0, Math.min(MAX_COOLDOWN_TICKS, ascensionCooldownTicks));
        }

        public int ascensionManaCost(int enchantmentLevel) {
            var reductionLevels = Math.max(0L, (long) enchantmentLevel - 1L);
            var reducedCost = (long) ascensionBaseManaCost
                    - (long) ascensionManaCostReductionPerLevel * reductionLevels;
            return (int) Math.max(0L, reducedCost);
        }
    }
}
