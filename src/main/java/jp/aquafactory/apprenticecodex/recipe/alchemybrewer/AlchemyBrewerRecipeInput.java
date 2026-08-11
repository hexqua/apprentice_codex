package jp.aquafactory.apprenticecodex.recipe.alchemybrewer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

public final class AlchemyBrewerRecipeInput implements RecipeInput {
    public static final AlchemyBrewerRecipeInput EMPTY = new AlchemyBrewerRecipeInput();

    private AlchemyBrewerRecipeInput() {
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 0;
    }
}
