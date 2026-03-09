package jp.aquafactory.apprenticecodex.recipe.essencesmoker;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record EssenceSmokerRecipeInput(ItemStack catalyst, ItemStack material) implements RecipeInput {
    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case 0 -> catalyst;
            case 1 -> material;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int size() {
        return 2;
    }
}
