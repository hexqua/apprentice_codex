package jp.aquafactory.apprenticecodex.recipe.crafting;

import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class SpellcastersFlaskTransferRecipe extends CustomRecipe {
    public SpellcastersFlaskTransferRecipe(ResourceLocation recipeId, CraftingBookCategory category) {
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

        return SpellcastersFlask.copyWithAddedDose(match.flaskStack(), match.transferItemStack());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        var remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        var match = findMatch(container);
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

    private static Match findMatch(CraftingContainer container) {
        ItemStack flaskStack = ItemStack.EMPTY;
        ItemStack transferItemStack = ItemStack.EMPTY;
        int transferItemSlot = -1;

        for (int i = 0; i < container.getContainerSize(); ++i) {
            var stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof AbstractPotionFlaskItem) {
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
