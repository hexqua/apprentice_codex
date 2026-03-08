package jp.aquafactory.apprenticecodex.recipe.essencesmoker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.NotNull;

public final class EssenceSmokerRecipeSerializer implements RecipeSerializer<EssenceSmokerRecipe> {
    @Override
    public @NotNull EssenceSmokerRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
        var catalyst = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "catalyst"));
        var material = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "material"));
        var result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
        if (result.isEmpty() || result.getCount() <= 0) {
            throw new JsonParseException("EssenceSmoker recipe result must not be empty.");
        }
        return new EssenceSmokerRecipe(recipeId, catalyst, material, result);
    }

    @Override
    public @NotNull EssenceSmokerRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
        var catalyst = Ingredient.fromNetwork(buffer);
        var material = Ingredient.fromNetwork(buffer);
        var result = buffer.readItem();
        return new EssenceSmokerRecipe(recipeId, catalyst, material, result);
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull EssenceSmokerRecipe recipe) {
        recipe.getCatalyst().toNetwork(buffer);
        recipe.getMaterial().toNetwork(buffer);
        buffer.writeItem(recipe.getResultTemplate());
    }
}
