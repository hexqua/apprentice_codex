package jp.aquafactory.apprenticecodex.registry;

import com.mojang.serialization.MapCodec;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.recipe.condition.ApprenticeDeskRecipeEnabledCondition;
import jp.aquafactory.apprenticecodex.recipe.condition.ArcanumInAJarRecipeEnabledCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class RecipeConditionRegistry {
    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, ApprenticeCodex.MODID);

    static {
        CONDITION_CODECS.register("apprentice_desk_recipe_enabled", () -> ApprenticeDeskRecipeEnabledCondition.CODEC);
        CONDITION_CODECS.register("arcanum_in_a_jar_recipe_enabled", () -> ArcanumInAJarRecipeEnabledCondition.CODEC);
    }

    private RecipeConditionRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        CONDITION_CODECS.register(modEventBus);
    }
}
