package jp.aquafactory.apprenticecodex.datagen.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Malum を必須依存にせず、Soulcollector 一式の魂の注入レシピだけを出力する。 */
public final class MalumSpiritInfusionRecipeDataGenerator implements DataProvider {
    private static final String MALUM_MOD_ID = "malum";
    private static final String RECIPE_DIRECTORY = "recipe/malum/spirit_infusion";
    private final PackOutput.PathProvider pathProvider;

    public MalumSpiritInfusionRecipeDataGenerator(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, RECIPE_DIRECTORY);
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
        var spirits = List.of(new SpiritCost("arcane", 16), new SpiritCost("wicked", 16));
        var recipes = List.of(
                recipe("soulcollector_hat", "soul_hunter_cloak", ItemRegistry.SOULCOLLECTOR_HAT.get(),
                        ItemRegistry.APPRENTICE_MAGE_SCARF.get(), spirits),
                recipe("soulcollector_robe", "soul_hunter_robe", ItemRegistry.SOULCOLLECTOR_ROBE.get(),
                        ItemRegistry.APPRENTICE_MAGE_TORSO.get(), spirits),
                recipe("soulcollector_leggings", "soul_hunter_leggings", ItemRegistry.SOULCOLLECTOR_LEGGINGS.get(),
                        ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(), spirits),
                recipe("soulcollector_boots", "soul_hunter_boots", ItemRegistry.SOULCOLLECTOR_BOOTS.get(),
                        ItemRegistry.APPRENTICE_MAGE_BOOTS.get(), spirits)
        );
        return CompletableFuture.allOf(recipes.stream().map(recipe -> DataProvider.saveStable(
                cachedOutput,
                recipe.toJson(),
                pathProvider.json(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, recipe.path()))
        )).toArray(CompletableFuture[]::new));
    }

    private static RecipeDefinition recipe(
            String path, String input, net.minecraft.world.item.Item output,
            net.minecraft.world.item.Item baseArmor, List<SpiritCost> spirits
    ) {
        return new RecipeDefinition(path, input, output, baseArmor, spirits);
    }

    @Override
    public @NotNull String getName() {
        return "ApprenticeCodex Malum Spirit Infusion Recipes";
    }

    private record RecipeDefinition(
            String path, String input, net.minecraft.world.item.Item output,
            net.minecraft.world.item.Item baseArmor, List<SpiritCost> spirits
    ) {
        private JsonObject toJson() {
            var root = new JsonObject();
            var conditions = new JsonArray();
            var condition = new JsonObject();
            condition.addProperty("type", "neoforge:mod_loaded");
            condition.addProperty("modid", MALUM_MOD_ID);
            conditions.add(condition);
            root.add("neoforge:conditions", conditions);
            root.addProperty("type", "malum:spirit_infusion");
            root.add("input", item(MALUM_MOD_ID + ":" + input, 1));
            root.add("result", result(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(output).toString()));
            var extras = new JsonArray();
            extras.add(item(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(baseArmor).toString(), 1));
            extras.add(item("irons_spellbooks:magic_cloth", 2));
            extras.add(item("malum:refined_soulstone", 4));
            root.add("extraInputs", extras);
            var spiritJson = new JsonArray();
            for (var spirit : spirits) spiritJson.add(spirit.toJson());
            root.add("spirits", spiritJson);
            return root;
        }

        private static JsonObject item(String id, int count) {
            var result = new JsonObject();
            result.addProperty("item", id);
            if (count > 1) result.addProperty("count", count);
            return result;
        }

        private static JsonObject result(String id) {
            var result = new JsonObject();
            result.addProperty("id", id);
            return result;
        }
    }

    private record SpiritCost(String type, int count) {
        private JsonObject toJson() {
            var result = new JsonObject();
            result.addProperty("type", MALUM_MOD_ID + ":" + type);
            result.addProperty("count", count);
            return result;
        }
    }
}
