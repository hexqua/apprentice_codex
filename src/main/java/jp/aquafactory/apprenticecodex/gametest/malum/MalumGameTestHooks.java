package jp.aquafactory.apprenticecodex.gametest.malum;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Recipe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Optional Malum API is reflected so the standard GameTest profile remains loadable without Malum. */
public final class MalumGameTestHooks {
    private static final String SPIRIT_INFUSION_RECIPE = "com.sammy.malum.common.recipe.SpiritInfusionRecipe";

    private MalumGameTestHooks() {
    }

    public static void assertSpiritInfusionRecipe(
            Level level,
            ResourceLocation recipeId,
            ItemStack input,
            List<ItemStack> extras,
            ItemStack expectedOutput
    ) {
        try {
            var recipeClass = Class.forName(SPIRIT_INFUSION_RECIPE);
            Recipe<?> recipe = level.getRecipeManager().byKey(recipeId)
                    .orElseThrow(() -> new AssertionError("Missing Malum Spirit Altar recipe " + recipeId)).value();
            if (!recipeClass.isInstance(recipe)) {
                throw new AssertionError("Recipe " + recipeId + " is not malum:spirit_infusion");
            }

            Field inputField = recipeClass.getField("input");
            assertIngredientMatches(inputField.get(recipe), input, recipeId, "input");

            Field extrasField = recipeClass.getField("extraItems");
            var recipeExtras = (List<?>) extrasField.get(recipe);
            if (recipeExtras.size() != extras.size()) {
                throw new AssertionError("Malum Spirit Altar recipe " + recipeId + " extra item count mismatch");
            }
            for (ItemStack extra : extras) {
                boolean matched = recipeExtras.stream().anyMatch(ingredient -> ingredientMatches(ingredient, extra));
                if (!matched) {
                    throw new AssertionError("Malum Spirit Altar recipe " + recipeId + " is missing extra " + itemId(extra));
                }
            }

            Field outputField = recipeClass.getField("output");
            var output = (ItemStack) outputField.get(recipe);
            if (!ItemStack.isSameItemSameComponents(output, expectedOutput)) {
                throw new AssertionError("Malum Spirit Altar recipe " + recipeId + " output expected="
                        + itemId(expectedOutput) + ", actual=" + itemId(output));
            }

            Field spiritsField = recipeClass.getField("spirits");
            var spirits = (List<?>) spiritsField.get(recipe);
            assertSpiritCost(spirits, "arcane_spirit", 16, recipeId);
            assertSpiritCost(spirits, "wicked_spirit", 16, recipeId);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Malum Spirit Infusion GameTest hook failed for " + recipeId, exception);
        }
    }

    private static void assertSpiritCost(List<?> spirits, String expectedPath, int expectedCount, ResourceLocation recipeId)
            throws ReflectiveOperationException {
        for (Object spirit : spirits) {
            Method getItem = spirit.getClass().getMethod("getItem");
            Method getCount = spirit.getClass().getMethod("getCount");
            var item = (net.minecraft.world.item.Item) getItem.invoke(spirit);
            int count = (int) getCount.invoke(spirit);
            var itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId != null && expectedPath.equals(itemId.getPath()) && count == expectedCount) {
                return;
            }
        }

        throw new AssertionError("Malum Spirit Altar recipe " + recipeId + " is missing "
                + expectedCount + " " + expectedPath);
    }

    private static void assertIngredientMatches(Object ingredient, ItemStack expected, ResourceLocation recipeId, String name)
            throws ReflectiveOperationException {
        if (!ingredientMatches(ingredient, expected)) {
            throw new AssertionError("Malum Spirit Altar recipe " + recipeId + " has invalid " + name
                    + ", expected=" + itemId(expected));
        }
    }

    private static boolean ingredientMatches(Object ingredient, ItemStack expected) {
        try {
            Method matches = ingredient.getClass().getMethod("matches", ItemStack.class);
            Method getCount = ingredient.getClass().getMethod("getCount");
            return (boolean) matches.invoke(ingredient, expected) && (int) getCount.invoke(ingredient) == expected.getCount();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to inspect Malum recipe ingredient", exception);
        }
    }

    private static String itemId(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? stack.toString() : id.toString();
    }
}
