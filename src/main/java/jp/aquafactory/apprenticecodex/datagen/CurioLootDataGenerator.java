package jp.aquafactory.apprenticecodex.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CurioLootDataGenerator implements DataProvider {
    private static final int IRONS_BASIC_CURIO_COUNT = 13;
    private static final int APPRENTICE_BASIC_CURIO_COUNT = 3;
    private static final List<ResourceLocation> APPRENTICE_BASIC_CURIO_ITEM_IDS = List.of(
            ItemRegistry.SCARLET_THIRST.getId(),
            ItemRegistry.CRAFTSMANS_DELIGHT.getId(),
            ItemRegistry.PROTECTION_SPELL_SUPPORTER.getId(),
            ItemRegistry.ENCHANTED_CIRCLET.getId()
    );

    private static final ResourceLocation BASIC_CURIOS_BONUS =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "magic_items/basic_curios_bonus");
    private static final ResourceLocation OMIMOUS_VAULT_CURIOS_BONUS =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "magic_items/ominous_vault_curios_bonus");
    private static final ResourceLocation APPRENTICE_CURIO_LOOT_CHANCE_CONDITION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "apprentice_curio_loot_chance");

    private final PackOutput.PathProvider lootTablePathProvider;
    private final PackOutput.PathProvider lootModifierPathProvider;

    public CurioLootDataGenerator(PackOutput output) {
        lootTablePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table");
        lootModifierPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_modifiers");
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
        var futures = new ArrayList<CompletableFuture<?>>();

        futures.add(saveLootTable(cachedOutput, BASIC_CURIOS_BONUS, createBasicCuriosBonusTable()));
        futures.add(saveLootTable(cachedOutput, OMIMOUS_VAULT_CURIOS_BONUS,
                createItemTable(APPRENTICE_BASIC_CURIO_ITEM_IDS, 1.0D)));

        futures.add(saveLootModifier(cachedOutput,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "add_apprentice_curios_to_generic_loot"),
                createAppendLootModifier(List.of(
                                ResourceLocation.withDefaultNamespace("chests/buried_treasure"),
                                ResourceLocation.withDefaultNamespace("chests/desert_pyramid"),
                                ResourceLocation.withDefaultNamespace("chests/jungle_temple"),
                                ResourceLocation.withDefaultNamespace("chests/pillager_outpost"),
                                ResourceLocation.withDefaultNamespace("chests/shipwreck_map"),
                                ResourceLocation.withDefaultNamespace("chests/simple_dungeon"),
                                ResourceLocation.withDefaultNamespace("chests/stronghold_crossing"),
                                ResourceLocation.withDefaultNamespace("chests/stronghold_corridor"),
                                ResourceLocation.withDefaultNamespace("chests/underwater_ruin_big"),
                                ResourceLocation.withDefaultNamespace("chests/underwater_ruin_small"),
                                ResourceLocation.withDefaultNamespace("chests/woodland_mansion"),
                                ResourceLocation.withDefaultNamespace("chests/village/village_cartographer"),
                                ResourceLocation.withDefaultNamespace("chests/village/village_temple"),
                                ResourceLocation.withDefaultNamespace("chests/ruined_portal"),
                                ResourceLocation.withDefaultNamespace("chests/abandoned_mineshaft"),
                                ResourceLocation.fromNamespaceAndPath("betterdungeons", "zombie_dungeon/chests/common"),
                                ResourceLocation.fromNamespaceAndPath("betterdungeons", "skeleton_dungeon/chests/common"),
                                ResourceLocation.fromNamespaceAndPath("betterdungeons", "spider_dungeon/chests/egg_room"),
                                ResourceLocation.fromNamespaceAndPath("betterstrongholds", "chests/common"),
                                ResourceLocation.fromNamespaceAndPath("betterstrongholds", "chests/grand_library"),
                                ResourceLocation.fromNamespaceAndPath("structory", "harvest/graveyard"),
                                ResourceLocation.fromNamespaceAndPath("structory", "harvest/graveyard2"),
                                ResourceLocation.fromNamespaceAndPath("structory", "harvest/old_manor/common"),
                                ResourceLocation.fromNamespaceAndPath("structory", "ruin/swamp/loot"),
                                ResourceLocation.fromNamespaceAndPath("structory", "ruin/taiga/illager_low"),
                                ResourceLocation.fromNamespaceAndPath("structory", "ruin/taiga/illager_high"),
                                ResourceLocation.fromNamespaceAndPath("structory", "harvest/manor2/loot"),
                                ResourceLocation.fromNamespaceAndPath("idas", "chests/wizardtower/wizardtower_library"),
                                ResourceLocation.fromNamespaceAndPath("idas", "chests/apothecary_abode/apothecary_abode"),
                                ResourceLocation.fromNamespaceAndPath("nova_structures", "chests/badland_miner_outpost_towers"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/magic_bookshelf_loot")
                        ),
                        createChanceWrappedLootTableId("generic"),
                        basicCurioEquivalentChance(0.05D)
                )));
        futures.add(saveLootModifier(cachedOutput,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "add_apprentice_curios_to_good_loot"),
                createAppendLootModifier(List.of(
                                ResourceLocation.fromNamespaceAndPath("betterdeserttemples", "chests/tomb"),
                                ResourceLocation.fromNamespaceAndPath("betterdeserttemples", "chests/storage"),
                                ResourceLocation.fromNamespaceAndPath("betterdeserttemples", "chests/library"),
                                ResourceLocation.fromNamespaceAndPath("betterdungeons", "zombie_dungeon/chests/tombstone"),
                                ResourceLocation.fromNamespaceAndPath("betterwitchhuts", "chests/hut_0"),
                                ResourceLocation.fromNamespaceAndPath("structory", "library/high"),
                                ResourceLocation.fromNamespaceAndPath("structory", "library/low"),
                                ResourceLocation.fromNamespaceAndPath("structory", "ruin/taiga/illager_treasure"),
                                ResourceLocation.fromNamespaceAndPath("idas", "chests/wizardtower/wizardtower_top"),
                                ResourceLocation.fromNamespaceAndPath("idas", "chests/tinkers_workshop/tinkers_workshop_vault"),
                                ResourceLocation.withDefaultNamespace("chests/ancient_city")
                        ),
                        createChanceWrappedLootTableId("good"),
                        basicCurioEquivalentChance(0.15D)
                )));
        futures.add(saveLootModifier(cachedOutput,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "add_apprentice_curios_to_treasure_loot"),
                createAppendLootModifier(List.of(
                                ResourceLocation.fromNamespaceAndPath("betterdeserttemples", "chests/pharaoh_hidden"),
                                ResourceLocation.fromNamespaceAndPath("betterfortresses", "chests/worship")
                        ),
                        createChanceWrappedLootTableId("treasure"),
                        basicCurioEquivalentChance(0.8D)
                )));
        futures.add(saveLootModifier(cachedOutput,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "add_apprentice_curios_to_library_endgame_loot"),
                createAppendLootModifier(List.of(
                                ResourceLocation.withDefaultNamespace("chests/stronghold_library"),
                                ResourceLocation.withDefaultNamespace("chests/end_city_treasure"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/citadel/citadel_vault")
                        ),
                        createChanceWrappedLootTableId("library_endgame"),
                        basicCurioEquivalentChance(0.5D)
                )));
        futures.add(saveLootModifier(cachedOutput,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "add_apprentice_curios_to_nether_and_trial_loot"),
                createAppendLootModifier(List.of(
                                ResourceLocation.withDefaultNamespace("chests/nether_bridge"),
                                ResourceLocation.withDefaultNamespace("chests/bastion_treasure"),
                                ResourceLocation.withDefaultNamespace("chests/bastion_bridge"),
                                ResourceLocation.withDefaultNamespace("chests/bastion_other"),
                                ResourceLocation.withDefaultNamespace("chests/trial_chambers/corridor"),
                                ResourceLocation.withDefaultNamespace("chests/trial_chambers/entrance"),
                                ResourceLocation.withDefaultNamespace("chests/trial_chambers/intersection"),
                                ResourceLocation.withDefaultNamespace("chests/trial_chambers/intersection_barrel"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/pyromancer_tower/pyromancer_basic_storage")
                        ),
                        createChanceWrappedLootTableId("nether_and_trial"),
                        basicCurioEquivalentChance(0.075D)
                )));
        futures.add(saveLootModifier(cachedOutput,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "add_apprentice_curios_to_regular_trial_vault"),
                createAppendLootModifier(
                        List.of(ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward")),
                        createChanceWrappedLootTableId("regular_trial_vault"),
                        APPRENTICE_BASIC_CURIO_COUNT * (3.0D / 29.0D / IRONS_BASIC_CURIO_COUNT)
                )));
        futures.add(saveLootModifier(cachedOutput,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "add_apprentice_curios_to_irons_tier_two_loot"),
                createAppendLootModifier(List.of(
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/evoker_fort"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/generic_magic_treasure")
                        ),
                        createChanceWrappedLootTableId("irons_tier_two"),
                        basicCurioEquivalentChance(0.2D)
                )));
        futures.add(saveLootModifier(cachedOutput,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "add_apprentice_curios_to_ice_region_loot"),
                createAppendLootModifier(List.of(
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/ice_spider_den/basement"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/ice_spider_den/dungeon"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/ice_spider_den/tower"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/impaled_icebreaker/captain_quarters")
                        ),
                        createChanceWrappedLootTableId("ice_region"),
                        basicCurioEquivalentChance(0.25D)
                )));
        futures.add(saveLootModifier(cachedOutput,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "add_apprentice_curios_to_mountain_tower"),
                createAppendLootModifier(
                        List.of(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/mountain_tower/mountain_tower")),
                        createChanceWrappedLootTableId("mountain_tower"),
                        basicCurioEquivalentChance(0.3D)
                )));
        futures.add(saveLootModifier(cachedOutput,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "add_apprentice_curios_to_nature_fire_loot"),
                createAppendLootModifier(List.of(
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/mangrove_hut"),
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/pyromancer_tower/pyromancer_supplies")
                        ),
                        createChanceWrappedLootTableId("nature_fire"),
                        basicCurioEquivalentChance(0.4D)
                )));
        futures.add(saveLootModifier(cachedOutput,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "add_apprentice_curios_to_catacombs_crypt"),
                createAppendLootModifier(
                        List.of(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/catacombs/crypt_loot")),
                        createChanceWrappedLootTableId("catacombs_crypt"),
                        basicCurioEquivalentChance(1.0D)
                )));
        futures.add(saveLootModifier(cachedOutput,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "add_apprentice_curios_to_catacombs_wall"),
                createAppendLootModifier(
                        List.of(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "chests/catacombs/wall_loot")),
                        createChanceWrappedLootTableId("catacombs_wall"),
                        basicCurioEquivalentChance(0.35D)
                )));
        futures.add(saveLootModifier(cachedOutput,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "add_apprentice_curios_to_ominous_vault"),
                createAppendLootModifier(
                        List.of(ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward_ominous")),
                        OMIMOUS_VAULT_CURIOS_BONUS,
                        0.075D
                )));
        futures.add(DataProvider.saveStable(cachedOutput, createGlobalLootModifierList(), lootModifierPathProvider.json(
                ResourceLocation.fromNamespaceAndPath("neoforge", "global_loot_modifiers")
        )));

        for (var id : List.of(
                "generic",
                "good",
                "treasure",
                "library_endgame",
                "nether_and_trial",
                "regular_trial_vault",
                "irons_tier_two",
                "ice_region",
                "mountain_tower",
                "nature_fire",
                "catacombs_crypt",
                "catacombs_wall"
        )) {
            futures.add(saveLootTable(cachedOutput, createChanceWrappedLootTableId(id), createChanceWrappedTable(BASIC_CURIOS_BONUS)));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public @NotNull String getName() {
        return "ApprenticeCodex Curio Loot";
    }

    private CompletableFuture<?> saveLootTable(CachedOutput cachedOutput, ResourceLocation id, JsonObject json) {
        return DataProvider.saveStable(cachedOutput, json, lootTablePathProvider.json(id));
    }

    private CompletableFuture<?> saveLootModifier(CachedOutput cachedOutput, ResourceLocation id, JsonObject json) {
        return DataProvider.saveStable(cachedOutput, json, lootModifierPathProvider.json(id));
    }

    private static JsonObject createGlobalLootModifierList() {
        var root = new JsonObject();
        root.addProperty("replace", false);
        var entries = new JsonArray();
        for (var id : List.of(
                "apprenticecodex:add_isekai_travel_guidebook_to_bonus_chest",
                "apprenticecodex:add_apprentice_curios_to_generic_loot",
                "apprenticecodex:add_apprentice_curios_to_good_loot",
                "apprenticecodex:add_apprentice_curios_to_treasure_loot",
                "apprenticecodex:add_apprentice_curios_to_library_endgame_loot",
                "apprenticecodex:add_apprentice_curios_to_nether_and_trial_loot",
                "apprenticecodex:add_apprentice_curios_to_regular_trial_vault",
                "apprenticecodex:add_apprentice_curios_to_irons_tier_two_loot",
                "apprenticecodex:add_apprentice_curios_to_ice_region_loot",
                "apprenticecodex:add_apprentice_curios_to_mountain_tower",
                "apprenticecodex:add_apprentice_curios_to_nature_fire_loot",
                "apprenticecodex:add_apprentice_curios_to_catacombs_crypt",
                "apprenticecodex:add_apprentice_curios_to_catacombs_wall",
                "apprenticecodex:add_apprentice_curios_to_ominous_vault"
        )) {
            entries.add(id);
        }
        root.add("entries", entries);
        return root;
    }

    private static JsonObject createBasicCuriosBonusTable() {
        return createItemTable(APPRENTICE_BASIC_CURIO_ITEM_IDS, 1.0D);
    }

    private static JsonObject createItemTable(List<ResourceLocation> itemIds, double rolls) {
        var root = new JsonObject();
        var pools = new JsonArray();
        var pool = new JsonObject();
        pool.addProperty("rolls", rolls);
        var entries = new JsonArray();
        for (var itemId : itemIds) {
            var entry = new JsonObject();
            entry.addProperty("type", "minecraft:item");
            entry.addProperty("name", itemId.toString());
            entries.add(entry);
        }
        pool.add("entries", entries);
        pools.add(pool);
        root.add("pools", pools);
        return root;
    }

    private static JsonObject createChanceWrappedTable(ResourceLocation nestedLootTableId) {
        var root = new JsonObject();
        root.addProperty("type", "minecraft:chest");
        var pools = new JsonArray();
        var pool = new JsonObject();
        pool.addProperty("rolls", 1);
        var entries = new JsonArray();
        var entry = new JsonObject();
        entry.addProperty("type", "minecraft:loot_table");
        entry.addProperty("value", nestedLootTableId.toString());
        entries.add(entry);
        pool.add("entries", entries);
        pools.add(pool);
        root.add("pools", pools);
        return root;
    }

    private static JsonObject createAppendLootModifier(
            List<ResourceLocation> lootTableIds,
            ResourceLocation key,
            double chance
    ) {
        var root = new JsonObject();
        root.addProperty("type", "irons_spellbooks:append_loot");
        var conditions = new JsonArray();
        conditions.add(createLootTableCondition(lootTableIds));
        conditions.add(createApprenticeCurioLootChanceCondition(chance));
        root.add("conditions", conditions);
        root.addProperty("key", key.toString());
        return root;
    }

    private static JsonObject createLootTableCondition(List<ResourceLocation> lootTableIds) {
        if (lootTableIds.size() == 1) {
            var condition = new JsonObject();
            condition.addProperty("condition", "neoforge:loot_table_id");
            condition.addProperty("loot_table_id", lootTableIds.getFirst().toString());
            return condition;
        }

        var condition = new JsonObject();
        condition.addProperty("condition", "minecraft:any_of");
        var terms = new JsonArray();
        for (var lootTableId : lootTableIds) {
            var term = new JsonObject();
            term.addProperty("condition", "neoforge:loot_table_id");
            term.addProperty("loot_table_id", lootTableId.toString());
            terms.add(term);
        }
        condition.add("terms", terms);
        return condition;
    }

    private static JsonObject createApprenticeCurioLootChanceCondition(double chance) {
        var condition = new JsonObject();
        condition.addProperty("condition", APPRENTICE_CURIO_LOOT_CHANCE_CONDITION.toString());
        condition.addProperty("base_chance", chance);
        return condition;
    }

    private static ResourceLocation createChanceWrappedLootTableId(String name) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "chests/" + name + "_curio_bonus");
    }

    private static double basicCurioEquivalentChance(double chanceToGetAnyIronsBasicCurio) {
        return chanceToGetAnyIronsBasicCurio * APPRENTICE_BASIC_CURIO_COUNT / IRONS_BASIC_CURIO_COUNT;
    }
}
