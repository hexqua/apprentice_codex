package jp.aquafactory.apprenticecodex.compat.patchouli;

import net.minecraft.world.level.Level;
import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;

public final class SpellcasterWorkbenchRecipeTemplateProcessor implements IComponentProcessor {
    private static final String SINGLE_RECIPE_TEXT_GROUP = "single_recipe_text";
    private static final String DOUBLE_RECIPE_TEXT_GROUP = "double_recipe_text";

    private boolean hasSecondRecipe;

    @Override
    public void setup(Level level, IVariableProvider variables) {
        hasSecondRecipe = variables.has("recipe2")
                && !variables.get("recipe2", level.registryAccess()).asString("").isBlank();
    }

    @Override
    public IVariable process(Level level, String key) {
        return null;
    }

    @Override
    public boolean allowRender(String group) {
        return switch (group) {
            case SINGLE_RECIPE_TEXT_GROUP -> !hasSecondRecipe;
            case DOUBLE_RECIPE_TEXT_GROUP -> hasSecondRecipe;
            default -> true;
        };
    }
}
