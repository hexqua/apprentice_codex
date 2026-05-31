package jp.aquafactory.apprenticecodex.config.spell;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BoundBowServerConfig {
    private final ForgeConfigSpec.IntValue maxPowerEnchantmentLevel;
    private final ForgeConfigSpec.DoubleValue forgeArrowManaCost;
    private Integer maxPowerEnchantmentLevelOverride;
    private Float forgeArrowManaCostOverride;

    private BoundBowServerConfig(
            ForgeConfigSpec.IntValue maxPowerEnchantmentLevel,
            ForgeConfigSpec.DoubleValue forgeArrowManaCost
    ) {
        this.maxPowerEnchantmentLevel = maxPowerEnchantmentLevel;
        this.forgeArrowManaCost = forgeArrowManaCost;
    }

    public static BoundBowServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("BoundBow");

        var maxPowerEnchantmentLevel = builder.defineInRange("maxPowerEnchantmentLevel", 6, 0, 255);
        var forgeArrowManaCost = builder.defineInRange("forgeArrowManaCost", 25.0D, 0.0D, 10000.0D);

        builder.pop();
        return new BoundBowServerConfig(maxPowerEnchantmentLevel, forgeArrowManaCost);
    }

    public int maxPowerEnchantmentLevel() {
        if (maxPowerEnchantmentLevelOverride != null) {
            return maxPowerEnchantmentLevelOverride;
        }
        return maxPowerEnchantmentLevel.get();
    }

    public float forgeArrowManaCost() {
        if (forgeArrowManaCostOverride != null) {
            return forgeArrowManaCostOverride;
        }
        return forgeArrowManaCost.get().floatValue();
    }

    public void setForGameTest(int maxPowerEnchantmentLevel, float forgeArrowManaCost) {
        this.maxPowerEnchantmentLevelOverride = maxPowerEnchantmentLevel;
        this.forgeArrowManaCostOverride = forgeArrowManaCost;
    }
}
