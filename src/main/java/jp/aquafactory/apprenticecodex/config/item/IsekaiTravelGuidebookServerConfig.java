package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class IsekaiTravelGuidebookServerConfig {
    private final ModConfigSpec.BooleanValue showTooltip;

    private IsekaiTravelGuidebookServerConfig(ModConfigSpec.BooleanValue showTooltip) {
        this.showTooltip = showTooltip;
    }

    public static IsekaiTravelGuidebookServerConfig define(ModConfigSpec.Builder builder) {
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
