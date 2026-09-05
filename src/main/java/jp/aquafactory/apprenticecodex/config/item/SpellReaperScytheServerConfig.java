package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SpellReaperScytheServerConfig {
    private static final int MAX_COOLDOWN_TICKS = 200;

    private final ModConfigSpec.IntValue ascensionBaseManaCost;
    private final ModConfigSpec.IntValue ascensionManaCostReductionPerLevel;
    private final ModConfigSpec.IntValue ascensionCooldownTicks;
    private final ModConfigSpec.IntValue throwManaCost;
    private final ModConfigSpec.IntValue throwManaPerTick;
    private Values override;

    private SpellReaperScytheServerConfig(
            ModConfigSpec.IntValue ascensionBaseManaCost,
            ModConfigSpec.IntValue ascensionManaCostReductionPerLevel,
            ModConfigSpec.IntValue ascensionCooldownTicks,
            ModConfigSpec.IntValue throwManaCost,
            ModConfigSpec.IntValue throwManaPerTick
    ) {
        this.ascensionBaseManaCost = ascensionBaseManaCost;
        this.ascensionManaCostReductionPerLevel = ascensionManaCostReductionPerLevel;
        this.ascensionCooldownTicks = ascensionCooldownTicks;
        this.throwManaCost = throwManaCost;
        this.throwManaPerTick = throwManaPerTick;
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
        var throwManaCost = builder.comment("Mana consumed once when throwing Spell Reaper Scythe.")
                .defineInRange("throwManaCost", 100, 0, Integer.MAX_VALUE);
        var throwManaPerTick = builder.comment("Mana consumed each tick while the thrown scythe hovers. 20 ticks = 1 second.")
                .defineInRange("throwManaPerTick", 3, 0, Integer.MAX_VALUE);
        builder.pop();

        return new SpellReaperScytheServerConfig(
                ascensionBaseManaCost,
                ascensionManaCostReductionPerLevel,
                ascensionCooldownTicks, throwManaCost, throwManaPerTick
        );
    }

    public Values values() {
        if (override != null) {
            return override;
        }
        return new Values(
                ascensionBaseManaCost.get(),
                ascensionManaCostReductionPerLevel.get(),
                ascensionCooldownTicks.get(), throwManaCost.get(), throwManaPerTick.get()
        );
    }

    public void setForGameTest(Values values) {
        override = values;
    }

    public record Values(int ascensionBaseManaCost, int ascensionManaCostReductionPerLevel, int ascensionCooldownTicks,
                         int throwManaCost, int throwManaPerTick) {
        public Values(int base, int reduction, int cooldown) {
            this(base, reduction, cooldown, 100, 3);
        }

        public Values {
            throwManaCost = Math.max(0, throwManaCost);
            throwManaPerTick = Math.max(0, throwManaPerTick);
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
