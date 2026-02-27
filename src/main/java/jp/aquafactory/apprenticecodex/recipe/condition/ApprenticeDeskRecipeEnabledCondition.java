package jp.aquafactory.apprenticecodex.recipe.condition;

import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

public final class ApprenticeDeskRecipeEnabledCondition implements ICondition {
    public static final ApprenticeDeskRecipeEnabledCondition INSTANCE = new ApprenticeDeskRecipeEnabledCondition();
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "apprentice_desk_recipe_enabled");

    private ApprenticeDeskRecipeEnabledCondition() {
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return !ApprenticeCodexServerConfig.disableApprenticeDeskRecipe();
    }

    public static final class Serializer implements IConditionSerializer<ApprenticeDeskRecipeEnabledCondition> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public void write(JsonObject json, ApprenticeDeskRecipeEnabledCondition value) {
        }

        @Override
        public ApprenticeDeskRecipeEnabledCondition read(JsonObject json) {
            return ApprenticeDeskRecipeEnabledCondition.INSTANCE;
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
