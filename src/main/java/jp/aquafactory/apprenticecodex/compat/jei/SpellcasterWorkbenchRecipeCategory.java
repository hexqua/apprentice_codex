package jp.aquafactory.apprenticecodex.compat.jei;

import jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipe;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class SpellcasterWorkbenchRecipeCategory extends AbstractApprenticeCodexRecipeCategory<SpellcasterWorkbenchRecipe> {
    private static final Component ARCHIVISTS_GRIMOIRE_UPGRADE_HINT =
            Component.translatable("jei.apprenticecodex.archivists_grimoire.upgrade_hint");
    private static final int WIDTH = 134;
    private static final int HEIGHT = 56;
    private static final int[][] INPUT_POSITIONS = {
            {4, 5},
            {24, 15},
            {4, 25}
    };
    private static final int ARROW_X = 48;
    private static final int ARROW_Y = 15;
    private static final int OUTPUT_X = 78;
    private static final int OUTPUT_Y = 13;
    private static final int OUTPUT_COLUMNS = 3;
    private static final int SLOT_SPACING = 18;

    public SpellcasterWorkbenchRecipeCategory(IGuiHelper guiHelper) {
        super(
                ApprenticeCodexJeiRecipeTypes.SPELLCASTER_WORKBENCH,
                new ItemStack(ItemRegistry.SPELLCASTER_WORKBENCH.get()).getHoverName(),
                guiHelper,
                new ItemStack(ItemRegistry.SPELLCASTER_WORKBENCH.get()),
                WIDTH,
                HEIGHT
        );
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull SpellcasterWorkbenchRecipe recipe, @NotNull IFocusGroup focuses) {
        var ingredients = recipe.getSizedIngredients();
        for (var index = 0; index < Math.min(INPUT_POSITIONS.length, ingredients.size()); ++index) {
            var sizedIngredient = ingredients.get(index);
            var slot = builder.addInputSlot(INPUT_POSITIONS[index][0], INPUT_POSITIONS[index][1]).setStandardSlotBackground();
            var ingredientStacks = copyIngredientStacks(sizedIngredient.ingredient(), sizedIngredient.count());
            if (ingredientStacks.isEmpty()) {
                slot.addIngredients(sizedIngredient.ingredient());
            } else {
                slot.addItemStacks(ingredientStacks);
            }
        }

        var outputs = recipe.getResultTemplates();
        for (var index = 0; index < outputs.size(); ++index) {
            var x = OUTPUT_X + index % OUTPUT_COLUMNS * SLOT_SPACING;
            var y = OUTPUT_Y + index / OUTPUT_COLUMNS * SLOT_SPACING;
            builder.addOutputSlot(x, y)
                    .setStandardSlotBackground()
                    .addItemStack(outputs.get(index).copy());
        }
    }

    @Override
    public void draw(
            @NotNull SpellcasterWorkbenchRecipe recipe,
            @NotNull mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
            @NotNull net.minecraft.client.gui.GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        recipeArrow.draw(guiGraphics, ARROW_X, ARROW_Y);
        if (isArchivistsGrimoireRowUpgradeRecipe(recipe)) {
            drawLabel(guiGraphics, ARCHIVISTS_GRIMOIRE_UPGRADE_HINT, 74, 36);
        }
    }

    private static boolean isArchivistsGrimoireRowUpgradeRecipe(SpellcasterWorkbenchRecipe recipe) {
        var outputs = recipe.getResultTemplates();
        return outputs.size() == 1
                && outputs.getFirst().is(ItemRegistry.ARCHIVISTS_GRIMOIRE.get())
                && recipe.getSizedIngredients().size() == SpellcasterWorkbenchRecipe.INPUT_SLOT_COUNT;
    }
}
