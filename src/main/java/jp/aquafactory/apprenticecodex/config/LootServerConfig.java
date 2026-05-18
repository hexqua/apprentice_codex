package jp.aquafactory.apprenticecodex.config;

import net.minecraftforge.common.ForgeConfigSpec;

final class LootServerConfig {
    private final ForgeConfigSpec.BooleanValue enableApprenticeCurioLoot;
    private final ForgeConfigSpec.DoubleValue apprenticeCurioLootChanceMultiplier;

    private LootServerConfig(
            ForgeConfigSpec.BooleanValue enableApprenticeCurioLoot,
            ForgeConfigSpec.DoubleValue apprenticeCurioLootChanceMultiplier
    ) {
        this.enableApprenticeCurioLoot = enableApprenticeCurioLoot;
        this.apprenticeCurioLootChanceMultiplier = apprenticeCurioLootChanceMultiplier;
    }

    static LootServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("Loot");

        var enableApprenticeCurioLoot = builder
                .comment("Enable Apprentice Curio additions to configured chest loot.")
                .define("enableApprenticeCurioLoot", true);
        var apprenticeCurioLootChanceMultiplier = builder
                .comment("Multiplier for all Apprentice Curio chest loot additions. 1.0 = default chance. 0.0 effectively disables this loot without editing datapacks.")
                .defineInRange("apprenticeCurioLootChanceMultiplier", 1.0d, 0.0d, 10.0d);

        builder.pop();
        return new LootServerConfig(enableApprenticeCurioLoot, apprenticeCurioLootChanceMultiplier);
    }

    boolean enableApprenticeCurioLoot() {
        return enableApprenticeCurioLoot.get();
    }

    double apprenticeCurioLootChanceMultiplier() {
        return apprenticeCurioLootChanceMultiplier.get();
    }
}
