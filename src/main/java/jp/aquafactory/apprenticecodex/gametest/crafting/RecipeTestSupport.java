package jp.aquafactory.apprenticecodex.gametest.crafting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class RecipeTestSupport {
    private RecipeTestSupport() {}

    static void assertRecipeLoaded(
            GameTestHelper helper,
            RecipeManager recipeManager,
            ResourceLocation recipeId,
            RecipeSerializer<?> expectedSerializer,
            RecipeType<?> expectedType
    ) {
        var recipeHolder = recipeManager.byKey(recipeId).orElse(null);
        helper.assertTrue(recipeHolder != null, "Missing recipe: " + recipeId);

        var recipe = recipeHolder.value();
        helper.assertTrue(recipe.getSerializer() == expectedSerializer,
                "Recipe serializer mismatch for " + recipeId + ": " + BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer()));
        if (expectedType != null) {
            helper.assertTrue(recipe.getType() == expectedType,
                    "Recipe type mismatch for " + recipeId + ": " + BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()));
        }
    }

    static void assertRecipeLoadedWithSerializerId(
            GameTestHelper helper,
            RecipeManager recipeManager,
            ResourceLocation recipeId,
            ResourceLocation expectedSerializerId
    ) {
        var recipeHolder = recipeManager.byKey(recipeId).orElse(null);
        helper.assertTrue(recipeHolder != null, "Missing recipe: " + recipeId);
        var recipe = recipeHolder.value();
        var actualSerializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
        helper.assertTrue(expectedSerializerId.equals(actualSerializerId),
                "Recipe serializer mismatch for " + recipeId + ": expected " + expectedSerializerId + " but got " + actualSerializerId);
    }

    static void assertRecipeAcceptsIngredientItem(
            GameTestHelper helper,
            RecipeManager recipeManager,
            ResourceLocation recipeId,
            Item expectedItem
    ) {
        var recipeHolder = recipeManager.byKey(recipeId).orElse(null);
        helper.assertTrue(recipeHolder != null, "Missing recipe: " + recipeId);
        var expectedStack = new ItemStack(expectedItem);
        var acceptsItem = recipeHolder.value().getIngredients().stream()
                .anyMatch(ingredient -> ingredient.test(expectedStack));
        helper.assertTrue(acceptsItem, "Recipe " + recipeId + " should accept "
                + BuiltInRegistries.ITEM.getKey(expectedItem));
    }

    public static void assertRecipePresent(
            GameTestHelper helper,
            RecipeManager recipeManager,
            ResourceLocation recipeId
    ) {
        helper.assertTrue(recipeManager.byKey(recipeId).isPresent(), "Missing recipe: " + recipeId);
    }
}
