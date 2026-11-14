package jp.aquafactory.apprenticecodex.datagen.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SpectrumEnchantmentRecipeDataGenerator implements DataProvider {
    private static final String ARCANE_CINDER = "apprenticecodex:arcane_cinder";
    private static final String ARCANE_SHARD = "apprenticecodex:crystalline_arcane_shard";
    private static final String STORM_STONE = "spectrum:storm_stone";
    private static final String ARCANE_ESSENCE = "irons_spellbooks:arcane_essence";
    private static final String SUGAR = "minecraft:sugar";
    private static final String COMFORT_BERRIES = "apprenticecodex:comfort_berries";

    private final PackOutput.PathProvider paths;

    public SpectrumEnchantmentRecipeDataGenerator(PackOutput output) {
        paths = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipes/spectrum");
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput output) {
        // Spectrumをdatagenへ導入せず、1.20.1の公開レシピ形式だけに依存する。
        var recipes = List.of(
                new Definition("surge", "purple", ARCANE_CINDER, STORM_STONE, 8, 1, 200),
                new Definition("attunement", "green", ARCANE_CINDER, STORM_STONE, 8, 1, 200),
                new Definition("tense", "magenta", ARCANE_CINDER, SUGAR, 8, 1, 200),
                new Definition("alacrity", "yellow", ARCANE_CINDER, SUGAR, 8, 1, 200),
                new Definition("reflux", "black", ARCANE_CINDER, ARCANE_ESSENCE, 8, 1, 400),
                new Definition("reservoir", "cyan", ARCANE_CINDER, ARCANE_ESSENCE, 8, 1, 400),
                new Definition("transcendence", "purple", ARCANE_CINDER, "apprenticecodex:wisdom_shard", 1, 0, 0),
                new Definition("wisdom", "purple", "minecraft:experience_bottle", ARCANE_SHARD, 6, 1, 200),
                new Definition("plunder", "light_blue", ARCANE_SHARD, "spectrum:four_leaf_clover", 6, 2, 200),
                new Definition("red_energy", "red", "minecraft:redstone", COMFORT_BERRIES, 6, 2, 400),
                new Definition("glow_energy", "yellow", "minecraft:glowstone_dust", COMFORT_BERRIES, 4, 2, 400),
                new Definition("synthesis", "red", "spectrum:quitoxic_powder", ARCANE_CINDER, 1, 0, 0)
        );
        var writes = new ArrayList<CompletableFuture<?>>();
        for (var recipe : recipes) {
            writes.add(save(output, "enchanter/" + recipe.enchantment(), book(recipe)));
            if (recipe.maxLevel() > 1) {
                writes.add(save(output, "enchantment_upgrade/" + recipe.enchantment(), upgrade(recipe)));
            }
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> save(CachedOutput output, String path, JsonObject json) {
        return DataProvider.saveStable(output, json, paths.json(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, path)));
    }

    private static JsonObject base(String type) {
        var root = new JsonObject();
        var conditions = new JsonArray();
        var condition = new JsonObject();
        condition.addProperty("type", "forge:mod_loaded");
        condition.addProperty("modid", "spectrum");
        conditions.add(condition);
        root.add("conditions", conditions);
        root.addProperty("type", "spectrum:" + type);
        // 限界突破の追加解放はSpectrum自身が別途判定するため、通常の装置解放だけを要求する。
        root.addProperty("required_advancement", "spectrum:midgame/build_enchanting_structure");
        return root;
    }

    private static JsonObject book(Definition recipe) {
        if (recipe.material1().equals(recipe.material2())) {
            throw new IllegalArgumentException("Spectrum book materials must be distinct: " + recipe.enchantment());
        }
        var root = base("enchanter");
        root.addProperty("time", 300);
        root.addProperty("required_experience", 100);
        var ingredients = new JsonArray();
        ingredients.add(item("minecraft:book"));
        for (int i = 0; i < 2; i++) {
            ingredients.add(item(recipe.pigment()));
            ingredients.add(item(recipe.pigment()));
            ingredients.add(item(recipe.material1()));
            ingredients.add(item(recipe.material2()));
        }
        root.add("ingredients", ingredients);
        var result = new JsonObject();
        result.addProperty("item", "minecraft:enchanted_book");
        // 1.20.1の本はdata componentではなく、short型のlvlを持つStoredEnchantmentsを使う。
        result.addProperty("nbt", "{StoredEnchantments:[{id:\"apprenticecodex:" + recipe.enchantment() + "\",lvl:1s}]}");
        root.add("result", result);
        return root;
    }

    private static JsonObject upgrade(Definition recipe) {
        var root = base("enchantment_upgrade");
        root.addProperty("enchantment", "apprenticecodex:" + recipe.enchantment());
        var levels = new JsonArray();
        for (int sourceLevel = 1; sourceLevel < recipe.maxLevel(); sourceLevel++) {
            int count = recipe.initialCountPerBowl() * (1 << (sourceLevel - 1));
            // Item Bowlは1スタックまで。達成不能な要求を生成しない。
            if (count < 1 || count > 64) {
                throw new IllegalArgumentException("Spectrum upgrade exceeds bowl capacity: " + recipe.enchantment());
            }
            var level = new JsonObject();
            level.addProperty("item", recipe.pigment());
            // Spectrum 1.8.15は皿ごとの個数ではなく8皿の合計を要求する。
            level.addProperty("item_count", count * 8);
            level.addProperty("experience", recipe.initialExperience() * (1 << (sourceLevel - 1)));
            levels.add(level);
        }
        root.add("levels", levels);
        return root;
    }

    private static JsonObject item(String id) {
        var item = new JsonObject();
        item.addProperty("item", id);
        return item;
    }

    @Override
    public @NotNull String getName() {
        return "Spectrum enchantment recipes";
    }

    private record Definition(String enchantment, String color, String material1, String material2,
                              int maxLevel, int initialCountPerBowl, int initialExperience) {
        String pigment() {
            return "spectrum:" + color + "_pigment";
        }
    }
}
