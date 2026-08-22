package jp.aquafactory.apprenticecodex.recipe.alchemybrewer;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AlchemyBrewerModifierRecipeSerializer implements RecipeSerializer<AlchemyBrewerModifierRecipe> {
    @Override public @NotNull AlchemyBrewerModifierRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
        return new AlchemyBrewerModifierRecipe(id,
                readResourceLocation(json, "input"),
                Ingredient.fromJson(json.get("ingredient")),
                readResourceLocation(json, "result"),
                GsonHelper.getAsInt(json, "priority", 0));
    }

    @Override public @Nullable AlchemyBrewerModifierRecipe fromNetwork(@NotNull ResourceLocation id, @NotNull FriendlyByteBuf buffer) {
        return new AlchemyBrewerModifierRecipe(id, buffer.readResourceLocation(), Ingredient.fromNetwork(buffer),
                buffer.readResourceLocation(), buffer.readInt());
    }

    @Override public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull AlchemyBrewerModifierRecipe recipe) {
        buffer.writeResourceLocation(recipe.input());
        recipe.ingredient().toNetwork(buffer);
        buffer.writeResourceLocation(recipe.result());
        buffer.writeInt(recipe.priority());
    }

    private static ResourceLocation readResourceLocation(JsonObject json, String field) {
        var value = GsonHelper.getAsString(json, field);
        var id = ResourceLocation.tryParse(value);
        if (id == null) throw new com.google.gson.JsonParseException("Invalid resource location in " + field + ": " + value);
        return id;
    }
}
