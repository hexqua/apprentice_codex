package jp.aquafactory.apprenticecodex.gametest.crafting;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

public final class RecipeTestSupport {
    private RecipeTestSupport() {}

    public static final TagKey<Item> SPELLCASTER_WORKBENCH_EXTRACTABLE = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_workbench_extractable")
    );

    static void assertRecipeLoaded(
            GameTestHelper helper,
            RecipeManager recipeManager,
            ResourceLocation recipeId,
            RecipeSerializer<?> expectedSerializer,
            RecipeType<?> expectedType
    ) {
        var recipe = recipeManager.byKey(recipeId).orElse(null);
        helper.assertTrue(recipe != null, "Missing recipe: " + recipeId);
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
        var recipe = recipeManager.byKey(recipeId).orElse(null);
        helper.assertTrue(recipe != null, "Missing recipe: " + recipeId);
        var actualSerializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
        helper.assertTrue(expectedSerializerId.equals(actualSerializerId),
                "Recipe serializer mismatch for " + recipeId + ": expected " + expectedSerializerId + " but got " + actualSerializerId);
    }

    static void assertShapedRecipeCenterIngredientMatches(
            GameTestHelper helper,
            RecipeManager recipeManager,
            ResourceLocation recipeId,
            ItemStack acceptedStack,
            ItemStack rejectedStack,
            String message
    ) {
        var recipe = recipeManager.byKey(recipeId).orElse(null);
        helper.assertTrue(recipe instanceof ShapedRecipe,
                "Expected shaped recipe for " + recipeId);
        if (!(recipe instanceof ShapedRecipe shapedRecipe)) {
            return;
        }

        var ingredients = shapedRecipe.getIngredients();
        helper.assertTrue(ingredients.size() > 4, "Recipe center ingredient missing for " + recipeId);
        if (ingredients.size() <= 4) {
            return;
        }

        var centerIngredient = ingredients.get(4);
        helper.assertTrue(centerIngredient.test(acceptedStack),
                message + ": accepted stack did not match");
        helper.assertFalse(centerIngredient.test(rejectedStack),
                message + ": rejected stack unexpectedly matched");
    }

    public static CraftingContainer createCraftingContainer(ItemStack... stacks) {
        var menu = new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return false;
            }
        };

        var container = new TransientCraftingContainer(menu, 3, 3);
        for (int i = 0; i < stacks.length; ++i) {
            container.setItem(i, stacks[i]);
        }
        return container;
    }
}
