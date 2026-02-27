package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.recipe.condition.ApprenticeDeskRecipeEnabledCondition;
import net.minecraftforge.common.crafting.CraftingHelper;

public final class RecipeConditionRegistry {
    private static boolean initialized;

    private RecipeConditionRegistry() {
    }

    public static void register() {
        if (initialized) {
            return;
        }

        CraftingHelper.register(ApprenticeDeskRecipeEnabledCondition.Serializer.INSTANCE);
        initialized = true;
    }
}
