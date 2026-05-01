package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.recipe.condition.ArcanumInAJarRecipeEnabledCondition;
import jp.aquafactory.apprenticecodex.recipe.condition.ExplorersCodexRecipeEnabledCondition;
import net.minecraftforge.common.crafting.CraftingHelper;

public final class RecipeConditionRegistry {
    private static boolean initialized;

    private RecipeConditionRegistry() {
    }

    public static void register() {
        if (initialized) {
            return;
        }

        CraftingHelper.register(ArcanumInAJarRecipeEnabledCondition.Serializer.INSTANCE);
        CraftingHelper.register(ExplorersCodexRecipeEnabledCondition.Serializer.INSTANCE);
        initialized = true;
    }
}
