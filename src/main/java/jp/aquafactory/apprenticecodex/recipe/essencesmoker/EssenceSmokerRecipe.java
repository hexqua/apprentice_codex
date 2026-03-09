package jp.aquafactory.apprenticecodex.recipe.essencesmoker;

import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class EssenceSmokerRecipe implements Recipe<EssenceSmokerRecipeInput> {
    private final Ingredient catalyst;
    private final Ingredient material;
    private final ItemStack result;

    public EssenceSmokerRecipe(Ingredient catalyst, Ingredient material, ItemStack result) {
        this.catalyst = catalyst;
        this.material = material;
        this.result = sanitizeResult(result);
    }

    @Override
    public boolean matches(EssenceSmokerRecipeInput input, @NotNull Level level) {
        return catalyst.test(input.catalyst()) && material.test(input.material());
    }

    public boolean matches(ItemStack catalystStack, ItemStack materialStack) {
        return catalyst.test(catalystStack) && material.test(materialStack);
    }

    @Override
    public @NotNull ItemStack assemble(EssenceSmokerRecipeInput input, HolderLookup.Provider registries) {
        return getResultTemplate();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return getResultTemplate();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.ESSENCE_SMOKER_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return RecipeRegistry.ESSENCE_SMOKER_RECIPE_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        // Essence Smoker 専用の加工レシピで、通常レシピブックには載せない。
        return true;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        var ingredients = NonNullList.withSize(2, Ingredient.EMPTY);
        ingredients.set(0, catalyst);
        ingredients.set(1, material);
        return ingredients;
    }

    public @NotNull Ingredient getCatalyst() {
        return catalyst;
    }

    public @NotNull Ingredient getMaterial() {
        return material;
    }

    public @NotNull ItemStack getResultTemplate() {
        return result.copy();
    }

    private static ItemStack sanitizeResult(ItemStack result) {
        if (result.isEmpty() || result.getCount() <= 0) {
            throw new IllegalArgumentException("EssenceSmoker recipe requires a non-empty result.");
        }
        return result.copy();
    }
}
