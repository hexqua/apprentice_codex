package jp.aquafactory.apprenticecodex.datagen.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class GrindRunnerRecipeDataGenerator implements DataProvider {
    private static final String RECIPE_TYPE = "apprenticecodex:grind_runner";
    private static final String ALLOW_UNSTACKABLE_AND_TAGGED_INPUT = "allow_unstackable_and_tagged_input";

    private final PackOutput.PathProvider pathProvider;

    public GrindRunnerRecipeDataGenerator(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe/grind_runner");
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
        var recipes = List.of(
                recipe(Items.AMETHYST_BLOCK, result(Items.AMETHYST_SHARD, 4)),
                recipe(Items.BONE, result(Items.BONE_MEAL, 6)),
                recipe(Items.BLAZE_ROD, result(Items.BLAZE_POWDER, 4)),
                recipe(Items.GLOWSTONE, result(Items.GLOWSTONE_DUST, 4)),
                recipe(Items.NETHER_WART_BLOCK, result(Items.NETHER_WART, 1)),
                recipe(Items.SUGAR_CANE, result(Items.SUGAR, 2)),
                recipe(ItemTags.WOOL, result(Items.STRING, 4)),
                recipe(Items.STONE, result(Items.COBBLESTONE, 1)),
                recipe(Items.COBBLESTONE, result(Items.GRAVEL, 1)),
                recipe(Items.GRAVEL, result(Items.SAND, 1)),
                recipe(Items.BOOK, result(Items.LEATHER, 1), result(Items.PAPER, 3)),

                recipe(ItemRegistry.AFFINITY_RING.get(), true, result(ItemRegistry.MITHRIL_SCRAP.get(), 1)),
                recipe(ItemRegistry.CAST_TIME_RING.get(), true, result(ItemRegistry.MITHRIL_SCRAP.get(), 1)),
                recipe(ItemRegistry.COOLDOWN_RING.get(), true, result(ItemRegistry.MITHRIL_SCRAP.get(), 1)),
                recipe(ItemRegistry.EMERALD_STONEPLATE_RING.get(), true, result(ItemRegistry.MITHRIL_SCRAP.get(), 1)),
                recipe(ItemRegistry.VISIBILITY_RING.get(), true, result(ItemRegistry.MITHRIL_SCRAP.get(), 1)),
                recipe(ItemRegistry.CONCENTRATION_AMULET.get(), true, result(ItemRegistry.MITHRIL_SCRAP.get(), 1)),
                recipe(ItemRegistry.SILVER_RING.get(), true, result(ItemRegistry.MITHRIL_SCRAP.get(), 1)),

                recipe(ItemRegistry.FIREWARD_RING.get(), true, result(ItemRegistry.MITHRIL_SCRAP.get(), 1), result(ItemRegistry.CINDER_ESSENCE.get(), 1)),
                recipe(ItemRegistry.FROSTWARD_RING.get(), true, result(ItemRegistry.MITHRIL_SCRAP.get(), 1), result(ItemRegistry.ICE_CRYSTAL.get(), 1)),
                recipe(ItemRegistry.POISONWARD_RING.get(), true, result(ItemRegistry.MITHRIL_SCRAP.get(), 1), result(Items.POISONOUS_POTATO, 1))
        );

        return CompletableFuture.allOf(recipes.stream()
                .map(recipe -> saveRecipe(cachedOutput, recipe))
                .toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> saveRecipe(
            CachedOutput cachedOutput,
            RecipeDefinition recipe
    ) {
        var results = recipe.results();
        if (results.isEmpty()) {
            throw new IllegalArgumentException("GrindRunner recipe requires at least one result.");
        }

        var id = ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                recipeIdPath(recipe.ingredient(), results.get(0).item())
        );
        return DataProvider.saveStable(
                cachedOutput,
                createRecipeJson(recipe.ingredient(), recipe.allowUnstackableAndTaggedInput(), results),
                pathProvider.json(id)
        );
    }

    private static String recipeIdPath(IngredientDefinition ingredient, ItemLike resultItem) {
        return itemPath(resultItem) + "_from_" + ingredient.recipeIdPath();
    }

    private static String itemPath(ItemLike item) {
        var id = BuiltInRegistries.ITEM.getKey(item.asItem());
        if (id == null) {
            return "unknown";
        }

        if ("minecraft".equals(id.getNamespace())) {
            return id.getPath();
        }
        return id.getNamespace() + "_" + id.getPath();
    }

    private static JsonObject createRecipeJson(
            IngredientDefinition ingredient,
            boolean allowUnstackableAndTaggedInput,
            List<RecipeResult> results
    ) {
        if (results.isEmpty()) {
            throw new IllegalArgumentException("GrindRunner recipe requires at least one result.");
        }

        var json = new JsonObject();
        json.addProperty("type", RECIPE_TYPE);
        json.add("ingredient", ingredient.toJson());

        if (allowUnstackableAndTaggedInput) {
            json.addProperty(ALLOW_UNSTACKABLE_AND_TAGGED_INPUT, true);
        }

        var resultsArray = new JsonArray();
        for (var resultEntry : results) {
            var result = new JsonObject();
            result.addProperty("id", itemId(resultEntry.item()));
            result.addProperty("count", Math.max(1, resultEntry.count()));
            resultsArray.add(result);
        }
        json.add("results", resultsArray);
        return json;
    }

    private static RecipeDefinition recipe(
            ItemLike ingredientItem,
            RecipeResult... results
    ) {
        return new RecipeDefinition(ingredient(ingredientItem), false, List.of(results));
    }

    private static RecipeDefinition recipe(
            ItemLike ingredientItem,
            boolean allowUnstackableAndTaggedInput,
            RecipeResult... results
    ) {
        return new RecipeDefinition(ingredient(ingredientItem), allowUnstackableAndTaggedInput, List.of(results));
    }

    private static RecipeDefinition recipe(
            TagKey<Item> ingredientTag,
            RecipeResult... results
    ) {
        return new RecipeDefinition(ingredient(ingredientTag), false, List.of(results));
    }

    private static RecipeDefinition recipe(
            TagKey<Item> ingredientTag,
            boolean allowUnstackableAndTaggedInput,
            RecipeResult... results
    ) {
        return new RecipeDefinition(ingredient(ingredientTag), allowUnstackableAndTaggedInput, List.of(results));
    }

    private static RecipeResult result(ItemLike item, int count) {
        return new RecipeResult(item, count);
    }

    private static IngredientDefinition ingredient(ItemLike item) {
        return new IngredientDefinition(itemPath(item), "item", itemId(item));
    }

    private static IngredientDefinition ingredient(TagKey<Item> tag) {
        return new IngredientDefinition(tagPath(tag) + "_tag", "tag", tag.location().toString());
    }

    private static String itemId(ItemLike item) {
        var id = BuiltInRegistries.ITEM.getKey(item.asItem());
        if (id != null) {
            return id.toString();
        }
        return "unknown";
    }

    private static String tagPath(TagKey<Item> tag) {
        var id = tag.location();
        var path = id.getPath().replace('/', '_');
        if ("minecraft".equals(id.getNamespace())) {
            return path;
        }
        return id.getNamespace() + "_" + path;
    }

    @Override
    public @NotNull String getName() {
        return "ApprenticeCodex GrindRunner Recipes";
    }

    private record RecipeResult(
            ItemLike item,
            int count
    ) {
    }

    private record RecipeDefinition(
            IngredientDefinition ingredient,
            boolean allowUnstackableAndTaggedInput,
            List<RecipeResult> results
    ) {
    }

    private record IngredientDefinition(
            String recipeIdPath,
            String ingredientKey,
            String ingredientValue
    ) {
        private JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty(ingredientKey, ingredientValue);
            return json;
        }
    }
}
