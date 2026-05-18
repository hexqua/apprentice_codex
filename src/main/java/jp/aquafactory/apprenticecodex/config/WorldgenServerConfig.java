package jp.aquafactory.apprenticecodex.config;

import net.neoforged.neoforge.common.ModConfigSpec;

final class WorldgenServerConfig {
    private final ModConfigSpec.BooleanValue enableErrandMageVillageHouseInjection;

    private WorldgenServerConfig(ModConfigSpec.BooleanValue enableErrandMageVillageHouseInjection) {
        this.enableErrandMageVillageHouseInjection = enableErrandMageVillageHouseInjection;
    }

    static WorldgenServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("Worldgen");
        builder.push("ErrandMageVillageHouse");
        var enableErrandMageVillageHouseInjection = builder
                .comment("Inject Errand Mage houses into village template pools. Disable this before world generation if another datapack or modpack should fully control these houses.")
                .define("enableInjection", true);
        builder.pop();
        builder.pop();

        return new WorldgenServerConfig(enableErrandMageVillageHouseInjection);
    }

    boolean enableErrandMageVillageHouseInjection() {
        return enableErrandMageVillageHouseInjection.get();
    }
}
