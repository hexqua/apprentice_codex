package jp.aquafactory.apprenticecodex.recipe.condition;

import com.mojang.serialization.MapCodec;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexCommonConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

public final class ExplorersCodexRecipeEnabledCondition implements ICondition {
    public static final ExplorersCodexRecipeEnabledCondition INSTANCE = new ExplorersCodexRecipeEnabledCondition();
    public static final MapCodec<ExplorersCodexRecipeEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    private ExplorersCodexRecipeEnabledCondition() {
    }

    @Override
    public boolean test(IContext context) {
        return !ApprenticeCodexCommonConfig.disableExplorersCodexRecipe();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
