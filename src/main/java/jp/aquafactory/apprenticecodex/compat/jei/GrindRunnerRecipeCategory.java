package jp.aquafactory.apprenticecodex.compat.jei;

import jp.aquafactory.apprenticecodex.recipe.grindrunner.GrindRunnerRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class GrindRunnerRecipeCategory extends AbstractApprenticeCodexRecipeCategory<GrindRunnerRecipe> {
    private static final int WIDTH = 126;
    private static final int HEIGHT = 62;
    private static final int INPUT_X = 4;
    private static final int INPUT_Y = 23;
    private static final int ARROW_X = 28;
    private static final int ARROW_Y = 24;
    private static final int OUTPUT_X = 60;
    private static final int OUTPUT_Y = 12;
    private static final int OUTPUT_COLUMNS = 2;
    private static final int SLOT_SPACING = 18;

    public GrindRunnerRecipeCategory(IGuiHelper guiHelper, ItemStack iconStack) {
        super(
                ApprenticeCodexJeiRecipeTypes.GRIND_RUNNER,
                Component.translatable("spell.apprenticecodex.grind_runner"),
                guiHelper,
                iconStack,
                WIDTH,
                HEIGHT
        );
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull GrindRunnerRecipe recipe, @NotNull IFocusGroup focuses) {
        var inputSlot = builder.addInputSlot(INPUT_X, INPUT_Y).setStandardSlotBackground();
        var inputStacks = copyIngredientStacks(recipe.getIngredient(), 1);
        if (inputStacks.isEmpty()) {
            inputSlot.addIngredients(recipe.getIngredient());
        } else {
            inputSlot.addItemStacks(inputStacks);
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
            @NotNull GrindRunnerRecipe recipe,
            @NotNull mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
            @NotNull net.minecraft.client.gui.GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        recipeArrow.draw(guiGraphics, ARROW_X, ARROW_Y);
    }
}
