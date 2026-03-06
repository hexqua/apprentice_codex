package jp.aquafactory.apprenticecodex.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
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
                recipe(Items.STONE_SWORD, true, result(Items.COBBLESTONE, 2)),
                recipe(Items.GOLDEN_SWORD, true, result(Items.GOLD_INGOT, 2)),
                recipe(Items.GOLDEN_AXE, true, result(Items.GOLD_INGOT, 3)),
                recipe(Items.BOW, true, result(Items.STRING, 3)),

                recipe(Items.AMETHYST_BLOCK, result(Items.AMETHYST_SHARD, 4)),
                recipe(Items.BONE, result(Items.BONE_MEAL, 6)),
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

                recipe(ItemRegistry.FIREWARD_RING.get(), true, result(ItemRegistry.MITHRIL_SCRAP.get(), 1), result(ItemRegistry.CINDER_ESSENCE.get(), 1)),
                recipe(ItemRegistry.FROSTWARD_RING.get(), true, result(ItemRegistry.MITHRIL_SCRAP.get(), 1), result(ItemRegistry.ICE_CRYSTAL.get(), 1)),
                recipe(ItemRegistry.POISONWARD_RING.get(), true, result(ItemRegistry.MITHRIL_SCRAP.get(), 1), result(Items.POISONOUS_POTATO, 4)),
                recipe(ItemRegistry.SILVER_RING.get(), true, result(ItemRegistry.MITHRIL_SCRAP.get(), 1), result(ItemRegistry.MITHRIL_INGOT.get(), 1))
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
                recipeIdPath(recipe.ingredientItem(), results.get(0).item())
        );
        return DataProvider.saveStable(
                cachedOutput,
                createRecipeJson(recipe.ingredientItem(), recipe.allowUnstackableAndTaggedInput(), results),
                pathProvider.json(id)
        );
    }

    private static String recipeIdPath(ItemLike ingredientItem, ItemLike resultItem) {
        return itemPath(resultItem) + "_from_" + itemPath(ingredientItem);
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
            ItemLike ingredientItem,
            boolean allowUnstackableAndTaggedInput,
            List<RecipeResult> results
    ) {
        if (results.isEmpty()) {
            throw new IllegalArgumentException("GrindRunner recipe requires at least one result.");
        }

        var json = new JsonObject();
        json.addProperty("type", RECIPE_TYPE);

        var ingredient = new JsonObject();
        ingredient.addProperty("item", itemId(ingredientItem));
        json.add("ingredient", ingredient);

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
        return new RecipeDefinition(ingredientItem, false, List.of(results));
    }

    private static RecipeDefinition recipe(
            ItemLike ingredientItem,
            boolean allowUnstackableAndTaggedInput,
            RecipeResult... results
    ) {
        return new RecipeDefinition(ingredientItem, allowUnstackableAndTaggedInput, List.of(results));
    }

    private static RecipeResult result(ItemLike item, int count) {
        return new RecipeResult(item, count);
    }

    private static String itemId(ItemLike item) {
        var id = BuiltInRegistries.ITEM.getKey(item.asItem());
        if (id != null) {
            return id.toString();
        }
        return "unknown";
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
            ItemLike ingredientItem,
            boolean allowUnstackableAndTaggedInput,
            List<RecipeResult> results
    ) {
    }
}
