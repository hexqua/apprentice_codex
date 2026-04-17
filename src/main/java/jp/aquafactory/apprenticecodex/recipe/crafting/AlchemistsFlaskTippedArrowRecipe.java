package jp.aquafactory.apprenticecodex.recipe.crafting;

import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class AlchemistsFlaskTippedArrowRecipe extends CustomRecipe {
    private static final int REQUIRED_ARROW_COUNT = 8;

    public AlchemistsFlaskTippedArrowRecipe(ResourceLocation recipeId, CraftingBookCategory category) {
        super(recipeId, category);
    }

    @Override
    public boolean matches(@NotNull CraftingContainer container, @NotNull Level level) {
        return findMatch(container) != null;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingContainer container, @NotNull RegistryAccess registryAccess) {
        var match = findMatch(container);
        if (match == null) {
            return ItemStack.EMPTY;
        }

        var storedItem = AbstractPotionFlaskItem.getStoredItem(match.flaskStack());
        var result = new ItemStack(Items.TIPPED_ARROW, REQUIRED_ARROW_COUNT);
        if (storedItem.hasTag()) {
            if (storedItem.getTag() != null) {
                result.setTag(storedItem.getTag().copy());
            }
        }
        return result;
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        var remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        var match = findMatch(container);
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
    public @NotNull RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.ALCHEMISTS_FLASK_TIPPED_ARROW_SERIALIZER.get();
    }

    public static boolean matchesContainer(Container container) {
        return findMatch(container) != null;
    }

    private static Match findMatch(Container container) {
        if (container.getContainerSize() < 9) {
            return null;
        }

        ItemStack flaskStack = ItemStack.EMPTY;
        int flaskSlot = -1;
        int arrowCount = 0;

        for (int i = 0; i < container.getContainerSize(); ++i) {
            var stack = container.getItem(i);
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
