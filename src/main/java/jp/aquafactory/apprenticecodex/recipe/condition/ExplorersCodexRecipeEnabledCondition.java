package jp.aquafactory.apprenticecodex.recipe.condition;

import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexCommonConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

public final class ExplorersCodexRecipeEnabledCondition implements ICondition {
    public static final ExplorersCodexRecipeEnabledCondition INSTANCE = new ExplorersCodexRecipeEnabledCondition();
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "explorers_codex_recipe_enabled");

    private ExplorersCodexRecipeEnabledCondition() {
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return !ApprenticeCodexCommonConfig.disableExplorersCodexRecipe();
    }

    public static final class Serializer implements IConditionSerializer<ExplorersCodexRecipeEnabledCondition> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public void write(JsonObject json, ExplorersCodexRecipeEnabledCondition value) {
        }

        @Override
        public ExplorersCodexRecipeEnabledCondition read(JsonObject json) {
            return ExplorersCodexRecipeEnabledCondition.INSTANCE;
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
