package jp.aquafactory.apprenticecodex.recipe.condition;

import com.mojang.serialization.MapCodec;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexCommonConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

public final class ArcanumInAJarRecipeEnabledCondition implements ICondition {
    public static final ArcanumInAJarRecipeEnabledCondition INSTANCE = new ArcanumInAJarRecipeEnabledCondition();
    public static final MapCodec<ArcanumInAJarRecipeEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    private ArcanumInAJarRecipeEnabledCondition() {
    }

    @Override
    public boolean test(IContext context) {
        return !ApprenticeCodexCommonConfig.disableArcanumInAJarRecipe();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
