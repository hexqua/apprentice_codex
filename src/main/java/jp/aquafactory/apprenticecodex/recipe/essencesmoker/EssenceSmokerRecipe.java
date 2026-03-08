package jp.aquafactory.apprenticecodex.recipe.essencesmoker;

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

public final class EssenceSmokerRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient catalyst;
    private final Ingredient material;
    private final ItemStack result;

    public EssenceSmokerRecipe(ResourceLocation id, Ingredient catalyst, Ingredient material, ItemStack result) {
        this.id = id;
        this.catalyst = catalyst;
        this.material = material;
        this.result = sanitizeResult(result);
    }

    @Override
    public boolean matches(Container container, @NotNull Level level) {
        if (container.getContainerSize() < 2) {
            return false;
        }

        return catalyst.test(container.getItem(0)) && material.test(container.getItem(1));
    }

    public boolean matches(ItemStack catalystStack, ItemStack materialStack) {
        return catalyst.test(catalystStack) && material.test(materialStack);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess registryAccess) {
        return getResultTemplate();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return getResultTemplate();
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
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
            return ItemStack.EMPTY;
        }
        return result.copy();
    }
}
