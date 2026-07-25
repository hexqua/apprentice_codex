package jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevice;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDeviceUpgrade;
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
    private final @Nullable LuminousDeviceUpgrade luminousDeviceUpgrade;
    private final @Nullable ResourceLocation requiredSpell;
    private final int minimumSpellLevel;

    public SpellcasterWorkbenchRecipe(
            ResourceLocation id,
            List<SizedIngredient> ingredients,
            List<ItemStack> results,
            int priority
    ) {
        this(id, ingredients, results, priority, null, null, 1);
    }

    public SpellcasterWorkbenchRecipe(
            ResourceLocation id,
            List<SizedIngredient> ingredients,
            List<ItemStack> results,
            int priority,
            @Nullable LuminousDeviceUpgrade luminousDeviceUpgrade,
            @Nullable ResourceLocation requiredSpell,
            int minimumSpellLevel
    ) {
        this.id = id;
        this.ingredients = sanitizeIngredients(ingredients);
        this.results = sanitizeResults(results);
        this.priority = priority;
        this.luminousDeviceUpgrade = luminousDeviceUpgrade;
        this.requiredSpell = requiredSpell;
        this.minimumSpellLevel = Math.max(1, minimumSpellLevel);
    }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        var matchedSlots = findMatchingSlots(container);
        return matchedSlots != null && canApplyOperation(container, matchedSlots);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess registryAccess) {
        var matchedSlots = findMatchingSlots(container);
        if (matchedSlots == null || !canApplyOperation(container, matchedSlots)) {
            return ItemStack.EMPTY;
        }
        if (luminousDeviceUpgrade == null) {
            return getPrimaryResultTemplate();
        }
        var deviceStack = findLuminousDevice(container, matchedSlots);
        return LuminousDevice.createUpgradeResult(deviceStack, luminousDeviceUpgrade);
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
        var result = results.get(0).copy();
        if (luminousDeviceUpgrade != null) {
            LuminousDevice.addUpgrade(result, luminousDeviceUpgrade);
        }
        return result;
    }

    public @NotNull List<ItemStack> getResultTemplates() {
        var copies = new ArrayList<ItemStack>(results.size());
        for (var index = 0; index < results.size(); ++index) {
            copies.add(index == 0 ? getPrimaryResultTemplate() : results.get(index).copy());
        }
        return copies;
    }

    public int getPriority() {
        return priority;
    }

    public @Nullable LuminousDeviceUpgrade getLuminousDeviceUpgrade() {
        return luminousDeviceUpgrade;
    }

    public @Nullable ResourceLocation getRequiredSpell() {
        return requiredSpell;
    }

    public int getMinimumSpellLevel() {
        return minimumSpellLevel;
    }

    public boolean matchesIngredientItem(int ingredientIndex, ItemStack stack) {
        if (ingredientIndex < 0
                || ingredientIndex >= ingredients.size()
                || !ingredients.get(ingredientIndex).ingredient().test(stack)) {
            return false;
        }
        if (luminousDeviceUpgrade == null) {
            return true;
        }
        if (stack.getItem() instanceof LuminousDevice) {
            return !LuminousDevice.hasUpgrade(stack, luminousDeviceUpgrade);
        }
        if (requiredSpell != null
                && stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get())) {
            return isRequiredSpellScroll(stack);
        }
        return true;
    }

    public int[] findMatchingSlots(@NotNull Container container) {
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

    private boolean canApplyOperation(Container container, int[] matchedSlots) {
        if (luminousDeviceUpgrade == null) {
            return true;
        }
        var deviceStack = findLuminousDevice(container, matchedSlots);
        if (deviceStack.isEmpty() || LuminousDevice.hasUpgrade(deviceStack, luminousDeviceUpgrade)) {
            return false;
        }
        if (requiredSpell == null) {
            return true;
        }
        for (var slot : matchedSlots) {
            var stack = container.getItem(slot);
            if (!stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get())
                    || !ISpellContainer.isSpellContainer(stack)) {
                continue;
            }
            if (isRequiredSpellScroll(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRequiredSpellScroll(ItemStack stack) {
        if (requiredSpell == null
                || !stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get())
                || !ISpellContainer.isSpellContainer(stack)) {
            return false;
        }
        var spellContainer = ISpellContainer.get(stack);
        var spellData = spellContainer == null ? SpellData.EMPTY : spellContainer.getSpellAtIndex(0);
        return spellData != null
                && spellData != SpellData.EMPTY
                && spellData.getSpell() != null
                && requiredSpell.equals(spellData.getSpell().getSpellResource())
                && spellData.getLevel() >= minimumSpellLevel;
    }

    private static ItemStack findLuminousDevice(Container container, int[] matchedSlots) {
        for (var slot : matchedSlots) {
            var stack = container.getItem(slot);
            if (stack.getItem() instanceof LuminousDevice) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
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
