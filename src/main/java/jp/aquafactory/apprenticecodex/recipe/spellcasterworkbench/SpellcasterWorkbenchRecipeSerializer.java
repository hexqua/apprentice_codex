package jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDeviceUpgrade;
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

public final class SpellcasterWorkbenchRecipeSerializer implements RecipeSerializer<SpellcasterWorkbenchRecipe> {
    private static final String INGREDIENTS = "ingredients";
    private static final String RESULTS = "results";
    private static final String PRIORITY = "priority";
    private static final String OPERATION = "operation";

    @Override
    public @NotNull SpellcasterWorkbenchRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
        var ingredients = readIngredientsFromJson(GsonHelper.getAsJsonArray(json, INGREDIENTS));
        var results = readResultsFromJson(json);
        var priority = GsonHelper.getAsInt(json, PRIORITY, 0);
        var operation = readOperation(json);
        return new SpellcasterWorkbenchRecipe(
                recipeId,
                ingredients,
                results,
                priority,
                operation.upgrade(),
                operation.requiredSpell(),
                operation.minimumSpellLevel()
        );
    }

    @Override
    public @Nullable SpellcasterWorkbenchRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
        var ingredientCount = buffer.readVarInt();
        var ingredients = new ArrayList<SpellcasterWorkbenchRecipe.SizedIngredient>(ingredientCount);
        for (var index = 0; index < ingredientCount; ++index) {
            ingredients.add(new SpellcasterWorkbenchRecipe.SizedIngredient(Ingredient.fromNetwork(buffer), buffer.readVarInt()));
        }

        var priority = buffer.readInt();
        var resultCount = buffer.readVarInt();
        var results = new ArrayList<ItemStack>(resultCount);
        for (var index = 0; index < resultCount; ++index) {
            results.add(buffer.readItem());
        }
        var hasOperation = buffer.readBoolean();
        var upgrade = hasOperation ? LuminousDeviceUpgrade.byId(buffer.readResourceLocation()) : null;
        var requiredSpell = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        var minimumSpellLevel = buffer.readVarInt();
        return new SpellcasterWorkbenchRecipe(
                recipeId,
                ingredients,
                results,
                priority,
                upgrade,
                requiredSpell,
                minimumSpellLevel
        );
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull SpellcasterWorkbenchRecipe recipe) {
        var ingredients = recipe.getSizedIngredients();
        buffer.writeVarInt(ingredients.size());
        for (var ingredient : ingredients) {
            ingredient.ingredient().toNetwork(buffer);
            buffer.writeVarInt(ingredient.count());
        }

        buffer.writeInt(recipe.getPriority());

        var results = recipe.getResultTemplates();
        buffer.writeVarInt(results.size());
        for (var result : results) {
            buffer.writeItem(result);
        }
        var upgrade = recipe.getLuminousDeviceUpgrade();
        buffer.writeBoolean(upgrade != null);
        if (upgrade != null) {
            buffer.writeResourceLocation(upgrade.id());
        }
        buffer.writeBoolean(recipe.getRequiredSpell() != null);
        if (recipe.getRequiredSpell() != null) {
            buffer.writeResourceLocation(recipe.getRequiredSpell());
        }
        buffer.writeVarInt(recipe.getMinimumSpellLevel());
    }

    private static ArrayList<SpellcasterWorkbenchRecipe.SizedIngredient> readIngredientsFromJson(JsonArray ingredientsArray) {
        if (ingredientsArray.size() != SpellcasterWorkbenchRecipe.INPUT_SLOT_COUNT) {
            throw new JsonParseException("SpellcasterWorkbench recipe must have exactly 3 ingredients.");
        }

        var ingredients = new ArrayList<SpellcasterWorkbenchRecipe.SizedIngredient>(ingredientsArray.size());
        for (var element : ingredientsArray) {
            if (!element.isJsonObject()) {
                throw new JsonParseException("SpellcasterWorkbench ingredient entry must be an object.");
            }

            var ingredientObject = element.getAsJsonObject();
            if (!ingredientObject.has("ingredient")) {
                throw new JsonParseException("SpellcasterWorkbench ingredient entry requires ingredient.");
            }

            ingredients.add(new SpellcasterWorkbenchRecipe.SizedIngredient(
                    Ingredient.fromJson(ingredientObject.get("ingredient")),
                    GsonHelper.getAsInt(ingredientObject, "count", 1)
            ));
        }
        return ingredients;
    }

    private static ArrayList<ItemStack> readResultsFromJson(JsonObject json) {
        var results = new ArrayList<ItemStack>();
        if (json.has(RESULTS)) {
            var array = GsonHelper.getAsJsonArray(json, RESULTS);
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
        throw new JsonParseException("SpellcasterWorkbench recipe must have at least one output in result/results.");
    }

    private static Operation readOperation(JsonObject json) {
        if (!json.has(OPERATION)) {
            return new Operation(null, null, 1);
        }
        var operation = GsonHelper.getAsJsonObject(json, OPERATION);
        var type = GsonHelper.getAsString(operation, "type");
        if (!"add_luminous_device_upgrade".equals(type)) {
            throw new JsonParseException("Unknown SpellcasterWorkbench operation: " + type);
        }
        var featureId = ResourceLocation.tryParse(GsonHelper.getAsString(operation, "feature"));
        var upgrade = LuminousDeviceUpgrade.byId(featureId);
        if (upgrade == null) {
            throw new JsonParseException("Unknown Luminous Device upgrade: " + featureId);
        }
        var requiredSpell = operation.has("required_spell")
                ? ResourceLocation.tryParse(GsonHelper.getAsString(operation, "required_spell"))
                : null;
        if (operation.has("required_spell") && requiredSpell == null) {
            throw new JsonParseException("Invalid required_spell in SpellcasterWorkbench operation.");
        }
        return new Operation(
                upgrade,
                requiredSpell,
                GsonHelper.getAsInt(operation, "minimum_spell_level", 1)
        );
    }

    private record Operation(
            @Nullable LuminousDeviceUpgrade upgrade,
            @Nullable ResourceLocation requiredSpell,
            int minimumSpellLevel
    ) {
    }
}
