package jp.aquafactory.apprenticecodex.recipe.smithing;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SpellbookCarryoverSmithingRecipeSerializer implements RecipeSerializer<SpellbookCarryoverSmithingRecipe> {
    @Override
    public @NotNull SpellbookCarryoverSmithingRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
        var template = Ingredient.fromJson(GsonHelper.getNonNull(json, "template"));
        var base = Ingredient.fromJson(GsonHelper.getNonNull(json, "base"));
        var addition = Ingredient.fromJson(GsonHelper.getNonNull(json, "addition"));
        var result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
        return new SpellbookCarryoverSmithingRecipe(recipeId, template, base, addition, result);
    }

    @Override
    public @Nullable SpellbookCarryoverSmithingRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
        var template = Ingredient.fromNetwork(buffer);
        var base = Ingredient.fromNetwork(buffer);
        var addition = Ingredient.fromNetwork(buffer);
        var result = buffer.readItem();
        return new SpellbookCarryoverSmithingRecipe(recipeId, template, base, addition, result);
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull SpellbookCarryoverSmithingRecipe recipe) {
        recipe.getTemplate().toNetwork(buffer);
        recipe.getBase().toNetwork(buffer);
        recipe.getAddition().toNetwork(buffer);
        buffer.writeItem(recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY));
    }
}
