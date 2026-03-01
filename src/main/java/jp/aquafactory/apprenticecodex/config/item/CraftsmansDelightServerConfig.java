package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CraftsmansDelightServerConfig {
    private final ModConfigSpec.BooleanValue canImbueEnchantment;
    private final ModConfigSpec.DoubleValue requiredMana;
    private final ModConfigSpec.IntValue fortuneLevel;

    private CraftsmansDelightServerConfig(
            ModConfigSpec.BooleanValue canImbueEnchantment,
            ModConfigSpec.DoubleValue requiredMana,
            ModConfigSpec.IntValue fortuneLevel
    ) {
        this.canImbueEnchantment = canImbueEnchantment;
        this.requiredMana = requiredMana;
        this.fortuneLevel = fortuneLevel;
    }

    public static CraftsmansDelightServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("CraftsmansDelight");

        var canImbueEnchantment = builder.define("canImbueEnchantment", true);
        var requiredMana = builder.defineInRange("requiredMana", 500.0d, 0.0d, 10000.0d);
        var fortuneLevel = builder.defineInRange("fortuneLevel", 3, 1, 10);

        builder.pop();
        return new CraftsmansDelightServerConfig(
                canImbueEnchantment,
                requiredMana,
                fortuneLevel
        );
    }

    public boolean canImbueEnchantment() {
        return canImbueEnchantment.get();
    }

    public float requiredMana() {
        return requiredMana.get().floatValue();
    }

    public int fortuneLevel() {
        return fortuneLevel.get();
    }
}

