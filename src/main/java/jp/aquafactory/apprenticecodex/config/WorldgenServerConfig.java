package jp.aquafactory.apprenticecodex.config;

import net.minecraftforge.common.ForgeConfigSpec;

final class WorldgenServerConfig {
    private final ForgeConfigSpec.BooleanValue enableErrandMageVillageHouseInjection;

    private WorldgenServerConfig(ForgeConfigSpec.BooleanValue enableErrandMageVillageHouseInjection) {
        this.enableErrandMageVillageHouseInjection = enableErrandMageVillageHouseInjection;
    }

    static WorldgenServerConfig define(ForgeConfigSpec.Builder builder) {
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
