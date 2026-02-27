package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CraftsmansDelightServerConfig {
    private final ForgeConfigSpec.BooleanValue canImbueEnchantment;
    private final ForgeConfigSpec.DoubleValue requiredMana;
    private final ForgeConfigSpec.IntValue fortuneLevel;

    private CraftsmansDelightServerConfig(
            ForgeConfigSpec.BooleanValue canImbueEnchantment,
            ForgeConfigSpec.DoubleValue requiredMana,
            ForgeConfigSpec.IntValue fortuneLevel
    ) {
        this.canImbueEnchantment = canImbueEnchantment;
        this.requiredMana = requiredMana;
        this.fortuneLevel = fortuneLevel;
    }

    public static CraftsmansDelightServerConfig define(ForgeConfigSpec.Builder builder) {
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
