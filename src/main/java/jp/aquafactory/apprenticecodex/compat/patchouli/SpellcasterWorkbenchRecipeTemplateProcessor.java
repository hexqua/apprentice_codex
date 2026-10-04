package jp.aquafactory.apprenticecodex.compat.patchouli;

import net.minecraft.world.level.Level;
import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;

public final class SpellcasterWorkbenchRecipeTemplateProcessor implements IComponentProcessor {
    private static final String SINGLE_RECIPE_TEXT_GROUP = "single_recipe_text";
    private static final String DOUBLE_RECIPE_TEXT_GROUP = "double_recipe_text";

    private boolean hasSecondDisplay;

    @Override
    public void setup(Level level, IVariableProvider variables) {
        hasSecondDisplay = hasValue(level, variables, "recipe2")
                || hasValue(level, variables, "example2");
    }

    private static boolean hasValue(Level level, IVariableProvider variables, String key) {
        return variables.has(key) && !variables.get(key).asString("").isBlank();
    }

    @Override
    public IVariable process(Level level, String key) {
        return null;
    }

    @Override
    public boolean allowRender(String group) {
        return switch (group) {
            case SINGLE_RECIPE_TEXT_GROUP -> !hasSecondDisplay;
            case DOUBLE_RECIPE_TEXT_GROUP -> hasSecondDisplay;
            default -> true;
        };
    }
}
