package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.recipe.essencesmoker.EssenceSmokerRecipe;
import jp.aquafactory.apprenticecodex.recipe.essencesmoker.EssenceSmokerRecipeSerializer;
import jp.aquafactory.apprenticecodex.recipe.grindrunner.GrindRunnerRecipe;
import jp.aquafactory.apprenticecodex.recipe.grindrunner.GrindRunnerRecipeSerializer;
import jp.aquafactory.apprenticecodex.recipe.smithing.SpellbookCarryoverSmithingRecipe;
import jp.aquafactory.apprenticecodex.recipe.smithing.SpellbookCarryoverSmithingRecipeSerializer;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipe;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipeSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
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
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SpellbookCarryoverSmithingRecipe>> SPELLBOOK_CARRYOVER_SMITHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("spellbook_carryover_smithing", SpellbookCarryoverSmithingRecipeSerializer::new);

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

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
        RECIPE_TYPES.register(bus);
    }
}
