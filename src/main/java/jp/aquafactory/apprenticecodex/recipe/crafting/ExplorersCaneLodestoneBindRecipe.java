package jp.aquafactory.apprenticecodex.recipe.crafting;

import jp.aquafactory.apprenticecodex.item.offhand.ExplorersCane;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class ExplorersCaneLodestoneBindRecipe extends CustomRecipe {
    public ExplorersCaneLodestoneBindRecipe(ResourceLocation recipeId, CraftingBookCategory category) {
        super(recipeId, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return findMatch(container) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        var match = findMatch(container);
        if (match == null) {
            return ItemStack.EMPTY;
        }

        var result = match.caneStack().copy();
        return ExplorersCane.copyLodestoneData(result, match.compassStack()) ? result : ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        var remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        var match = findMatch(container);
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

    private static Match findMatch(CraftingContainer container) {
        ItemStack caneStack = ItemStack.EMPTY;
        ItemStack compassStack = ItemStack.EMPTY;
        int compassSlot = -1;

        for (int i = 0; i < container.getContainerSize(); ++i) {
            var stack = container.getItem(i);
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
                    && CompassItem.isLodestoneCompass(stack)
                    && stack.getTag() != null
                    && CompassItem.getLodestonePosition(stack.getTag()) != null) {
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
