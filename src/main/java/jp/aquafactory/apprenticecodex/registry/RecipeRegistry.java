package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.recipe.grindrunner.GrindRunnerRecipe;
import jp.aquafactory.apprenticecodex.recipe.grindrunner.GrindRunnerRecipeSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
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

    public static final RegistryObject<RecipeType<GrindRunnerRecipe>> GRIND_RUNNER_RECIPE_TYPE =
            RECIPE_TYPES.register("grind_runner", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return ApprenticeCodex.MODID + ":grind_runner";
                }
            });

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
        RECIPE_TYPES.register(bus);
    }
}
