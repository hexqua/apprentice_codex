package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.recipe.essencesmoker.EssenceSmokerRecipe;
import jp.aquafactory.apprenticecodex.recipe.essencesmoker.EssenceSmokerRecipeSerializer;
import jp.aquafactory.apprenticecodex.recipe.crafting.ExplorersCodexGuidebookTransferRecipe;
import jp.aquafactory.apprenticecodex.recipe.crafting.ExplorersCaneLodestoneBindRecipe;
import jp.aquafactory.apprenticecodex.recipe.crafting.SpellcastersFlaskExtractRecipe;
import jp.aquafactory.apprenticecodex.recipe.crafting.SpellcastersFlaskTransferRecipe;
import jp.aquafactory.apprenticecodex.recipe.grindrunner.GrindRunnerRecipe;
import jp.aquafactory.apprenticecodex.recipe.grindrunner.GrindRunnerRecipeSerializer;
import jp.aquafactory.apprenticecodex.recipe.crafting.IronsGuideBookRepairRecipe;
import jp.aquafactory.apprenticecodex.recipe.smithing.SpellbookCarryoverSmithingRecipe;
import jp.aquafactory.apprenticecodex.recipe.smithing.SpellbookCarryoverSmithingRecipeSerializer;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipe;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipeSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class RecipeRegistry {
    private RecipeRegistry() {
    }

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ApprenticeCodex.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ApprenticeCodex.MODID);

    public static final RegistryObject<RecipeSerializer<GrindRunnerRecipe>> GRIND_RUNNER_SERIALIZER =
            RECIPE_SERIALIZERS.register("grind_runner", GrindRunnerRecipeSerializer::new);
    public static final RegistryObject<RecipeSerializer<EssenceSmokerRecipe>> ESSENCE_SMOKER_SERIALIZER =
            RECIPE_SERIALIZERS.register("essence_smoker", EssenceSmokerRecipeSerializer::new);
    public static final RegistryObject<RecipeSerializer<SpellbookCarryoverSmithingRecipe>> SPELLBOOK_CARRYOVER_SMITHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("spellbook_carryover_smithing", SpellbookCarryoverSmithingRecipeSerializer::new);
    public static final RegistryObject<RecipeSerializer<SpellcasterWorkbenchRecipe>> SPELLCASTER_WORKBENCH_SERIALIZER =
            RECIPE_SERIALIZERS.register("spellcaster_workbench", SpellcasterWorkbenchRecipeSerializer::new);
    public static final RegistryObject<RecipeSerializer<IronsGuideBookRepairRecipe>> IRONS_GUIDE_BOOK_REPAIR_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    "irons_guide_book_repair",
                    () -> new SimpleCraftingRecipeSerializer<>(IronsGuideBookRepairRecipe::new)
            );
    public static final RegistryObject<RecipeSerializer<ExplorersCaneLodestoneBindRecipe>> EXPLORERS_CANE_LODESTONE_BIND_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    "explorers_cane_lodestone_bind",
                    () -> new SimpleCraftingRecipeSerializer<>(ExplorersCaneLodestoneBindRecipe::new)
            );
    public static final RegistryObject<RecipeSerializer<ExplorersCodexGuidebookTransferRecipe>> EXPLORERS_CODEX_GUIDEBOOK_TRANSFER_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    "explorers_codex_guidebook_transfer",
                    () -> new SimpleCraftingRecipeSerializer<>(ExplorersCodexGuidebookTransferRecipe::new)
            );
    public static final RegistryObject<RecipeSerializer<SpellcastersFlaskTransferRecipe>> SPELLCASTERS_FLASK_TRANSFER_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    "spellcasters_flask_transfer",
                    () -> new SimpleCraftingRecipeSerializer<>(SpellcastersFlaskTransferRecipe::new)
            );
    public static final RegistryObject<RecipeSerializer<SpellcastersFlaskExtractRecipe>> SPELLCASTERS_FLASK_EXTRACT_SERIALIZER =
            RECIPE_SERIALIZERS.register(
                    "spellcasters_flask_extract",
                    () -> new SimpleCraftingRecipeSerializer<>(SpellcastersFlaskExtractRecipe::new)
            );

    public static final RegistryObject<RecipeType<GrindRunnerRecipe>> GRIND_RUNNER_RECIPE_TYPE =
            RECIPE_TYPES.register("grind_runner", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return ApprenticeCodex.MODID + ":grind_runner";
                }
            });
    public static final RegistryObject<RecipeType<EssenceSmokerRecipe>> ESSENCE_SMOKER_RECIPE_TYPE =
            RECIPE_TYPES.register("essence_smoker", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return ApprenticeCodex.MODID + ":essence_smoker";
                }
            });
    public static final RegistryObject<RecipeType<SpellcasterWorkbenchRecipe>> SPELLCASTER_WORKBENCH_RECIPE_TYPE =
            RECIPE_TYPES.register("spellcaster_workbench", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return ApprenticeCodex.MODID + ":spellcaster_workbench";
                }
            });

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
        RECIPE_TYPES.register(bus);
    }
}
