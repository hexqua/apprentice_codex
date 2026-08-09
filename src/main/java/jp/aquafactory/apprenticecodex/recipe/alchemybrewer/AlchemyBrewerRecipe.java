package jp.aquafactory.apprenticecodex.recipe.alchemybrewer;

import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record AlchemyBrewerRecipe(
        Ingredient base,
        Ingredient ingredient,
        ResourceLocation result,
        int fluidAmountMb,
        int processingTimeTicks,
        int priority
) implements Recipe<AlchemyBrewerRecipeInput> {
    public AlchemyBrewerRecipe {
        if (fluidAmountMb < 250 || fluidAmountMb > 1000 || fluidAmountMb % 250 != 0) {
            throw new IllegalArgumentException("AlchemyBrewer fluid amount must be 250..1000 mB in 250 mB units.");
        }
        if (processingTimeTicks <= 0) {
            throw new IllegalArgumentException("AlchemyBrewer processing time must be positive.");
        }
    }

    @Override public boolean matches(@NotNull AlchemyBrewerRecipeInput input, @NotNull Level level) { return false; }
    @Override public @NotNull ItemStack assemble(@NotNull AlchemyBrewerRecipeInput input, HolderLookup.@NotNull Provider registries) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) { return ItemStack.EMPTY; }
    @Override public @NotNull RecipeSerializer<?> getSerializer() { return RecipeRegistry.ALCHEMY_BREWER_SERIALIZER.get(); }
    @Override public @NotNull RecipeType<?> getType() { return RecipeRegistry.ALCHEMY_BREWER_RECIPE_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
    @Override public @NotNull NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, base, ingredient); }
}
