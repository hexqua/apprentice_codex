package jp.aquafactory.apprenticecodex.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ApprenticeCodexCommonConfig {
    public static final ModConfigSpec SPEC;

    // レシピ制御はSERVERだと評価タイミングが遅く、COMMON出ないとダメ.
    private static final ModConfigSpec.BooleanValue DISABLE_APPRENTICE_DESK_RECIPE;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("Items");
        DISABLE_APPRENTICE_DESK_RECIPE = builder.define("disableApprenticeDeskRecipe", false);
        builder.pop();
        SPEC = builder.build();
    }

    private ApprenticeCodexCommonConfig() {
    }

    public static boolean disableApprenticeDeskRecipe() {
        return DISABLE_APPRENTICE_DESK_RECIPE.get();
    }
}

