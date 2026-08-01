package jp.aquafactory.apprenticecodex.datagen.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/** Malum を runData の実行依存へ加えず、通常作業台で使う条件付きレシピを出力する。 */
public final class MalumCraftingRecipeDataGenerator implements DataProvider {
    private static final String MALUM_MOD_ID = "malum";
    private static final String RECIPE_NAME = "soulstained_steel_spell_amplifier";
    private final PackOutput.PathProvider recipePathProvider;
    private final PackOutput.PathProvider advancementPathProvider;

    public MalumCraftingRecipeDataGenerator(PackOutput output) {
        recipePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
        advancementPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancement/recipes/combat");
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
        var id = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, RECIPE_NAME);
        return CompletableFuture.allOf(
                DataProvider.saveStable(cachedOutput, createRecipe(), recipePathProvider.json(id)),
                DataProvider.saveStable(cachedOutput, createAdvancement(), advancementPathProvider.json(id))
        );
    }

    private static JsonObject createRecipe() {
        var root = conditionalRoot();
        root.addProperty("type", "minecraft:crafting_shaped");
        root.addProperty("category", "equipment");

        var key = new JsonObject();
        key.add("H", item("malum:hex_ash"));
        key.add("A", item("irons_spellbooks:arcane_ingot"));
        key.add("S", item("malum:soul_stained_steel_ingot"));
        root.add("key", key);

        var pattern = new JsonArray();
        pattern.add("HAH");
        pattern.add(" S ");
        pattern.add(" S ");
        root.add("pattern", pattern);

        var result = new JsonObject();
        var components = new JsonObject();
        var spellContainer = new JsonObject();
        spellContainer.add("data", new JsonArray());
        spellContainer.addProperty("maxSpells", 1);
        spellContainer.addProperty("mustEquip", false);
        spellContainer.addProperty("spellWheel", true);
        components.add("irons_spellbooks:spell_container", spellContainer);
        result.add("components", components);
        result.addProperty("count", 1);
        result.addProperty("id", ApprenticeCodex.MODID + ":" + RECIPE_NAME);
        root.add("result", result);
        return root;
    }

    private static JsonObject createAdvancement() {
        var root = conditionalRoot();
        root.addProperty("parent", "minecraft:recipes/root");

        var criteria = new JsonObject();
        var hasIngot = new JsonObject();
        var hasIngotConditions = new JsonObject();
        var itemPredicates = new JsonArray();
        var itemPredicate = new JsonObject();
        itemPredicate.addProperty("items", "malum:soul_stained_steel_ingot");
        itemPredicates.add(itemPredicate);
        hasIngotConditions.add("items", itemPredicates);
        hasIngot.add("conditions", hasIngotConditions);
        hasIngot.addProperty("trigger", "minecraft:inventory_changed");
        criteria.add("has_soul_stained_steel_ingot", hasIngot);

        var hasRecipe = new JsonObject();
        var hasRecipeConditions = new JsonObject();
        hasRecipeConditions.addProperty("recipe", ApprenticeCodex.MODID + ":" + RECIPE_NAME);
        hasRecipe.add("conditions", hasRecipeConditions);
        hasRecipe.addProperty("trigger", "minecraft:recipe_unlocked");
        criteria.add("has_the_recipe", hasRecipe);
        root.add("criteria", criteria);

        var requirements = new JsonArray();
        var alternatives = new JsonArray();
        alternatives.add("has_the_recipe");
        alternatives.add("has_soul_stained_steel_ingot");
        requirements.add(alternatives);
        root.add("requirements", requirements);

        var rewards = new JsonObject();
        var recipes = new JsonArray();
        recipes.add(ApprenticeCodex.MODID + ":" + RECIPE_NAME);
        rewards.add("recipes", recipes);
        root.add("rewards", rewards);
        return root;
    }

    private static JsonObject conditionalRoot() {
        var root = new JsonObject();
        var conditions = new JsonArray();
        var condition = new JsonObject();
        condition.addProperty("type", "neoforge:mod_loaded");
        condition.addProperty("modid", MALUM_MOD_ID);
        conditions.add(condition);
        root.add("neoforge:conditions", conditions);
        return root;
    }

    private static JsonObject item(String id) {
        var item = new JsonObject();
        item.addProperty("item", id);
        return item;
    }

    @Override
    public @NotNull String getName() {
        return "ApprenticeCodex Malum Crafting Recipes";
    }
}
