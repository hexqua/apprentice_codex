package jp.aquafactory.apprenticecodex.recipe.condition;

import com.mojang.serialization.MapCodec;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexCommonConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

public final class ApprenticeDeskRecipeEnabledCondition implements ICondition {
    public static final ApprenticeDeskRecipeEnabledCondition INSTANCE = new ApprenticeDeskRecipeEnabledCondition();
    public static final MapCodec<ApprenticeDeskRecipeEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    private ApprenticeDeskRecipeEnabledCondition() {
    }

    @Override
    public boolean test(IContext context) {
        return !ApprenticeCodexCommonConfig.disableApprenticeDeskRecipe();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
