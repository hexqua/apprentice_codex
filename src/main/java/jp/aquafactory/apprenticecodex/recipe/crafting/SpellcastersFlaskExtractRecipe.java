package jp.aquafactory.apprenticecodex.recipe.crafting;

import jp.aquafactory.apprenticecodex.item.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class SpellcastersFlaskExtractRecipe extends CustomRecipe {
    public SpellcastersFlaskExtractRecipe(CraftingBookCategory category) {
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

        return SpellcastersFlask.copyStoredItemForCrafting(match.flaskStack());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        var match = findMatch(input);
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

    private static Match findMatch(CraftingInput input) {
        ItemStack flaskStack = ItemStack.EMPTY;
        int flaskSlot = -1;
        ItemStack bottleStack = ItemStack.EMPTY;

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
