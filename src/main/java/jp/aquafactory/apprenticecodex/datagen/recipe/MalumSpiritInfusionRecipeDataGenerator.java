package jp.aquafactory.apprenticecodex.datagen.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Malum を必須依存にせず、Soulcollector 一式の魂の注入レシピだけを出力する。 */
public final class MalumSpiritInfusionRecipeDataGenerator implements DataProvider {
    private static final String MALUM_MOD_ID = "malum";
    private static final String RECIPE_TYPE = MALUM_MOD_ID + ":spirit_infusion";
    private static final String RECIPE_DIRECTORY = "recipe/malum/spirit_infusion";
    private static final ResourceLocation MAGIC_CLOTH =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "magic_cloth");
    private static final ResourceLocation REFINED_SOULSTONE =
            ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, "refined_soulstone");
    private final PackOutput.PathProvider pathProvider;

    public MalumSpiritInfusionRecipeDataGenerator(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, RECIPE_DIRECTORY);
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
        var spirits = List.of(malumSpirit("arcane", 16), malumSpirit("wicked", 16));
        var recipes = List.of(
                soulcollectorRecipe("soulcollector_hat", "soul_hunter_cloak", ItemRegistry.SOULCOLLECTOR_HAT.get(),
                        ItemRegistry.APPRENTICE_MAGE_SCARF.get(), spirits),
                soulcollectorRecipe("soulcollector_robe", "soul_hunter_robe", ItemRegistry.SOULCOLLECTOR_ROBE.get(),
                        ItemRegistry.APPRENTICE_MAGE_TORSO.get(), spirits),
                soulcollectorRecipe("soulcollector_leggings", "soul_hunter_leggings", ItemRegistry.SOULCOLLECTOR_LEGGINGS.get(),
                        ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(), spirits),
                soulcollectorRecipe("soulcollector_boots", "soul_hunter_boots", ItemRegistry.SOULCOLLECTOR_BOOTS.get(),
                        ItemRegistry.APPRENTICE_MAGE_BOOTS.get(), spirits),
                spiritInfusionRecipe("soulstained_steel_swingcast_staff",
                        ItemRegistry.IRON_SWINGCAST_STAFF.getId(),
                        ItemRegistry.SOULSTAINED_STEEL_SWINGCAST_STAFF.getId(),
                        List.of(
                                new RecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "refined_brilliance"), 8),
                                new RecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "soul_stained_steel_ingot"), 4),
                                new RecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "hex_ash"), 8)
                        ),
                        List.of(
                                malumSpirit("wicked", 32),
                                malumSpirit("arcane", 16),
                                malumSpirit("eldritch", 8)
                        )
                ),
                spiritInfusionRecipe("malignant_spellcaster_gun",
                        ItemRegistry.DIAMOND_SPELLCASTER_GUN.getId(),
                        ItemRegistry.MALIGNANT_SPELLCASTER_GUN.getId(),
                        List.of(
                                new RecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "malignant_pewter_ingot"), 4),
                                new RecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "mnemonic_fragment"), 8),
                                new RecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "void_salts"), 8)
                        ),
                        List.of(
                                malumSpirit("wicked", 32),
                                malumSpirit("arcane", 64),
                                malumSpirit("eldritch", 32)
                        )
                )
        );
        return CompletableFuture.allOf(recipes.stream().map(recipe -> DataProvider.saveStable(
                cachedOutput,
                recipe.toJson(),
                pathProvider.json(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, recipe.path()))
        )).toArray(CompletableFuture[]::new));
    }

    /**
     * 任意MODの入力・出力と追加素材を取るSpirit Infusionの共通定義。
     * Soulcollectorの定型素材は{@link #soulcollectorRecipe}だけに閉じ込める。
     */
    private static RecipeDefinition spiritInfusionRecipe(
            String path,
            ResourceLocation input,
            ResourceLocation result,
            List<RecipeItem> extraInputs,
            List<SpiritCost> spirits
    ) {
        return new RecipeDefinition(path, input, result, extraInputs, spirits);
    }

    private static RecipeDefinition soulcollectorRecipe(
            String path,
            String malumInput,
            Item output,
            Item baseArmor,
            List<SpiritCost> spirits
    ) {
        return spiritInfusionRecipe(
                path,
                ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, malumInput),
                itemId(output),
                List.of(
                        recipeItem(baseArmor, 1),
                        new RecipeItem(MAGIC_CLOTH, 2),
                        new RecipeItem(REFINED_SOULSTONE, 4)
                ),
                spirits
        );
    }

    private static RecipeItem recipeItem(Item item, int count) {
        return new RecipeItem(itemId(item), count);
    }

    private static SpiritCost malumSpirit(String type, int count) {
        return new SpiritCost(ResourceLocation.fromNamespaceAndPath(MALUM_MOD_ID, type), count);
    }

    private static ResourceLocation itemId(Item item) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            throw new IllegalStateException("Unregistered item in Malum spirit infusion datagen: " + item);
        }
        return id;
    }

    @Override
    public @NotNull String getName() {
        return "ApprenticeCodex Malum Spirit Infusion Recipes";
    }

    private record RecipeDefinition(
            String path,
            ResourceLocation input,
            ResourceLocation result,
            List<RecipeItem> extraInputs,
            List<SpiritCost> spirits
    ) {
        private JsonObject toJson() {
            var root = new JsonObject();
            var conditions = new JsonArray();
            var condition = new JsonObject();
            condition.addProperty("type", "neoforge:mod_loaded");
            condition.addProperty("modid", MALUM_MOD_ID);
            conditions.add(condition);
            root.add("neoforge:conditions", conditions);
            root.addProperty("type", RECIPE_TYPE);
            root.add("input", item(input, 1));
            root.add("result", result(result));
            var extras = new JsonArray();
            for (var extraInput : extraInputs) {
                extras.add(extraInput.toJson());
            }
            root.add("extraInputs", extras);
            var spiritJson = new JsonArray();
            for (var spirit : spirits) spiritJson.add(spirit.toJson());
            root.add("spirits", spiritJson);
            return root;
        }

        private static JsonObject item(ResourceLocation id, int count) {
            var result = new JsonObject();
            result.addProperty("item", id.toString());
            if (count > 1) result.addProperty("count", count);
            return result;
        }

        private static JsonObject result(ResourceLocation id) {
            var result = new JsonObject();
            result.addProperty("id", id.toString());
            return result;
        }
    }

    private record RecipeItem(ResourceLocation item, int count) {
        private JsonObject toJson() {
            return RecipeDefinition.item(item, count);
        }
    }

    private record SpiritCost(ResourceLocation type, int count) {
        private JsonObject toJson() {
            var result = new JsonObject();
            result.addProperty("type", type.toString());
            result.addProperty("count", count);
            return result;
        }
    }
}
