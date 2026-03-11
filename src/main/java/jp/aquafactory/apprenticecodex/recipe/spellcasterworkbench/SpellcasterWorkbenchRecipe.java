package jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench;

import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SpellcasterWorkbenchRecipe implements Recipe<Container> {
    public static final int INPUT_SLOT_COUNT = 3;

    private final ResourceLocation id;
    private final List<SizedIngredient> ingredients;
    private final List<ItemStack> results;
    private final int priority;

    public SpellcasterWorkbenchRecipe(
            ResourceLocation id,
            List<SizedIngredient> ingredients,
            List<ItemStack> results,
            int priority
    ) {
        this.id = id;
        this.ingredients = sanitizeIngredients(ingredients);
        this.results = sanitizeResults(results);
        this.priority = priority;
    }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        return findMatchingSlots(container) != null;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess registryAccess) {
        return getPrimaryResultTemplate();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return getPrimaryResultTemplate();
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.SPELLCASTER_WORKBENCH_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        // SpellcasterWorkbench 専用の加工レシピで、通常レシピブックには載せない。
        return true;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        var vanillaIngredients = NonNullList.withSize(INPUT_SLOT_COUNT, Ingredient.EMPTY);
        for (var index = 0; index < Math.min(ingredients.size(), INPUT_SLOT_COUNT); ++index) {
            vanillaIngredients.set(index, ingredients.get(index).ingredient());
        }
        return vanillaIngredients;
    }

    public @NotNull List<SizedIngredient> getSizedIngredients() {
        return ingredients;
    }

    public @NotNull ItemStack getPrimaryResultTemplate() {
        if (results.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return results.get(0).copy();
    }

    public @NotNull List<ItemStack> getResultTemplates() {
        var copies = new ArrayList<ItemStack>(results.size());
        for (var result : results) {
            copies.add(result.copy());
        }
        return copies;
    }

    public int getPriority() {
        return priority;
    }

    public @Nullable int[] findMatchingSlots(@NotNull Container container) {
        if (container.getContainerSize() < INPUT_SLOT_COUNT || ingredients.size() != INPUT_SLOT_COUNT) {
            return null;
        }

        var usedSlots = new boolean[INPUT_SLOT_COUNT];
        var matchedSlots = new int[INPUT_SLOT_COUNT];
        Arrays.fill(matchedSlots, -1);
        if (!matchesUnordered(container, 0, usedSlots, matchedSlots)) {
            return null;
        }
        return matchedSlots;
    }

    private boolean matchesUnordered(Container container, int ingredientIndex, boolean[] usedSlots, int[] matchedSlots) {
        if (ingredientIndex >= ingredients.size()) {
            return true;
        }

        var ingredient = ingredients.get(ingredientIndex);
        for (var slotIndex = 0; slotIndex < INPUT_SLOT_COUNT; ++slotIndex) {
            var stack = container.getItem(slotIndex);
            if (usedSlots[slotIndex] || stack.isEmpty() || !ingredient.test(stack)) {
                continue;
            }

            usedSlots[slotIndex] = true;
            matchedSlots[ingredientIndex] = slotIndex;
            if (matchesUnordered(container, ingredientIndex + 1, usedSlots, matchedSlots)) {
                return true;
            }
            usedSlots[slotIndex] = false;
            matchedSlots[ingredientIndex] = -1;
        }

        return false;
    }

    private static List<SizedIngredient> sanitizeIngredients(List<SizedIngredient> ingredients) {
        if (ingredients.size() != INPUT_SLOT_COUNT) {
            throw new IllegalArgumentException("SpellcasterWorkbench recipe requires exactly 3 ingredients.");
        }
        return List.copyOf(ingredients);
    }

    private static List<ItemStack> sanitizeResults(List<ItemStack> results) {
        var sanitized = new ArrayList<ItemStack>();
        for (var result : results) {
            if (result.isEmpty() || result.getCount() <= 0) {
                continue;
            }
            sanitized.add(result.copy());
        }

        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("SpellcasterWorkbench recipe requires at least one result.");
        }
        return List.copyOf(sanitized);
    }

    public record SizedIngredient(
            Ingredient ingredient,
            int count
    ) {
        public SizedIngredient {
            count = Math.max(1, count);
        }

        public boolean test(ItemStack stack) {
            return ingredient.test(stack) && stack.getCount() >= count;
        }
    }
}
