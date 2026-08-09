package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.recipe.essencesmoker.EssenceSmokerRecipe;
import jp.aquafactory.apprenticecodex.recipe.alchemybrewer.AlchemyBrewerModifierRecipe;
import jp.aquafactory.apprenticecodex.recipe.alchemybrewer.AlchemyBrewerModifierRecipeSerializer;
import jp.aquafactory.apprenticecodex.recipe.alchemybrewer.AlchemyBrewerRecipe;
import jp.aquafactory.apprenticecodex.recipe.alchemybrewer.AlchemyBrewerRecipeSerializer;
import jp.aquafactory.apprenticecodex.recipe.essencesmoker.EssenceSmokerRecipeSerializer;
import jp.aquafactory.apprenticecodex.recipe.crafting.ExplorersCodexGuidebookTransferRecipe;
import jp.aquafactory.apprenticecodex.recipe.crafting.ExplorersCaneLodestoneBindRecipe;
import jp.aquafactory.apprenticecodex.recipe.crafting.AlchemistsFlaskTippedArrowRecipe;
import jp.aquafactory.apprenticecodex.recipe.crafting.SpellcastersFlaskExtractRecipe;
import jp.aquafactory.apprenticecodex.recipe.crafting.SpellcastersFlaskTransferRecipe;
import jp.aquafactory.apprenticecodex.recipe.grindrunner.GrindRunnerRecipe;
import jp.aquafactory.apprenticecodex.recipe.grindrunner.GrindRunnerRecipeSerializer;
import jp.aquafactory.apprenticecodex.recipe.smithing.AlchemistsFlaskSmithingRecipe;
import jp.aquafactory.apprenticecodex.recipe.smithing.AlchemistsFlaskSmithingRecipeSerializer;
import jp.aquafactory.apprenticecodex.recipe.smithing.SpellbookCarryoverSmithingRecipe;
import jp.aquafactory.apprenticecodex.recipe.smithing.SpellbookCarryoverSmithingRecipeSerializer;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipe;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipeSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RecipeRegistry {
    private RecipeRegistry() {
    }

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ApprenticeCodex.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ApprenticeCodex.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GrindRunnerRecipe>> GRIND_RUNNER_SERIALIZER =
            RECIPE_SERIALIZERS.register("grind_runner", GrindRunnerRecipeSerializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EssenceSmokerRecipe>> ESSENCE_SMOKER_SERIALIZER =
            RECIPE_SERIALIZERS.register("essence_smoker", EssenceSmokerRecipeSerializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SpellcasterWorkbenchRecipe>> SPELLCASTER_WORKBENCH_SERIALIZER =
            RECIPE_SERIALIZERS.register("spellcaster_workbench", SpellcasterWorkbenchRecipeSerializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AlchemyBrewerRecipe>> ALCHEMY_BREWER_SERIALIZER =
            RECIPE_SERIALIZERS.register("alchemy_brewer", AlchemyBrewerRecipeSerializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AlchemyBrewerModifierRecipe>> ALCHEMY_BREWER_MODIFIER_SERIALIZER =
            RECIPE_SERIALIZERS.register("alchemy_brewer_modifier", AlchemyBrewerModifierRecipeSerializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SpellbookCarryoverSmithingRecipe>> SPELLBOOK_CARRYOVER_SMITHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("spellbook_carryover_smithing", SpellbookCarryoverSmithingRecipeSerializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AlchemistsFlaskSmithingRecipe>> ALCHEMISTS_FLASK_SMITHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("alchemists_flask_smithing", AlchemistsFlaskSmithingRecipeSerializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ExplorersCaneLodestoneBindRecipe>> EXPLORERS_CANE_LODESTONE_BIND_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    "explorers_cane_lodestone_bind",
                    () -> new SimpleCraftingRecipeSerializer<>(ExplorersCaneLodestoneBindRecipe::new)
            );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ExplorersCodexGuidebookTransferRecipe>> EXPLORERS_CODEX_GUIDEBOOK_TRANSFER_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    "explorers_codex_guidebook_transfer",
                    () -> new SimpleCraftingRecipeSerializer<>(ExplorersCodexGuidebookTransferRecipe::new)
            );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SpellcastersFlaskTransferRecipe>> SPELLCASTERS_FLASK_TRANSFER_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    "spellcasters_flask_transfer",
                    () -> new SimpleCraftingRecipeSerializer<>(SpellcastersFlaskTransferRecipe::new)
            );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SpellcastersFlaskExtractRecipe>> SPELLCASTERS_FLASK_EXTRACT_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    "spellcasters_flask_extract",
                    () -> new SimpleCraftingRecipeSerializer<>(SpellcastersFlaskExtractRecipe::new)
            );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AlchemistsFlaskTippedArrowRecipe>> ALCHEMISTS_FLASK_TIPPED_ARROW_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    "alchemists_flask_tipped_arrow",
                    () -> new SimpleCraftingRecipeSerializer<>(AlchemistsFlaskTippedArrowRecipe::new)
            );

    public static final DeferredHolder<RecipeType<?>, RecipeType<GrindRunnerRecipe>> GRIND_RUNNER_RECIPE_TYPE =
            RECIPE_TYPES.register("grind_runner", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return ApprenticeCodex.MODID + ":grind_runner";
                }
            });
    public static final DeferredHolder<RecipeType<?>, RecipeType<EssenceSmokerRecipe>> ESSENCE_SMOKER_RECIPE_TYPE =
            RECIPE_TYPES.register("essence_smoker", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return ApprenticeCodex.MODID + ":essence_smoker";
                }
            });
    public static final DeferredHolder<RecipeType<?>, RecipeType<SpellcasterWorkbenchRecipe>> SPELLCASTER_WORKBENCH_RECIPE_TYPE =
            RECIPE_TYPES.register("spellcaster_workbench", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return ApprenticeCodex.MODID + ":spellcaster_workbench";
                }
            });
    public static final DeferredHolder<RecipeType<?>, RecipeType<AlchemyBrewerRecipe>> ALCHEMY_BREWER_RECIPE_TYPE =
            RECIPE_TYPES.register("alchemy_brewer", () -> new RecipeType<>() {
                @Override public String toString() { return ApprenticeCodex.MODID + ":alchemy_brewer"; }
            });
    public static final DeferredHolder<RecipeType<?>, RecipeType<AlchemyBrewerModifierRecipe>> ALCHEMY_BREWER_MODIFIER_RECIPE_TYPE =
            RECIPE_TYPES.register("alchemy_brewer_modifier", () -> new RecipeType<>() {
                @Override public String toString() { return ApprenticeCodex.MODID + ":alchemy_brewer_modifier"; }
            });

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
        RECIPE_TYPES.register(bus);
    }
}
