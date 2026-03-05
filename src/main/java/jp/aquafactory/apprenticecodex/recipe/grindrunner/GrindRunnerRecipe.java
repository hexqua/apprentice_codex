package jp.aquafactory.apprenticecodex.recipe.grindrunner;

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

import java.util.ArrayList;
import java.util.List;

public final class GrindRunnerRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final List<ItemStack> results;
    private final boolean allowUnstackableAndTaggedInput;

    public GrindRunnerRecipe(
            ResourceLocation id,
            Ingredient ingredient,
            List<ItemStack> results,
            boolean allowUnstackableAndTaggedInput
    ) {
        this.id = id;
        this.ingredient = ingredient;
        this.results = sanitizeResults(results);
        this.allowUnstackableAndTaggedInput = allowUnstackableAndTaggedInput;
    }

    @Override
    public boolean matches(Container input, Level level) {
        if (input.getContainerSize() <= 0) {
            return false;
        }
        return ingredient.test(input.getItem(0));
    }

    @Override
    public @NotNull ItemStack assemble(Container input, RegistryAccess registryAccess) {
        return getResultTemplate();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(RegistryAccess registryAccess) {
        return getResultTemplate();
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.GRIND_RUNNER_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get();
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        var ingredients = NonNullList.withSize(1, Ingredient.EMPTY);
        ingredients.set(0, ingredient);
        return ingredients;
    }

    public @NotNull ItemStack getResultTemplate() {
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

    public @NotNull Ingredient getIngredient() {
        return ingredient;
    }

    public boolean allowsUnstackableAndTaggedInput() {
        return allowUnstackableAndTaggedInput;
    }

    private static List<ItemStack> sanitizeResults(List<ItemStack> results) {
        var sanitized = new ArrayList<ItemStack>();
        for (var result : results) {
            if (result.isEmpty() || result.getCount() <= 0) {
                continue;
            }
            sanitized.add(result.copy());
        }
        return List.copyOf(sanitized);
    }
}
