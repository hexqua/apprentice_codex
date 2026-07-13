package jp.aquafactory.apprenticecodex.datagen.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class MalumSpiritRepairRecipeDataGenerator implements DataProvider {
    private static final String MALUM_MOD_ID = "malum";
    private static final String RECIPE_TYPE = MALUM_MOD_ID + ":spirit_repair";
    private static final String RECIPE_DIRECTORY = "recipe/malum/spirit_crucible/repair";
    private static final String ARCANE_SPIRIT = MALUM_MOD_ID + ":arcane";
    private static final String EARTH_SPIRIT = MALUM_MOD_ID + ":earthen";
    private static final String ELDRITCH_SPIRIT = MALUM_MOD_ID + ":eldritch";
    private static final String INFERNAL_SPIRIT = MALUM_MOD_ID + ":infernal";
    private static final String SACRED_SPIRIT = MALUM_MOD_ID + ":sacred";
    private static final String AERIAL_SPIRIT = MALUM_MOD_ID + ":aerial";

    private final PackOutput.PathProvider pathProvider;

    public MalumSpiritRepairRecipeDataGenerator(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, RECIPE_DIRECTORY);
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
        // Malum は通常の修理素材定義から魂のるつぼ対象を自動解決しないため、対象装備と spirit コストを明示する。
        // 素材ごとにアイテムリストをまとめることで JEI の表示を整理する。
        var recipes = List.of(
                recipe(
                        "arcane_essence_armaments_repair",
                        0.5F,
                        List.of(
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.APPRENTICE_MAGE_BOOTS.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.REFLECTCAST_SHIELD.get()
                        ),
                        io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get(),
                        1,
                        List.of(spirit(ARCANE_SPIRIT, 8))
                ),
                recipe(
                        "hogskin_armaments_repair",
                        0.5F,
                        List.of(
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.ENCHANTRESS_HAT.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.ENCHANTRESS_ROBE.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.ENCHANTRESS_BOOTS.get()
                        ),
                        io.redspace.ironsspellbooks.registries.ItemRegistry.HOGSKIN.get(),
                        1,
                        List.of(
                                spirit(INFERNAL_SPIRIT, 8),
                                spirit(SACRED_SPIRIT, 4)
                        )
                ),
                recipe(
                        "mithril_scrap_armaments_repair",
                        1.0F,
                        List.of(
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get()
                        ),
                        io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get(),
                        1,
                        List.of(
                                spirit(ARCANE_SPIRIT, 16),
                                spirit(EARTH_SPIRIT, 16),
                                spirit(SACRED_SPIRIT, 16),
                                spirit(ELDRITCH_SPIRIT, 4)
                        )
                ),
                recipe(
                        "arcane_ingot_armaments_repair",
                        0.5F,
                        List.of(
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.ELEMENTAL_BOW.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.MANA_FORCE_BLADE.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.SPELL_SIDE_EDGE.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.SPELLCHARGED_GREATSWORD.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.BULWARK_GREATSHIELD.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.PARRYCAST_BUCKLER.get()
                        ),
                        io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get(),
                        1,
                        List.of(
                                spirit(ARCANE_SPIRIT, 8),
                                spirit(EARTH_SPIRIT, 8)
                        )
                ),
                recipe(
                        "magic_cloth_armaments_repair",
                        0.5F,
                        List.of(
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.MAGI_AGENT_SUIT_HOOD.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.MAGI_AGENT_SUIT_COAT.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get(),
                                jp.aquafactory.apprenticecodex.registry.ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()
                        ),
                        io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get(),
                        1,
                        List.of(
                                spirit(ARCANE_SPIRIT, 8),
                                spirit(AERIAL_SPIRIT, 8)
                        )
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
        var root = new JsonObject();
        root.add("neoforge:conditions", modLoadedConditions());
        root.addProperty("type", RECIPE_TYPE);
        root.addProperty("durabilityPercentage", recipe.durabilityPercentage());
        root.add("validItems", serializeItems(recipe.validItems()));
        root.add("repairMaterial", serializeRepairMaterial(recipe.repairMaterial(), recipe.repairMaterialCount()));
        root.add("spirits", serializeSpirits(recipe.spirits()));
        return root;
    }

    private static JsonArray modLoadedConditions() {
        var conditions = new JsonArray();
        var condition = new JsonObject();
        condition.addProperty("type", "neoforge:mod_loaded");
        condition.addProperty("modid", MALUM_MOD_ID);
        conditions.add(condition);
        return conditions;
    }

    private static JsonArray serializeItems(List<Item> validItems) {
        if (validItems.isEmpty()) {
            throw new IllegalArgumentException("Malum spirit repair recipe requires at least one input.");
        }

        var itemArray = new JsonArray();
        for (var item : validItems) {
            itemArray.add(itemId(item));
        }
        return itemArray;
    }

    private static JsonObject serializeRepairMaterial(Item repairMaterial, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Repair material count must be positive.");
        }

        var repairMaterialJson = new JsonObject();
        repairMaterialJson.addProperty("item", itemId(repairMaterial));
        repairMaterialJson.addProperty("count", count);
        return repairMaterialJson;
    }

    private static JsonArray serializeSpirits(List<SpiritCost> spirits) {
        if (spirits.isEmpty()) {
            throw new IllegalArgumentException("Malum spirit repair recipe requires at least one spirit cost.");
        }

        var spiritArray = new JsonArray();
        for (var spirit : spirits) {
            var spiritJson = new JsonObject();
            spiritJson.addProperty("type", spirit.type());
            spiritJson.addProperty("count", spirit.count());
            spiritArray.add(spiritJson);
        }
        return spiritArray;
    }

    private static RecipeDefinition recipe(
            String path,
            float durabilityPercentage,
            List<Item> validItems,
            Item repairMaterial,
            int repairMaterialCount,
            List<SpiritCost> spirits
    ) {
        return new RecipeDefinition(path, durabilityPercentage, validItems, repairMaterial, repairMaterialCount, spirits);
    }

    private static SpiritCost spirit(String type, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Spirit count must be positive.");
        }
        return new SpiritCost(type, count);
    }

    private static String itemId(Item item) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            throw new IllegalStateException("Unregistered item in Malum spirit repair datagen: " + item);
        }
        return id.toString();
    }

    @Override
    public @NotNull String getName() {
        return "ApprenticeCodex Malum Spirit Repair Recipes";
    }

    private record RecipeDefinition(
            String path,
            float durabilityPercentage,
            List<Item> validItems,
            Item repairMaterial,
            int repairMaterialCount,
            List<SpiritCost> spirits
    ) {
    }

    private record SpiritCost(
            String type,
            int count
    ) {
    }
}
