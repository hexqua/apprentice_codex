package jp.aquafactory.apprenticecodex.recipe.alchemybrewer;

import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record AlchemyBrewerModifierRecipe(
        ResourceLocation id,
        ResourceLocation input,
        Ingredient ingredient,
        ResourceLocation result,
        int priority
) implements Recipe<Container> {
    @Override public boolean matches(@NotNull Container input, @NotNull Level level) { return false; }
    @Override public @NotNull ItemStack assemble(@NotNull Container input, @NotNull RegistryAccess registries) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registries) { return ItemStack.EMPTY; }
    @Override public @NotNull ResourceLocation getId() { return id; }
    @Override public @NotNull RecipeSerializer<?> getSerializer() { return RecipeRegistry.ALCHEMY_BREWER_MODIFIER_SERIALIZER.get(); }
    @Override public @NotNull RecipeType<?> getType() { return RecipeRegistry.ALCHEMY_BREWER_MODIFIER_RECIPE_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
    @Override public @NotNull NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, ingredient); }
}
