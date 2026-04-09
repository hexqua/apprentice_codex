package jp.aquafactory.apprenticecodex.recipe.grindrunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public final class GrindRunnerRecipeSerializer implements RecipeSerializer<GrindRunnerRecipe> {
    private static final String ALLOW_UNSTACKABLE_AND_TAGGED_INPUT = "allow_unstackable_and_tagged_input";
    private static boolean hasLoggedObsoleteInputFlagWarning = false;

    @Override
    public @NotNull GrindRunnerRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
        var ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
        if (json.has(ALLOW_UNSTACKABLE_AND_TAGGED_INPUT)) {
            GsonHelper.getAsBoolean(json, ALLOW_UNSTACKABLE_AND_TAGGED_INPUT, false);
            logObsoleteInputFlagWarningOnce();
        }
        var results = readResultsFromJson(json);
        return new GrindRunnerRecipe(recipeId, ingredient, results);
    }

    @Override
    public @Nullable GrindRunnerRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
        var ingredient = Ingredient.fromNetwork(buffer);
        var resultCount = buffer.readVarInt();
        var results = new ArrayList<ItemStack>(resultCount);
        for (var i = 0; i < resultCount; i++) {
            results.add(buffer.readItem());
        }
        return new GrindRunnerRecipe(recipeId, ingredient, results);
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull GrindRunnerRecipe recipe) {
        recipe.getIngredient().toNetwork(buffer);
        var results = recipe.getResultTemplates();
        buffer.writeVarInt(results.size());
        for (var result : results) {
            buffer.writeItem(result);
        }
    }

    private static ArrayList<ItemStack> readResultsFromJson(JsonObject json) {
        var results = new ArrayList<ItemStack>();
        if (json.has("results")) {
            var array = GsonHelper.getAsJsonArray(json, "results");
            for (var element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                var stack = ShapedRecipe.itemStackFromJson(element.getAsJsonObject());
                if (!stack.isEmpty() && stack.getCount() > 0) {
                    results.add(stack);
                }
            }
        } else if (json.has("result")) {
            var stack = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            if (!stack.isEmpty() && stack.getCount() > 0) {
                results.add(stack);
            }
        }

        if (!results.isEmpty()) {
            return results;
        }
        throw new JsonParseException("GrindRunner recipe must have at least one output in result/results.");
    }

    private static void logObsoleteInputFlagWarningOnce() {
        if (hasLoggedObsoleteInputFlagWarning) {
            return;
        }

        hasLoggedObsoleteInputFlagWarning = true;
        ApprenticeCodex.LOGGER.warn(
                "GrindRunner recipe field '{}' is obsolete and ignored. Remove it from datapacks when convenient.",
                ALLOW_UNSTACKABLE_AND_TAGGED_INPUT
        );
    }
}
