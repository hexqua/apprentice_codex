package jp.aquafactory.apprenticecodex.recipe.crafting;

import jp.aquafactory.apprenticecodex.item.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class SpellcastersFlaskTransferRecipe extends CustomRecipe {
    public SpellcastersFlaskTransferRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findMatch(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        var match = findMatch(input);
        if (match == null) {
            return ItemStack.EMPTY;
        }

        return SpellcastersFlask.copyWithAddedDose(match.flaskStack(), match.transferItemStack());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        var match = findMatch(input);
        if (match == null) {
            return remaining;
        }

        var remainder = SpellcastersFlask.getTransferCraftingRemainder(match.transferItemStack());
        if (remainder.isEmpty()) {
            return remaining;
        }

        remaining.set(match.transferItemSlot(), remainder);
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.SPELLCASTERS_FLASK_TRANSFER_SERIALIZER.get();
    }

    private static Match findMatch(CraftingInput input) {
        ItemStack flaskStack = ItemStack.EMPTY;
        ItemStack transferItemStack = ItemStack.EMPTY;
        int transferItemSlot = -1;

        for (int i = 0; i < input.size(); ++i) {
            var stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(ItemRegistry.SPELLCASTERS_FLASK.get())) {
                if (!flaskStack.isEmpty()) {
                    return null;
                }

                flaskStack = stack;
                continue;
            }

            if (!transferItemStack.isEmpty()) {
                return null;
            }

            transferItemStack = stack;
            transferItemSlot = i;
        }

        if (flaskStack.isEmpty() || transferItemStack.isEmpty()) {
            return null;
        }

        return SpellcastersFlask.canAddDoseFromItem(flaskStack, transferItemStack)
                ? new Match(flaskStack, transferItemStack, transferItemSlot)
                : null;
    }

    private record Match(ItemStack flaskStack, ItemStack transferItemStack, int transferItemSlot) {
    }
}
