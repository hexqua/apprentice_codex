package jp.aquafactory.apprenticecodex.compat.jei;

import jp.aquafactory.apprenticecodex.recipe.essencesmoker.EssenceSmokerRecipe;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class EssenceSmokerRecipeCategory extends AbstractApprenticeCodexRecipeCategory<EssenceSmokerRecipe> {
    private static final int WIDTH = 126;
    private static final int HEIGHT = 44;
    private static final int MATERIAL_X = 4;
    private static final int MATERIAL_Y = 1;
    private static final int CATALYST_X = 4;
    private static final int CATALYST_Y = 23;
    private static final int ARROW_X = 28;
    private static final int ARROW_Y = 15;
    private static final int OUTPUT_X = 58;
    private static final int OUTPUT_Y = 14;

    public EssenceSmokerRecipeCategory(IGuiHelper guiHelper) {
        super(
                ApprenticeCodexJeiRecipeTypes.ESSENCE_SMOKER,
                new ItemStack(ItemRegistry.ESSENCE_SMOKER.get()).getHoverName(),
                guiHelper,
                new ItemStack(ItemRegistry.ESSENCE_SMOKER.get()),
                WIDTH,
                HEIGHT
        );
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull EssenceSmokerRecipe recipe, @NotNull IFocusGroup focuses) {
        var materialSlot = builder.addInputSlot(MATERIAL_X, MATERIAL_Y).setStandardSlotBackground();
        var materialStacks = copyIngredientStacks(recipe.getMaterial(), 1);
        if (materialStacks.isEmpty()) {
            materialSlot.addIngredients(recipe.getMaterial());
        } else {
            materialSlot.addItemStacks(materialStacks);
        }

        var catalystSlot = builder.addInputSlot(CATALYST_X, CATALYST_Y).setStandardSlotBackground();
        var catalystStacks = copyIngredientStacks(recipe.getCatalyst(), 1);
        if (catalystStacks.isEmpty()) {
            catalystSlot.addIngredients(recipe.getCatalyst());
        } else {
            catalystSlot.addItemStacks(catalystStacks);
        }

        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y)
                .setOutputSlotBackground()
                .addItemStack(recipe.getResultTemplate());
    }

    @Override
    public void draw(
            @NotNull EssenceSmokerRecipe recipe,
            @NotNull mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
            @NotNull net.minecraft.client.gui.GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        recipeArrow.draw(guiGraphics, ARROW_X, ARROW_Y);
    }
}
