package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class IsekaiTravelGuidebookServerConfig {
    private final ForgeConfigSpec.BooleanValue showTooltip;
    private final ForgeConfigSpec.BooleanValue enableBonusChestLoot;

    private IsekaiTravelGuidebookServerConfig(
            ForgeConfigSpec.BooleanValue showTooltip,
            ForgeConfigSpec.BooleanValue enableBonusChestLoot
    ) {
        this.showTooltip = showTooltip;
        this.enableBonusChestLoot = enableBonusChestLoot;
    }

    public static IsekaiTravelGuidebookServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("IsekaiTravelGuidebook");
        var showTooltip = builder
                .comment("Show a tooltip on the Isekai Travel Guidebook. Disable this if it feels too intrusive.")
                .define("showTooltip", true);
        var enableBonusChestLoot = builder
                .comment("Add the Isekai Travel Guidebook to bonus chest loot.")
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
