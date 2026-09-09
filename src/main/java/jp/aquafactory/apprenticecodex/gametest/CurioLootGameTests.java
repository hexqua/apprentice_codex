package jp.aquafactory.apprenticecodex.gametest;

import com.google.gson.JsonParser;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.spell.grindrunner.GrindRunnerWheelEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public class CurioLootGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final Set<String> WEIGHT_TWO = Set.of("scarlet_thirst", "craftsmans_delight",
            "protection_spell_supporter", "enchanted_circlet", "spell_cast_parrying_ring", "autocast_amulet",
            "satellite_followcast_amulet", "mana_shield_charm", "monarch_bond_charm");
    private static final Set<String> WEIGHT_ONE = Set.of("attackcast_ring", "mana_thruster", "jumpcast_charm",
            "mana_maneuver_gear", "quickcast_scroll_cartridge");

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.curio_loot")
    public static void generatedCuriosHaveWeightsAndRecyclingRecipes(GameTestHelper helper) throws IOException {
        // 生成済みデータと実際にロードされたレシピを照合し、登録だけの追加漏れを検出する。
        var location = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID,
                "loot_table/magic_items/basic_curios_bonus.json");
        try (var reader = helper.getLevel().getServer().getResourceManager().getResourceOrThrow(location).openAsReader()) {
            var pools = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("pools");
            helper.assertTrue(pools.size() == 1, "Curio loot must have one pool");
            var pool = pools.get(0).getAsJsonObject();
            helper.assertTrue(pool.get("rolls").getAsInt() == 1, "Curio loot must roll once");
            var entries = pool.getAsJsonArray("entries");
            helper.assertTrue(entries.size() == 14, "Curio loot must contain exactly fourteen items");
            var seen = new java.util.HashSet<String>();
            int weight = 0;
            for (var element : entries) {
                var entry = element.getAsJsonObject();
                var id = ResourceLocation.parse(entry.get("name").getAsString());
                helper.assertTrue(id.getNamespace().equals(ApprenticeCodex.MODID) && seen.add(id.getPath()),
                        "Curio entries must be unique Codex items: " + id);
                int expectedWeight = WEIGHT_TWO.contains(id.getPath()) ? 2 : WEIGHT_ONE.contains(id.getPath()) ? 1 : 0;
                helper.assertTrue(expectedWeight > 0 && entry.get("weight").getAsInt() == expectedWeight,
                        "Unexpected curio or weight: " + id);
                helper.assertFalse(entry.has("functions"), "Curio entries must not inject spells or enchantments");
                weight += expectedWeight;
                var item = BuiltInRegistries.ITEM.get(id);
                var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get(),
                        new SingleRecipeInput(new ItemStack(item)), helper.getLevel());
                helper.assertTrue(recipe.isPresent(), "Missing curio recycling recipe: " + id);
            }
            helper.assertTrue(weight == 23, "Curio weights must total twenty-three");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.curio_loot")
    public static void vaultBonusPreservesRewardsAndHonorsConfig(GameTestHelper helper) {
        ModConfigSpec.BooleanValue enabled = ApprenticeCodexServerConfig.SPEC.getValues().get("Loot.enableApprenticeCurioLoot");
        ModConfigSpec.DoubleValue multiplier = ApprenticeCodexServerConfig.SPEC.getValues().get("Loot.apprenticeCurioLootChanceMultiplier");
        boolean previousEnabled = enabled.get();
        double previousMultiplier = multiplier.get();
        try {
            // 同じseedで元報酬を比較し、追加分の抽選は倍率10で確定させる。統計的な当落には依存しない。
            enabled.set(false);
            var base = roll(helper, "irons_spellbooks:chests/catacombs/dead_king_vault");
            var ominous = roll(helper, "minecraft:chests/trial_chambers/reward_ominous");
            enabled.set(true);
            multiplier.set(0.0D);
            assertSameLoot(helper, base, roll(helper, "irons_spellbooks:chests/catacombs/dead_king_vault"));
            multiplier.set(10.0D);
            var added = roll(helper, "irons_spellbooks:chests/catacombs/dead_king_vault");
            helper.assertTrue(added.size() == base.size() + 1, "Dead King Vault must append exactly one bonus");
            assertSameLoot(helper, base, added.subList(0, base.size()));
            var bonus = added.getLast();
            var id = BuiltInRegistries.ITEM.getKey(bonus.getItem());
            helper.assertTrue(id.getNamespace().equals(ApprenticeCodex.MODID)
                            && (WEIGHT_ONE.contains(id.getPath()) || WEIGHT_TWO.contains(id.getPath()))
                            && bonus.getCount() == 1,
                    "Vault bonus must be one eligible curio");
            helper.assertTrue(ItemStack.isSameItemSameComponents(bonus, new ItemStack(bonus.getItem())),
                    "Vault bonus must preserve default item components");
            assertSameLoot(helper, ominous, roll(helper, "minecraft:chests/trial_chambers/reward_ominous"));
            enabled.set(false);
            assertSameLoot(helper, base, roll(helper, "irons_spellbooks:chests/catacombs/dead_king_vault"));
        } finally {
            enabled.set(previousEnabled);
            multiplier.set(previousMultiplier);
        }
        helper.succeed();
    }

    private static List<ItemStack> roll(GameTestHelper helper, String id) {
        // 素材の確定報酬とは独立に、従来のCurios追加だけを比較する。
        return rollAll(helper, id).stream().filter(stack -> !stack.is(ItemRegistry.MANA_ENVELOPED_SILVER_CHUNK.get())).toList();
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.curio_loot")
    public static void silverChunkVaultRewardsAndRecipe(GameTestHelper helper) throws IOException {
        ModConfigSpec.BooleanValue enabled = ApprenticeCodexServerConfig.SPEC.getValues().get("Loot.enableApprenticeCurioLoot");
        ModConfigSpec.DoubleValue multiplier = ApprenticeCodexServerConfig.SPEC.getValues().get("Loot.apprenticeCurioLootChanceMultiplier");
        boolean previousEnabled = enabled.get();
        double previousMultiplier = multiplier.get();
        try {
            for (boolean enable : List.of(false, true)) {
                enabled.set(enable);
                multiplier.set(0.0D);
                for (var id : List.of("minecraft:chests/trial_chambers/reward",
                        "minecraft:chests/trial_chambers/reward_ominous",
                        "irons_spellbooks:chests/catacombs/dead_king_vault",
                        "irons_spellbooks:chests/citadel/citadel_vault")) {
                    var all = rollAll(helper, id);
                    var chunks = all.stream().filter(stack -> stack.is(ItemRegistry.MANA_ENVELOPED_SILVER_CHUNK.get())).toList();
                    helper.assertTrue(chunks.size() == 1, "Vault must append exactly one silver chunk stack: " + id);
                    int count = chunks.getFirst().getCount();
                    helper.assertTrue(id.endsWith("/reward") ? count == 1 : count >= 2 && count <= 3,
                            "Unexpected silver chunk count: " + id);
                    var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(
                            ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(id)));
                    var params = new LootParams.Builder(helper.getLevel())
                            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                            .create(LootContextParamSets.VAULT);
                    var context = new net.minecraft.world.level.storage.loot.LootContext.Builder(params)
                            .withOptionalRandomSeed(12345L).create(java.util.Optional.empty());
                    var original = new java.util.ArrayList<ItemStack>();
                    table.getRandomItemsRaw(context, original::add);
                    // rawにはIron's自身のmodifierも含まれないため、元テーブルの報酬は先頭部分と比較する。
                    helper.assertTrue(all.size() >= original.size() + 1, "Base rewards and chunk bonus must be present");
                    assertSameLoot(helper, original, all.subList(0, original.size()));
                }
            }
            for (var id : List.of("minecraft:chests/simple_dungeon", "irons_spellbooks:chests/citadel/spawner_reward",
                    "irons_spellbooks:chests/ice_spider_den/spawner_reward")) {
                helper.assertTrue(rollAll(helper, id).stream().noneMatch(stack -> stack.is(ItemRegistry.MANA_ENVELOPED_SILVER_CHUNK.get())),
                        "Unrelated loot must not contain silver chunks: " + id);
            }
        } finally {
            enabled.set(previousEnabled);
            multiplier.set(previousMultiplier);
        }
        var resource = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "loot_table/chests/silver_chunk_special.json");
        try (var reader = helper.getLevel().getServer().getResourceManager().openAsReader(resource)) {
            var count = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonObject("count");
            helper.assertTrue(count.get("type").getAsString().equals("minecraft:uniform")
                    && count.get("min").getAsInt() == 2 && count.get("max").getAsInt() == 3, "Special vault counts must be uniformly two or three");
        }
        var stacks = new java.util.ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) stacks.add(new ItemStack(i == 4
                ? io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get() : ItemRegistry.MANA_ENVELOPED_SILVER_CHUNK.get()));
        var manager = helper.getLevel().getRecipeManager();
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, stacks);
        var recipe = manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "Silver ring crafting recipe must match");
        var output = recipe.orElseThrow().value().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(output.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()) && output.getCount() == 1,
                "Recipe must produce one original silver ring");
        stacks.set(0, ItemStack.EMPTY);
        helper.assertTrue(manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,
                net.minecraft.world.item.crafting.CraftingInput.of(3, 3, stacks), helper.getLevel()).isEmpty(), "Seven chunks must not craft a ring");
        stacks.set(0, new ItemStack(ItemRegistry.MANA_ENVELOPED_SILVER_CHUNK.get()));
        stacks.set(4, ItemStack.EMPTY);
        helper.assertTrue(manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,
                net.minecraft.world.item.crafting.CraftingInput.of(3, 3, stacks), helper.getLevel()).isEmpty(), "Mithril scrap must be required");
        helper.assertTrue(manager.getRecipeFor(RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get(),
                new SingleRecipeInput(new ItemStack(ItemRegistry.MANA_ENVELOPED_SILVER_CHUNK.get())), helper.getLevel()).isEmpty(),
                "Silver chunks must not have a recycling recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.curio_loot")
    public static void silverRingStillRecyclesOneScrap(GameTestHelper helper) {
        assertRecycling(helper, io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get(), 1, false);
    }

    private static List<ItemStack> rollAll(GameTestHelper helper, String id) {
        var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(id)));
        var params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .create(LootContextParamSets.VAULT);
        return table.getRandomItems(params, 12345L);
    }

    private static void assertSameLoot(GameTestHelper helper, List<ItemStack> expected, List<ItemStack> actual) {
        helper.assertTrue(expected.size() == actual.size(), "Existing loot stack count must be preserved");
        for (int i = 0; i < expected.size(); i++) {
            helper.assertTrue(ItemStack.matches(expected.get(i), actual.get(i)), "Existing reward changed at index " + i);
        }
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.curio_loot")
    public static void grindRunnerRecyclesManaManeuverGear(GameTestHelper helper) {
        assertRecycling(helper, ItemRegistry.MANA_MANEUVER_GEAR.get(), 2, false);
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.curio_loot")
    public static void grindRunnerRecyclesQuickcastCartridge(GameTestHelper helper) {
        assertRecycling(helper, ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get(), 2, false);
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.curio_loot")
    public static void grindRunnerRecyclesMonarchBondCharm(GameTestHelper helper) {
        assertRecycling(helper, ItemRegistry.MONARCH_BOND_CHARM.get(), 1, true);
    }

    private static void assertRecycling(GameTestHelper helper, Item input, int scraps, boolean rune) {
        var pos = new BlockPos(2, 2, 0);
        var owner = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper, pos, "curio_recycling_" + BuiltInRegistries.ITEM.getKey(input).getPath());
        helper.getLevel().addFreshEntity(owner);
        var wheel = new GrindRunnerWheelEntity(EntityRegistry.GRIND_RUNNER_WHEEL.get(), helper.getLevel(), owner);
        wheel.setGrindItemPerSecond(20.0F);
        helper.getLevel().addFreshEntity(wheel);
        var source = ApprenticeCodexGameTestScenarios.spawnNoGravityItem(helper, pos, new ItemStack(input));
        var bounds = source.getBoundingBox().inflate(3);
        helper.runAtTickTime(12, () -> {
            try {
                var outputs = new HashMap<Item, Integer>();
                for (var entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds)) {
                    outputs.merge(entity.getItem().getItem(), entity.getItem().getCount(), Integer::sum);
                }
                var expected = new HashMap<Item, Integer>();
                expected.put(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get(), scraps);
                if (rune) {
                    expected.put(io.redspace.ironsspellbooks.registries.ItemRegistry.EVOCATION_RUNE.get(), 1);
                }
                helper.assertTrue(outputs.equals(expected), "Recycling must consume one curio and produce exact materials: " + outputs);
            } finally {
                helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).forEach(ItemEntity::discard);
                source.discard();
                wheel.discard();
                owner.discard();
            }
            helper.succeed();
        });
    }
}
