package jp.aquafactory.apprenticecodex.recipe.alchemybrewer;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AlchemyBrewerRecipeSerializer implements RecipeSerializer<AlchemyBrewerRecipe> {
    @Override public @NotNull AlchemyBrewerRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
        return new AlchemyBrewerRecipe(id,
                Ingredient.fromJson(json.get("base")),
                Ingredient.fromJson(json.get("ingredient")),
                readResourceLocation(json, "result"),
                GsonHelper.getAsInt(json, "fluid_amount_mb"),
                GsonHelper.getAsInt(json, "processing_time_ticks"),
                GsonHelper.getAsInt(json, "priority", 0));
    }

    @Override public @Nullable AlchemyBrewerRecipe fromNetwork(@NotNull ResourceLocation id, @NotNull FriendlyByteBuf buffer) {
        return new AlchemyBrewerRecipe(id, Ingredient.fromNetwork(buffer), Ingredient.fromNetwork(buffer),
                buffer.readResourceLocation(), buffer.readVarInt(), buffer.readVarInt(), buffer.readInt());
    }

    @Override public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull AlchemyBrewerRecipe recipe) {
        recipe.base().toNetwork(buffer);
        recipe.ingredient().toNetwork(buffer);
        buffer.writeResourceLocation(recipe.result());
        buffer.writeVarInt(recipe.fluidAmountMb());
        buffer.writeVarInt(recipe.processingTimeTicks());
        buffer.writeInt(recipe.priority());
    }

    private static ResourceLocation readResourceLocation(JsonObject json, String field) {
        var value = GsonHelper.getAsString(json, field);
        var id = ResourceLocation.tryParse(value);
        if (id == null) throw new com.google.gson.JsonParseException("Invalid resource location in " + field + ": " + value);
        return id;
    }
}
