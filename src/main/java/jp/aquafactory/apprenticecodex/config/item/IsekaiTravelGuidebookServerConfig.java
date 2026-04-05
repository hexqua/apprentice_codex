package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class IsekaiTravelGuidebookServerConfig {
    private final ModConfigSpec.BooleanValue showTooltip;
    private final ModConfigSpec.BooleanValue enableBonusChestLoot;

    private IsekaiTravelGuidebookServerConfig(
            ModConfigSpec.BooleanValue showTooltip,
            ModConfigSpec.BooleanValue enableBonusChestLoot
    ) {
        this.showTooltip = showTooltip;
        this.enableBonusChestLoot = enableBonusChestLoot;
    }

    public static IsekaiTravelGuidebookServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("IsekaiTravelGuidebook");
        var showTooltip = builder
                .comment("Add tooltip to Isekai Travel Guidebook.(can disable because of immersive)")
                .define("showTooltip", true);
        var enableBonusChestLoot = builder
                .comment("Add Isekai Travel Guidebook to bonus chest loot.")
                .define("enableBonusChestLoot", true);
        builder.pop();

        return new IsekaiTravelGuidebookServerConfig(showTooltip, enableBonusChestLoot);
    }

    public boolean showTooltip() {
        return showTooltip.get();
    }

    public boolean enableBonusChestLoot() {
        return enableBonusChestLoot.get();
    }
}
