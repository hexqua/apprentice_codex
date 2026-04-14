package jp.aquafactory.apprenticecodex.recipe.crafting;

import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class SpellcastersFlaskExtractRecipe extends CustomRecipe {
    public SpellcastersFlaskExtractRecipe(ResourceLocation recipeId, CraftingBookCategory category) {
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

        return SpellcastersFlask.copyStoredItemForCrafting(match.flaskStack());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        var remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        var match = findMatch(container);
        if (match == null) {
            return remaining;
        }

        remaining.set(match.flaskSlot(), SpellcastersFlask.copyAfterExtractingOneDose(match.flaskStack()));
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.SPELLCASTERS_FLASK_EXTRACT_SERIALIZER.get();
    }

    private static Match findMatch(CraftingContainer container) {
        ItemStack flaskStack = ItemStack.EMPTY;
        int flaskSlot = -1;
        ItemStack bottleStack = ItemStack.EMPTY;

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
                flaskSlot = i;
                continue;
            }

            if (stack.is(Items.GLASS_BOTTLE)) {
                if (!bottleStack.isEmpty()) {
                    return null;
                }

                bottleStack = stack;
                continue;
            }

            return null;
        }

        if (flaskStack.isEmpty() || bottleStack.isEmpty()) {
            return null;
        }

        return SpellcastersFlask.canExtractOneDose(flaskStack) ? new Match(flaskStack, flaskSlot) : null;
    }

    private record Match(ItemStack flaskStack, int flaskSlot) {
    }
}
