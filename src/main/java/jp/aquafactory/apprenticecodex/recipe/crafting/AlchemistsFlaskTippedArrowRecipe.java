package jp.aquafactory.apprenticecodex.recipe.crafting;

import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
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

public final class AlchemistsFlaskTippedArrowRecipe extends CustomRecipe {
    private static final int REQUIRED_ARROW_COUNT = 8;

    public AlchemistsFlaskTippedArrowRecipe(CraftingBookCategory category) {
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

        var storedItem = AbstractPotionFlaskItem.getStoredItem(match.flaskStack());
        var result = new ItemStack(Items.TIPPED_ARROW, REQUIRED_ARROW_COUNT);
        var potionContents = storedItem.get(DataComponents.POTION_CONTENTS);
        if (potionContents != null) {
            result.set(DataComponents.POTION_CONTENTS, potionContents);
        }
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        var match = findMatch(input);
        if (match == null) {
            return remaining;
        }

        remaining.set(match.flaskSlot(), AbstractPotionFlaskItem.copyAfterExtractingOneDose(match.flaskStack()));
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 9;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.ALCHEMISTS_FLASK_TIPPED_ARROW_SERIALIZER.get();
    }

    public static boolean matchesInput(CraftingInput input) {
        return findMatch(input) != null;
    }

    private static Match findMatch(CraftingInput input) {
        if (input.size() < 9) {
            return null;
        }

        ItemStack flaskStack = ItemStack.EMPTY;
        int flaskSlot = -1;
        int arrowCount = 0;

        for (int i = 0; i < input.size(); ++i) {
            var stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(ItemRegistry.ALCHEMISTS_FLASK.get())) {
                if (!flaskStack.isEmpty()) {
                    return null;
                }

                flaskStack = stack;
                flaskSlot = i;
                continue;
            }

            if (!stack.is(Items.ARROW)) {
                return null;
            }

            arrowCount++;
        }

        if (flaskStack.isEmpty() || arrowCount != REQUIRED_ARROW_COUNT) {
            return null;
        }

        var storedItem = AbstractPotionFlaskItem.getStoredItem(flaskStack);
        if (storedItem.isEmpty()) {
            return null;
        }

        if (!AbstractPotionFlaskItem.canExtractOneDose(flaskStack)) {
            return null;
        }

        if (!storedItem.is(Items.SPLASH_POTION) && !storedItem.is(Items.LINGERING_POTION)) {
            return null;
        }

        return new Match(flaskStack, flaskSlot);
    }

    private record Match(ItemStack flaskStack, int flaskSlot) {
    }
}
