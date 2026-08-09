package jp.aquafactory.apprenticecodex.datagen.recipe;

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

public final class AlchemyBrewerRecipeDataGenerator implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public AlchemyBrewerRecipeDataGenerator(PackOutput output) {
        pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
    }

    @Override public @NotNull CompletableFuture<?> run(@NotNull CachedOutput output) {
        var futures = new ArrayList<CompletableFuture<?>>();
        var bases = List.of(
                new Base("nether_wart", "apprenticecodex:alchemy_brewer/high_efficiency_bases", 1000, 200),
                new Base("glow_lichen", "apprenticecodex:alchemy_brewer/fast_bases", 750, 100)
        );
        var potions = List.of(
                new Product("swiftness", "minecraft:sugar", "minecraft:swiftness"),
                new Product("healing", "minecraft:glistering_melon_slice", "minecraft:healing"),
                new Product("fire_resistance", "minecraft:magma_cream", "minecraft:fire_resistance"),
                new Product("night_vision", "minecraft:golden_carrot", "minecraft:night_vision"),
                new Product("meditation", "apprenticecodex:comfort_berries", "apprenticecodex:meditation"),
                new Product("mana", "irons_spellbooks:arcane_essence", "irons_spellbooks:instant_mana_one")
        );
        for (var base : bases) for (var product : potions) {
            var json = new JsonObject(); json.addProperty("type", ApprenticeCodex.MODID + ":alchemy_brewer");
            json.add("base", tag(base.tag)); json.add("ingredient", ingredient(product.item));
            json.addProperty("result", product.potion); json.addProperty("fluid_amount_mb", base.amount);
            json.addProperty("processing_time_ticks", base.ticks); json.addProperty("priority", 0);
            futures.add(save(output, "alchemy_brewer/" + base.name + "/" + product.name, json));
        }
        var modifiers = List.of(
                new Modifier("long_swiftness", "minecraft:swiftness", "minecraft:redstone", "minecraft:long_swiftness"),
                new Modifier("strong_swiftness", "minecraft:swiftness", "minecraft:glowstone_dust", "minecraft:strong_swiftness"),
                new Modifier("strong_healing", "minecraft:healing", "minecraft:glowstone_dust", "minecraft:strong_healing"),
                new Modifier("long_fire_resistance", "minecraft:fire_resistance", "minecraft:redstone", "minecraft:long_fire_resistance"),
                new Modifier("long_night_vision", "minecraft:night_vision", "minecraft:redstone", "minecraft:long_night_vision"),
                new Modifier("long_meditation", "apprenticecodex:meditation", "minecraft:redstone", "apprenticecodex:long_meditation"),
                new Modifier("strong_meditation", "apprenticecodex:meditation", "minecraft:glowstone_dust", "apprenticecodex:strong_meditation"),
                new Modifier("strong_mana", "irons_spellbooks:instant_mana_one", "minecraft:glowstone_dust", "irons_spellbooks:instant_mana_two")
        );
        for (var modifier : modifiers) {
            var json = new JsonObject(); json.addProperty("type", ApprenticeCodex.MODID + ":alchemy_brewer_modifier");
            json.addProperty("input", modifier.input); json.add("ingredient", ingredient(modifier.item));
            json.addProperty("result", modifier.result); json.addProperty("priority", 0);
            futures.add(save(output, "alchemy_brewer_modifier/" + modifier.name, json));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> save(CachedOutput output, String path, JsonObject json) {
        return DataProvider.saveStable(output, json, pathProvider.json(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, path)));
    }
    private static JsonObject ingredient(String item) { var json = new JsonObject(); json.addProperty("item", item); return json; }
    private static JsonObject tag(String tag) { var json = new JsonObject(); json.addProperty("tag", tag); return json; }
    @Override public @NotNull String getName() { return "ApprenticeCodex Alchemy Brewer Recipes"; }
    private record Base(String name, String tag, int amount, int ticks) { }
    private record Product(String name, String item, String potion) { }
    private record Modifier(String name, String input, String item, String result) { }
}
