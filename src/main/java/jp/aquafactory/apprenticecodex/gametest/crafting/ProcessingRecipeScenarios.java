package jp.aquafactory.apprenticecodex.gametest.crafting;

import java.util.List;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.utility.ProcessingRecipeDenylist;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import static jp.aquafactory.apprenticecodex.gametest.integration.create.CreateProcessingTestSupport.CREATE_MOD_ID;

final class ProcessingRecipeScenarios {
    private ProcessingRecipeScenarios() {}

    static void processingRecipeDenylistsRejectConfiguredRecipeIds(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipeManager = helper.getLevel().getRecipeManager();
            var spellcasterWorkbenchRecipeId = ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID, "spellcaster_workbench/basic_spellcaster_round");
            var essenceSmokerRecipeId = ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID, "essence_smoker/infuse_coal_to_arcane_cinder");
            var grindRunnerRecipeId = ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID, "grind_runner/bone_meal_from_bone");
            var heavenlyFistCreateRecipeId = ResourceLocation.fromNamespaceAndPath(
                    CREATE_MOD_ID, "pressing/sugar_cane");
            var deniedBlastingRecipeId = ResourceLocation.fromNamespaceAndPath(
                    "minecraft", "iron_ingot_from_blasting_iron_ore");
            var fallbackSmeltingRecipeId = ResourceLocation.fromNamespaceAndPath(
                    "minecraft", "iron_ingot_from_smelting_iron_ore");

            try (var ignored = ApprenticeCodexServerConfig.useProcessingRecipeDenylistOverrideForGameTest(
                    List.of(spellcasterWorkbenchRecipeId.toString()),
                    List.of(essenceSmokerRecipeId.toString()),
                    List.of(grindRunnerRecipeId.toString()),
                    List.of(heavenlyFistCreateRecipeId.toString()),
                    List.of(deniedBlastingRecipeId.toString())
            )) {
                var spellcasterWorkbenchRecipe = recipeManager
                        .getAllRecipesFor(RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get()).stream()
                        .filter(recipe -> recipe.id().equals(spellcasterWorkbenchRecipeId))
                        .findFirst()
                        .orElse(null);
                helper.assertTrue(spellcasterWorkbenchRecipe != null, "Missing Spellcaster Workbench test recipe");
                helper.assertFalse(ProcessingRecipeDenylist.isAllowed(spellcasterWorkbenchRecipe),
                        "Spellcaster Workbench denylist did not reject configured recipe");

                var essenceSmokerRecipe = recipeManager.getAllRecipesFor(RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get()).stream()
                        .filter(recipe -> recipe.id().equals(essenceSmokerRecipeId))
                        .findFirst()
                        .orElse(null);
                helper.assertTrue(essenceSmokerRecipe != null, "Missing Essence Smoker test recipe");
                helper.assertFalse(ProcessingRecipeDenylist.isAllowed(essenceSmokerRecipe),
                        "Essence Smoker denylist did not reject configured recipe");

                var grindRunnerRecipe = recipeManager.getAllRecipesFor(RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get()).stream()
                        .filter(recipe -> recipe.id().equals(grindRunnerRecipeId))
                        .findFirst()
                        .orElse(null);
                helper.assertTrue(grindRunnerRecipe != null, "Missing Grind Runner test recipe");
                helper.assertFalse(ProcessingRecipeDenylist.isAllowed(grindRunnerRecipe),
                        "Grind Runner denylist did not reject configured recipe");

                helper.assertTrue(ApprenticeCodexServerConfig.isHeavenlyFistCreateRecipeDenied(heavenlyFistCreateRecipeId),
                        "Heavenly Fist Create denylist did not reject configured recipe");

                var ironOreInput = new SingleRecipeInput(new ItemStack(Items.IRON_ORE));
                var thermalProcessRecipe = ProcessingRecipeDenylist.findThermalProcessRecipe(
                        recipeManager, ironOreInput, helper.getLevel()
                );
                helper.assertTrue(thermalProcessRecipe.isPresent(),
                        "Thermal Process denylist should fall back to an allowed cooking recipe");
                helper.assertFalse(thermalProcessRecipe.get().id().equals(deniedBlastingRecipeId),
                        "Thermal Process selected a denied blasting recipe");
                helper.assertTrue(thermalProcessRecipe.get().id().equals(fallbackSmeltingRecipeId),
                        "Thermal Process fallback recipe mismatch: " + thermalProcessRecipe.get().id());
            }
        });
    }
}
