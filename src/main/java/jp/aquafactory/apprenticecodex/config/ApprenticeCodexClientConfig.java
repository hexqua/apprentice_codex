package jp.aquafactory.apprenticecodex.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ApprenticeCodexClientConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLE_APPRENTICE_MAGE_ROBE_CAPE_ANIMATION;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("Items");
        ENABLE_APPRENTICE_MAGE_ROBE_CAPE_ANIMATION = builder
                .comment("Enable cape animation for the Apprentice Mage Robe.")
                .define("enableApprenticeMageRobeCapeAnimation", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ApprenticeCodexClientConfig() {
    }

    public static boolean enableApprenticeMageRobeCapeAnimation() {
        return ENABLE_APPRENTICE_MAGE_ROBE_CAPE_ANIMATION.get();
    }
}
