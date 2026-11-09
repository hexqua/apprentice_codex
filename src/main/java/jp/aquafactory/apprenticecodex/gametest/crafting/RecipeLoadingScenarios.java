package jp.aquafactory.apprenticecodex.gametest.crafting;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fml.ModList;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.crafting.RecipeTestSupport.*;
import static jp.aquafactory.apprenticecodex.gametest.integration.create.CreateProcessingTestSupport.CREATE_MOD_ID;

final class RecipeLoadingScenarios {
    private RecipeLoadingScenarios() {}

    static void customRecipeDataIsLoaded(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipeManager = helper.getLevel().getRecipeManager();

            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_transfer"),
                    RecipeRegistry.SPELLCASTERS_FLASK_TRANSFER_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcasters_flask_extract"),
                    RecipeRegistry.SPELLCASTERS_FLASK_EXTRACT_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemists_flask"),
                    RecipeRegistry.ALCHEMISTS_FLASK_SMITHING_SERIALIZER.get(), RecipeType.SMITHING);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemists_flask_tipped_arrow"),
                    RecipeRegistry.ALCHEMISTS_FLASK_TIPPED_ARROW_SERIALIZER.get(), RecipeType.CRAFTING);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "explorers_cane_lodestone_bind"),
                    RecipeRegistry.EXPLORERS_CANE_LODESTONE_BIND_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "explorers_codex_guidebook_transfer"),
                    RecipeRegistry.EXPLORERS_CODEX_GUIDEBOOK_TRANSFER_SERIALIZER.get(), null);
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "comfort_sandwich"),
                    RecipeSerializer.SHAPELESS_RECIPE,
                    RecipeType.CRAFTING);
            var guidebookRecipeId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "isekai_travel_guidebook");
            assertRecipeLoaded(helper, recipeManager,
                    ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.getId(),
                    RecipeSerializer.SHAPED_RECIPE, RecipeType.CRAFTING);
            for (var recipePath : List.of("fullauto_spell_casting_round", "fullauto_spell_casting_round_recycle")) {
                assertRecipeLoaded(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_workbench/" + recipePath),
                        RecipeRegistry.SPELLCASTER_WORKBENCH_SERIALIZER.get(), RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get());
            }
            helper.assertTrue(recipeManager.byKey(guidebookRecipeId).isEmpty(),
                    "Isekai Travel Guidebook crafting recipe should not be loaded: " + guidebookRecipeId);

            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "essence_smoker/infuse_coal_to_arcane_cinder"),
                    RecipeRegistry.ESSENCE_SMOKER_SERIALIZER.get(), RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get());
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "grind_runner/bone_meal_from_bone"),
                    RecipeRegistry.GRIND_RUNNER_SERIALIZER.get(), RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get());
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_workbench/basic_spellcaster_round"),
                    RecipeRegistry.SPELLCASTER_WORKBENCH_SERIALIZER.get(), RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get());
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_workbench/multi_purpose_spell_round"),
                    RecipeRegistry.SPELLCASTER_WORKBENCH_SERIALIZER.get(), RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get());
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_workbench/multi_purpose_spell_round_recycle"),
                    RecipeRegistry.SPELLCASTER_WORKBENCH_SERIALIZER.get(), RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get());
            assertRecipeLoaded(helper, recipeManager,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemist_cauldron/brew_isekai_travel_guidebook_to_common_ink"),
                    io.redspace.ironsspellbooks.registries.RecipeRegistry.ALCHEMIST_CAULDRON_BREW_SERIALIZER.get(),
                    io.redspace.ironsspellbooks.registries.RecipeRegistry.ALCHEMIST_CAULDRON_BREW_TYPE.get());

            helper.assertFalse(recipeManager.getAllRecipesFor(RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get()).isEmpty(),
                    "No Essence Smoker recipes were loaded");
            helper.assertFalse(recipeManager.getAllRecipesFor(RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get()).isEmpty(),
                    "No Grind Runner recipes were loaded");
            helper.assertFalse(recipeManager.getAllRecipesFor(RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get()).isEmpty(),
                    "No Spellcaster Workbench recipes were loaded");

            if (ModList.get().isLoaded(CREATE_MOD_ID)) {
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spell_bullet_mold"),
                        ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_shaped"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spell_casing_mold"),
                        ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_shaped"));
                assertShapedRecipeCenterIngredientMatches(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spell_casing_mold"),
                        new ItemStack(ItemRegistry.EMPTY_MULTI_PURPOSE_SPELL_CASING.get()),
                        new ItemStack(ItemRegistry.RAPID_SPELLCASTER_ROUND.get()),
                        "Spell Casing Mold recipe should use the empty casing tag");
                assertShapedRecipeCenterIngredientMatches(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spell_bullet_mold"),
                        new ItemStack(ItemRegistry.RAPID_SPELLCASTER_ROUND.get()),
                        new ItemStack(ItemRegistry.EMPTY_RAPID_SPELLCASTER_CASING.get()),
                        "Spell Bullet Mold recipe should use Rapid Spellcaster Round");
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/mixing/arcane_propellant_charge"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "mixing"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/crushing/crystalline_arcane_shard"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "crushing"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/haunting/comfort_berries"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "haunting"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/deploying/spell_bullet_head"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "deploying"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/deploying/empty_rapid_spellcaster_casing"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "deploying"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/deploying/empty_basic_spellcaster_casing"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "deploying"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/deploying/empty_arcane_spellcaster_casing"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "deploying"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/deploying/empty_advanced_spellcaster_casing"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "deploying"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/deploying/empty_spell_dominator_casing"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "deploying"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/deploying/empty_multi_purpose_spell_casing"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "deploying"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/deploying/empty_fullauto_spell_casting_casing"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "deploying"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/sequenced_assembly/rapid_spellcaster_round"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "sequenced_assembly"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/sequenced_assembly/basic_spellcaster_round"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "sequenced_assembly"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/sequenced_assembly/arcane_spellcaster_round"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "sequenced_assembly"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/sequenced_assembly/advanced_spellcaster_round"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "sequenced_assembly"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/sequenced_assembly/spell_dominator_round"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "sequenced_assembly"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/sequenced_assembly/multi_purpose_spell_round"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "sequenced_assembly"));
                assertRecipeLoadedWithSerializerId(helper, recipeManager,
                        ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "create/sequenced_assembly/fullauto_spell_casting_round"),
                        ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "sequenced_assembly"));
            }
        });
    }
}
