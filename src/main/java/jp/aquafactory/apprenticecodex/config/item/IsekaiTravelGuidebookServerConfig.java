package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class IsekaiTravelGuidebookServerConfig {
    private final ForgeConfigSpec.BooleanValue showTooltip;

    private IsekaiTravelGuidebookServerConfig(ForgeConfigSpec.BooleanValue showTooltip) {
        this.showTooltip = showTooltip;
    }

    public static IsekaiTravelGuidebookServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("IsekaiTravelGuidebook");
        var showTooltip = builder
                .comment("Show a tooltip on the Isekai Travel Guidebook. Disable this if it feels too intrusive.")
                .define("showTooltip", true);
        builder.pop();

        return new IsekaiTravelGuidebookServerConfig(showTooltip);
    }

    public boolean showTooltip() {
        return showTooltip.get();
    }
}
