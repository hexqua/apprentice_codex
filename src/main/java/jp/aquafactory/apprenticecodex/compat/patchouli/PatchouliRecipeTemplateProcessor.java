package jp.aquafactory.apprenticecodex.compat.patchouli;

import jp.aquafactory.apprenticecodex.compat.spellcasterworkbench.SpellcasterWorkbenchDisplayExamples;
import jp.aquafactory.apprenticecodex.recipe.essencesmoker.EssenceSmokerRecipe;
import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;

import java.util.ArrayList;

abstract class PatchouliRecipeTemplateProcessor implements IComponentProcessor {
    private final boolean workbench;
    private IVariableProvider variables;
    private int textY;

    protected PatchouliRecipeTemplateProcessor(boolean workbench) {
        this.workbench = workbench;
    }

    @Override
    public void setup(Level level, IVariableProvider variables) {
        this.variables = variables;
        updateLayout(level);
    }

    @Override
    public void refresh(Screen parent, int left, int top) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            updateLayout(level);
        }
    }

    private void updateLayout(Level level) {
        var outputs = new ArrayList<ItemStack>();
        var maxRecipes = workbench ? 2 : 3;
        for (var index = 0; index < maxRecipes; ++index) {
            var suffix = index == 0 ? "" : Integer.toString(index + 1);
            var recipe = value(level, "recipe" + suffix);
            var example = workbench ? value(level, "example" + suffix) : "";
            if (!recipe.isBlank() && ResourceLocation.tryParse(recipe) != null) {
                outputs.add(resolveOutput(level, recipe, false));
            } else if (!example.isBlank() && ResourceLocation.tryParse(example) != null) {
                outputs.add(resolveOutput(level, example, true));
            }
        }
        if (!workbench && outputs.isEmpty() && variables.has("recipe_ids")) {
            for (var id : variables.get("recipe_ids").asListOrSingleton()) {
                var rawId = id.asString("").trim();
                if (ResourceLocation.tryParse(rawId) != null) {
                    outputs.add(resolveOutput(level, rawId, false));
                    if (outputs.size() == maxRecipes) {
                        break;
                    }
                }
            }
        }
        var title = PatchouliRecipeLayout.resolveTitle("#title", variable -> {
            var key = variable.asString("").replaceFirst("^#", "");
            return variables.has(key) ? variables.get(key) : IVariable.empty();
        });
        textY = PatchouliRecipeLayout.create(outputs, title, workbench
                ? PatchouliRecipeLayout.WORKBENCH_ROW_HEIGHT : PatchouliRecipeLayout.SMOKER_ROW_HEIGHT).textY();
    }

    private String value(Level level, String key) {
        return variables.has(key) ? variables.get(key).asString("").trim() : "";
    }

    private ItemStack resolveOutput(Level level, String rawId, boolean example) {
        var id = ResourceLocation.tryParse(rawId);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        if (workbench && example) {
            return SpellcasterWorkbenchDisplayExamples.all().stream()
                    .filter(value -> value.id().equals(id))
                    .map(value -> value.recipe().getResultTemplates().get(0))
                    .findFirst().orElse(ItemStack.EMPTY);
        }
        var holder = level.getRecipeManager().byKey(id).orElse(null);
        if (holder != null) {
            if (workbench && holder instanceof SpellcasterWorkbenchRecipe recipe) {
                return recipe.getResultTemplates().get(0);
            }
            if (!workbench && holder instanceof EssenceSmokerRecipe recipe) {
                return recipe.getResultTemplate();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    @SuppressWarnings("NullableProblems") // Patchouli APIではnullが「変数を加工しない」を表す。
    public @Nullable IVariable process(Level level, String key) {
        return null;
    }

    @Override
    public boolean allowRender(String group) {
        // 標準text部品の座標は変数にできないため、同じ配置計算に対応する部品だけを表示する。
        return !group.startsWith("recipe_text_") || group.equals("recipe_text_" + textY);
    }
}
