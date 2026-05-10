package jp.aquafactory.apprenticecodex.datagen.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SpellcasterWorkbenchRecipeDataGenerator implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public SpellcasterWorkbenchRecipeDataGenerator(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipes/spellcaster_workbench");
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
        var recipes = List.of(
                recipe(
                        "rapid_spellcaster_round",
                        List.of(
                                ingredient(Items.AMETHYST_SHARD, 1),
                                ingredient(Items.COPPER_INGOT, 1),
                                ingredient(Items.GUNPOWDER, 1)
                        ),
                        List.of(result(ItemRegistry.RAPID_SPELLCASTER_ROUND.get(), 16)),
                        1
                ),
                recipe(
                        "basic_spellcaster_round",
                        List.of(
                                ingredient(Items.AMETHYST_SHARD, 1),
                                ingredient(Items.IRON_INGOT, 1),
                                ingredient(Items.GUNPOWDER, 1)
                        ),
                        List.of(result(ItemRegistry.BASIC_SPELLCASTER_ROUND.get(), 12)),
                        1
                ),
                recipe(
                        "arcane_spellcaster_round",
                        List.of(
                                ingredient(Items.AMETHYST_SHARD, 1),
                                ingredient(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get(), 1),
                                ingredient(Items.GUNPOWDER, 1)
                        ),
                        List.of(result(ItemRegistry.ARCANE_SPELLCASTER_ROUND.get(), 10)),
                        1
                ),
                recipe(
                        "rapid_spellcaster_round_recycle",
                        List.of(
                                ingredient(Items.AMETHYST_SHARD, 1),
                                ingredient(ItemRegistry.EMPTY_RAPID_SPELLCASTER_CASING.get(), 16),
                                ingredient(Items.GUNPOWDER, 1)
                        ),
                        List.of(result(ItemRegistry.RAPID_SPELLCASTER_ROUND.get(), 16)),
                        0
                ),
                recipe(
                        "basic_spellcaster_round_recycle",
                        List.of(
                                ingredient(Items.AMETHYST_SHARD, 1),
                                ingredient(ItemRegistry.EMPTY_BASIC_SPELLCASTER_CASING.get(), 12),
                                ingredient(Items.GUNPOWDER, 1)
                        ),
                        List.of(result(ItemRegistry.BASIC_SPELLCASTER_ROUND.get(), 12)),
                        0
                ),
                recipe(
                        "arcane_spellcaster_round_recycle",
                        List.of(
                                ingredient(Items.AMETHYST_SHARD, 1),
                                ingredient(ItemRegistry.EMPTY_ARCANE_SPELLCASTER_CASING.get(), 10),
                                ingredient(Items.GUNPOWDER, 1)
                        ),
                        List.of(result(ItemRegistry.ARCANE_SPELLCASTER_ROUND.get(), 10)),
                        0
                ),
                recipe(
                        "advanced_spellcaster_round",
                        List.of(
                                ingredient(Items.AMETHYST_SHARD, 1),
                                ingredient(Items.GOLD_INGOT, 1),
                                ingredient(Items.GUNPOWDER, 1)
                        ),
                        List.of(result(ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get(), 12)),
                        1
                ),
                recipe(
                        "spell_dominator_round",
                        List.of(
                                ingredient(Items.AMETHYST_SHARD, 1),
                                ingredient(ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get(), 1),
                                ingredient(Items.GUNPOWDER, 1)
                        ),
                        List.of(result(ItemRegistry.SPELL_DOMINATOR_ROUND.get(), 10)),
                        1
                ),
                recipe(
                        "multi_purpose_spell_round",
                        List.of(
                                ingredient(Items.AMETHYST_SHARD, 1),
                                ingredient(Items.NETHERITE_INGOT, 1),
                                ingredient(Items.GUNPOWDER, 1)
                        ),
                        List.of(result(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get(), 4)),
                        1
                ),
                recipe(
                        "advanced_spellcaster_round_recycle",
                        List.of(
                                ingredient(Items.AMETHYST_SHARD, 1),
                                ingredient(ItemRegistry.EMPTY_ADVANCED_SPELLCASTER_CASING.get(), 12),
                                ingredient(Items.GUNPOWDER, 1)
                        ),
                        List.of(result(ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get(), 12)),
                        0
                ),
                recipe(
                        "spell_dominator_round_recycle",
                        List.of(
                                ingredient(Items.AMETHYST_SHARD, 1),
                                ingredient(ItemRegistry.EMPTY_SPELL_DOMINATOR_CASING.get(), 10),
                                ingredient(Items.GUNPOWDER, 1)
                        ),
                        List.of(result(ItemRegistry.SPELL_DOMINATOR_ROUND.get(), 10)),
                        0
                ),
                recipe(
                        "multi_purpose_spell_round_recycle",
                        List.of(
                                ingredient(Items.AMETHYST_SHARD, 1),
                                ingredient(ItemRegistry.EMPTY_MULTI_PURPOSE_SPELL_CASING.get(), 4),
                                ingredient(Items.GUNPOWDER, 1)
                        ),
                        List.of(result(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get(), 4)),
                        0
                )
        );

        return CompletableFuture.allOf(recipes.stream()
                .map(recipe -> saveRecipe(cachedOutput, recipe))
                .toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> saveRecipe(CachedOutput cachedOutput, RecipeDefinition recipe) {
        return DataProvider.saveStable(
                cachedOutput,
                createRecipeJson(recipe),
                pathProvider.json(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, recipe.path()))
        );
    }

    private static JsonObject createRecipeJson(RecipeDefinition recipe) {
        if (recipe.ingredients().size() != 3) {
            throw new IllegalArgumentException("SpellcasterWorkbench recipe requires exactly 3 ingredients.");
        }
        if (recipe.results().isEmpty()) {
            throw new IllegalArgumentException("SpellcasterWorkbench recipe requires at least one result.");
        }

        var json = new JsonObject();
        json.addProperty("type", ApprenticeCodex.MODID + ":spellcaster_workbench");
        json.addProperty("priority", recipe.priority());

        var ingredients = new JsonArray();
        for (var ingredient : recipe.ingredients()) {
            var ingredientJson = new JsonObject();
            ingredientJson.add("ingredient", ingredient.itemIngredient());
            ingredientJson.addProperty("count", Math.max(1, ingredient.count()));
            ingredients.add(ingredientJson);
        }
        json.add("ingredients", ingredients);

        var results = new JsonArray();
        for (var result : recipe.results()) {
            results.add(serializeResult(result.item(), result.count()));
        }
        json.add("results", results);
        return json;
    }

    private static JsonObject serializeResult(ItemLike item, int count) {
        var itemId = ForgeRegistries.ITEMS.getKey(item.asItem());
        if (itemId == null) {
            throw new IllegalStateException("Unregistered item in SpellcasterWorkbench datagen result: " + item.asItem());
        }

        var json = new JsonObject();
        json.addProperty("item", itemId.toString());
        json.addProperty("count", Math.max(1, count));
        return json;
    }

    private static RecipeDefinition recipe(
            String path,
            List<RecipeIngredient> ingredients,
            List<RecipeResult> results,
            int priority
    ) {
        return new RecipeDefinition(path, ingredients, results, priority);
    }

    private static RecipeIngredient ingredient(ItemLike item, int count) {
        return new RecipeIngredient(itemId(item), count);
    }

    private static RecipeResult result(ItemLike item, int count) {
        return new RecipeResult(item, count);
    }

    private static JsonObject itemId(ItemLike item) {
        var itemId = ForgeRegistries.ITEMS.getKey(item.asItem());
        if (itemId == null) {
            throw new IllegalStateException("Unregistered ingredient item in SpellcasterWorkbench datagen: " + item.asItem());
        }

        var json = new JsonObject();
        json.addProperty("item", itemId.toString());
        return json;
    }

    @Override
    public @NotNull String getName() {
        return "ApprenticeCodex SpellcasterWorkbench Recipes";
    }

    private record RecipeDefinition(
            String path,
            List<RecipeIngredient> ingredients,
            List<RecipeResult> results,
            int priority
    ) {
    }

    private record RecipeIngredient(
            JsonObject itemIngredient,
            int count
    ) {
    }

    private record RecipeResult(
            ItemLike item,
            int count
    ) {
    }
}
