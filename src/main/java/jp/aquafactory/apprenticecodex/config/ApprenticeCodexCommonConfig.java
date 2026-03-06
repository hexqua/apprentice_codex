package jp.aquafactory.apprenticecodex.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ApprenticeCodexCommonConfig {
    public static final ForgeConfigSpec SPEC;

    // レシピ制御はSERVERだと評価タイミングが遅く、COMMON出ないとダメ.
    private static final ForgeConfigSpec.BooleanValue DISABLE_APPRENTICE_DESK_RECIPE;
    private static final ForgeConfigSpec.BooleanValue DISABLE_ARCANUM_IN_A_JAR_RECIPE;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("Items");
        DISABLE_APPRENTICE_DESK_RECIPE = builder.define("disableApprenticeDeskRecipe", false);
        DISABLE_ARCANUM_IN_A_JAR_RECIPE = builder.define("disableArcanumInAJarRecipe", false);
        builder.pop();
        SPEC = builder.build();
    }

    private ApprenticeCodexCommonConfig() {
    }

    public static boolean disableApprenticeDeskRecipe() {
        return DISABLE_APPRENTICE_DESK_RECIPE.get();
    }

    public static boolean disableArcanumInAJarRecipe() {
        return DISABLE_ARCANUM_IN_A_JAR_RECIPE.get();
    }
}
