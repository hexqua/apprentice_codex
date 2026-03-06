package jp.aquafactory.apprenticecodex.recipe.condition;

import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexCommonConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

public final class ArcanumInAJarRecipeEnabledCondition implements ICondition {
    public static final ArcanumInAJarRecipeEnabledCondition INSTANCE = new ArcanumInAJarRecipeEnabledCondition();
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "arcanum_in_a_jar_recipe_enabled");

    private ArcanumInAJarRecipeEnabledCondition() {
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return !ApprenticeCodexCommonConfig.disableArcanumInAJarRecipe();
    }

    public static final class Serializer implements IConditionSerializer<ArcanumInAJarRecipeEnabledCondition> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public void write(JsonObject json, ArcanumInAJarRecipeEnabledCondition value) {
        }

        @Override
        public ArcanumInAJarRecipeEnabledCondition read(JsonObject json) {
            return ArcanumInAJarRecipeEnabledCondition.INSTANCE;
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
