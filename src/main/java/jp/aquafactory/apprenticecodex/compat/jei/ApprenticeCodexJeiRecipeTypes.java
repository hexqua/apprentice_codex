package jp.aquafactory.apprenticecodex.compat.jei;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.recipe.essencesmoker.EssenceSmokerRecipe;
import jp.aquafactory.apprenticecodex.recipe.grindrunner.GrindRunnerRecipe;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipe;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.resources.ResourceLocation;

public final class ApprenticeCodexJeiRecipeTypes {
    private ApprenticeCodexJeiRecipeTypes() {
    }

    public static final RecipeType<GrindRunnerRecipe> GRIND_RUNNER =
            new RecipeType<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "grind_runner"), GrindRunnerRecipe.class);
    public static final RecipeType<EssenceSmokerRecipe> ESSENCE_SMOKER =
            new RecipeType<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "essence_smoker"), EssenceSmokerRecipe.class);
    public static final RecipeType<SpellcasterWorkbenchRecipe> SPELLCASTER_WORKBENCH =
            new RecipeType<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_workbench"), SpellcasterWorkbenchRecipe.class);
    public static final RecipeType<AlchemyBrewerJeiRecipe> ALCHEMY_BREWER =
            new RecipeType<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemy_brewer"), AlchemyBrewerJeiRecipe.class);
}
