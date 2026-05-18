package jp.aquafactory.apprenticecodex.config;

import net.neoforged.neoforge.common.ModConfigSpec;

final class LootServerConfig {
    private final ModConfigSpec.BooleanValue enableApprenticeCurioLoot;
    private final ModConfigSpec.DoubleValue apprenticeCurioLootChanceMultiplier;

    private LootServerConfig(
            ModConfigSpec.BooleanValue enableApprenticeCurioLoot,
            ModConfigSpec.DoubleValue apprenticeCurioLootChanceMultiplier
    ) {
        this.enableApprenticeCurioLoot = enableApprenticeCurioLoot;
        this.apprenticeCurioLootChanceMultiplier = apprenticeCurioLootChanceMultiplier;
    }

    static LootServerConfig define(ModConfigSpec.Builder builder) {
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
