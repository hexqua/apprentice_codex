package jp.aquafactory.apprenticecodex.utility;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.recipe.essencesmoker.EssenceSmokerRecipe;
import jp.aquafactory.apprenticecodex.recipe.grindrunner.GrindRunnerRecipe;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class ProcessingRecipeDenylist {
    private ProcessingRecipeDenylist() {
    }

    public static boolean isAllowed(SpellcasterWorkbenchRecipe recipe) {
        return !ApprenticeCodexServerConfig.isSpellcasterWorkbenchRecipeDenied(recipe.getId());
    }

    public static boolean isAllowed(EssenceSmokerRecipe recipe) {
        return !ApprenticeCodexServerConfig.isEssenceSmokerRecipeDenied(recipe.getId());
    }

    public static boolean isAllowed(GrindRunnerRecipe recipe) {
        return !ApprenticeCodexServerConfig.isGrindRunnerRecipeDenied(recipe.getId());
    }

    public static Optional<? extends AbstractCookingRecipe> findThermalProcessRecipe(
            RecipeManager recipeManager,
            Container input,
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

    private static <T extends AbstractCookingRecipe> Optional<T> findAllowedCookingRecipe(
            RecipeManager recipeManager,
            RecipeType<T> recipeType,
            Container input,
            Level level
    ) {
        return recipeManager.getAllRecipesFor(recipeType).stream()
                .filter(recipe -> !ApprenticeCodexServerConfig.isThermalProcessRecipeDenied(recipe.getId()))
                .filter(recipe -> recipe.matches(input, level))
                .findFirst();
    }
}
