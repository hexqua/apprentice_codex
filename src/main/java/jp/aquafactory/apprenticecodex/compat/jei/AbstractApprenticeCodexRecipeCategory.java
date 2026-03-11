package jp.aquafactory.apprenticecodex.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractApprenticeCodexRecipeCategory<T> implements IRecipeCategory<T> {
    private static final int TEXT_COLOR = 0x404040;

    private final RecipeType<T> recipeType;
    private final Component title;
    private final IDrawable icon;
    protected final IDrawable recipeArrow;
    private final int width;
    private final int height;

    protected AbstractApprenticeCodexRecipeCategory(
            RecipeType<T> recipeType,
            Component title,
            IGuiHelper guiHelper,
            ItemStack iconStack,
            int width,
            int height
    ) {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableItemStack(iconStack);
        this.recipeArrow = guiHelper.getRecipeArrow();
        this.width = width;
        this.height = height;
    }

    @Override
    public final @NotNull RecipeType<T> getRecipeType() {
        return recipeType;
    }

    @Override
    public final @NotNull Component getTitle() {
        return title;
    }

    @Override
    public final int getWidth() {
        return width;
    }

    @Override
    public final int getHeight() {
        return height;
    }

    @Override
    public final @NotNull IDrawable getIcon() {
        return icon;
    }

    protected static @NotNull List<ItemStack> copyIngredientStacks(Ingredient ingredient, int count) {
        var stacks = ingredient.getItems();
        if (stacks.length == 0) {
            return List.of();
        }

        var copies = new ArrayList<ItemStack>(stacks.length);
        for (var stack : stacks) {
            var copy = stack.copy();
            copy.setCount(Math.max(1, count));
            copies.add(copy);
        }
        return copies;
    }

    protected static void drawLabel(GuiGraphics guiGraphics, Component label, int x, int y) {
        guiGraphics.drawString(Minecraft.getInstance().font, label, x, y, TEXT_COLOR, false);
    }
}
