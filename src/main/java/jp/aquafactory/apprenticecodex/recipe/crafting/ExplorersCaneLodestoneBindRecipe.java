package jp.aquafactory.apprenticecodex.recipe.crafting;

import jp.aquafactory.apprenticecodex.item.offhand.ExplorersCane;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class ExplorersCaneLodestoneBindRecipe extends CustomRecipe {
    public ExplorersCaneLodestoneBindRecipe(CraftingBookCategory category) {
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

        var result = match.caneStack().copy();
        return ExplorersCane.copyLodestoneData(result, match.compassStack()) ? result : ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        var match = findMatch(input);
        if (match == null) {
            return remaining;
        }

        var returnedCompass = match.compassStack().copy();
        returnedCompass.setCount(1);
        remaining.set(match.compassSlot(), returnedCompass);
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.EXPLORERS_CANE_LODESTONE_BIND_SERIALIZER.get();
    }

    private static Match findMatch(CraftingInput input) {
        ItemStack caneStack = ItemStack.EMPTY;
        ItemStack compassStack = ItemStack.EMPTY;
        int compassSlot = -1;

        for (int i = 0; i < input.size(); ++i) {
            var stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(ItemRegistry.EXPLORERS_CANE.get())) {
                if (!caneStack.isEmpty()) {
                    return null;
                }

                caneStack = stack;
                continue;
            }

            if (stack.is(Items.COMPASS)
                    && ExplorersCane.hasTransferableLodestoneData(stack)) {
                if (!compassStack.isEmpty()) {
                    return null;
                }

                compassStack = stack;
                compassSlot = i;
                continue;
            }

            return null;
        }

        return caneStack.isEmpty() || compassStack.isEmpty() ? null : new Match(caneStack, compassStack, compassSlot);
    }

    private record Match(ItemStack caneStack, ItemStack compassStack, int compassSlot) {
    }
}
