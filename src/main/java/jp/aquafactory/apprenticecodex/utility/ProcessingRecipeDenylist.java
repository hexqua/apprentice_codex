package jp.aquafactory.apprenticecodex.utility;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class ProcessingRecipeDenylist {
    private ProcessingRecipeDenylist() {
    }

    public static boolean isAllowed(RecipeHolder<?> recipe) {
        return isAllowed(recipe.id(), recipe.value());
    }

    private static boolean isAllowed(ResourceLocation recipeId, Recipe<?> recipe) {
        var recipeType = recipe.getType();
        if (recipeType == jp.aquafactory.apprenticecodex.registry.RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get()) {
            return !ApprenticeCodexServerConfig.isSpellcasterWorkbenchRecipeDenied(recipeId);
        }
        if (recipeType == jp.aquafactory.apprenticecodex.registry.RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get()) {
            return !ApprenticeCodexServerConfig.isEssenceSmokerRecipeDenied(recipeId);
        }
        if (recipeType == jp.aquafactory.apprenticecodex.registry.RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get()) {
            return !ApprenticeCodexServerConfig.isGrindRunnerRecipeDenied(recipeId);
        }
        return true;
    }

    public static Optional<RecipeHolder<? extends AbstractCookingRecipe>> findThermalProcessRecipe(
            RecipeManager recipeManager,
            SingleRecipeInput input,
            Level level
    ) {
        var blasting = findAllowedCookingRecipe(recipeManager, RecipeType.BLASTING, input, level);
        if (blasting.isPresent()) {
            return blasting;
        }

        var smelting = findAllowedCookingRecipe(recipeManager, RecipeType.SMELTING, input, level);
        if (smelting.isPresent()) {
            return smelting;
        }

        return findAllowedCookingRecipe(recipeManager, RecipeType.SMOKING, input, level);
    }

    private static <T extends AbstractCookingRecipe> Optional<RecipeHolder<T>> findAllowedCookingRecipe(
            RecipeManager recipeManager,
            RecipeType<T> recipeType,
            SingleRecipeInput input,
            Level level
    ) {
        return recipeManager.getAllRecipesFor(recipeType).stream()
                .filter(recipe -> !ApprenticeCodexServerConfig.isThermalProcessRecipeDenied(recipe.id()))
                .filter(recipe -> recipe.value().matches(input, level))
                .findFirst();
    }
}
