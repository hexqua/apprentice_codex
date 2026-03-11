package jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record SpellcasterWorkbenchRecipeInput(
        ItemStack first,
        ItemStack second,
        ItemStack third
) implements RecipeInput {
    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case 0 -> first;
            case 1 -> second;
            case 2 -> third;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int size() {
        return 3;
    }
}
