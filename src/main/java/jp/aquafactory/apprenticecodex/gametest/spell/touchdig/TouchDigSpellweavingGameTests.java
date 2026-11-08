package jp.aquafactory.apprenticecodex.gametest.spell.touchdig;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumSpellweavingGameTestHelper;
import jp.aquafactory.apprenticecodex.gametest.support.MiningTestScope;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_MOD_ID;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createTrackedEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class TouchDigSpellweavingGameTests {
    private TouchDigSpellweavingGameTests() {}

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated", timeoutTicks = 300)
    public static void craftsmansDelightTouchDigActivatesPrimedSpellweavingPickaxeForDirt(GameTestHelper helper) {
        if (succeedWhenMalumIsMissing(helper)) {
            return;
        }
        helper.runAtTickTime(1, () -> {
            var scope = new MiningTestScope(helper);
            var context = prepareTouchDigSpellweavingTest(
                    helper, scope,
                    "touch_dig_spellweaving_dirt_test",
                    Blocks.DIRT.defaultBlockState(),
                    "spellweaving_pickaxe",
                    true,
                    true
            );
            castTouchDig(helper, context, 1, true);
            var activatorCount = MalumSpellweavingGameTestHelper.countActivators(
                    context.scope(), context.player());
            var activatorDiagnostics = MalumSpellweavingGameTestHelper.activatorDiagnostics(
                    helper.getLevel(), context.scope(), context.player());
            helper.assertTrue(activatorCount == 1,
                    "CraftsmansDelight Touch Dig should create one Spellweaving activator for adjacent dirt: "
                            + activatorCount + " [" + activatorDiagnostics.get() + "]");

            var directDrops = scope.drops(Items.DIRT);
            var removedDirectDirt = directDrops.stream()
                    .filter(entity -> entity.getItem().is(Items.DIRT))
                    .mapToInt(entity -> entity.getItem().getCount()).sum();
            helper.assertTrue(removedDirectDirt == 1, "The direct harvest must yield one owned dirt before testing adjacent drops");
            directDrops.forEach(ItemEntity::discard);

            helper.succeedWhen(() -> {
                helper.assertTrue(helper.getLevel().getBlockState(context.neighborPos()).isAir(),
                        "Spellweaving should destroy dirt adjacent to the successful Touch Dig target"
                                + " [tick=" + helper.getTick() + ", neighbor=" + context.neighborPos()
                                + ", state=" + helper.getLevel().getBlockState(context.neighborPos())
                                + ", " + activatorDiagnostics.get() + "]");
                var inventoryDrops = context.player().getInventory().countItem(Items.DIRT);
                var worldDrops = scope.count(Items.DIRT);
                helper.assertTrue(inventoryDrops + worldDrops > 0,
                        "Spellweaving should preserve a dirt drop when the held pickaxe is not the normal matching tool"
                                + " [tick=" + helper.getTick() + ", inventoryDirt=" + inventoryDrops
                                + ", worldDirt=" + worldDrops + ", removedDirectDirt=" + removedDirectDirt
                                + ", neighbor=" + context.neighborPos() + ", bounds=" + helper.getBounds()
                                + ", owned=" + scope.diagnostics()
                                + ", " + activatorDiagnostics.get() + "]");
            });
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void touchDigSpellweavingRequiresCraftsmansDelight(GameTestHelper helper) {
        if (succeedWhenMalumIsMissing(helper)) {
            return;
        }
        helper.runAtTickTime(1, () -> {
            var scope = new MiningTestScope(helper);
            var context = prepareTouchDigSpellweavingTest(
                    helper, scope,
                    "touch_dig_spellweaving_ring_test",
                    Blocks.DIRT.defaultBlockState(),
                    "spellweaving_pickaxe",
                    true,
                    false
            );
            castTouchDig(helper, context, 1, true);
            assertNoSpellweavingActivator(helper, context,
                    "Touch Dig should not bypass Spellweaving tool matching without CraftsmansDelight");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void touchDigSpellweavingRequiresPrimedPickaxe(GameTestHelper helper) {
        if (succeedWhenMalumIsMissing(helper)) {
            return;
        }
        helper.runAtTickTime(1, () -> {
            var scope = new MiningTestScope(helper);
            var context = prepareTouchDigSpellweavingTest(
                    helper, scope,
                    "touch_dig_spellweaving_primed_test",
                    Blocks.DIRT.defaultBlockState(),
                    "spellweaving_pickaxe",
                    false,
                    true
            );
            castTouchDig(helper, context, 1, true);
            assertNoSpellweavingActivator(helper, context,
                    "Touch Dig should not activate an unprimed Spellweaving Pickaxe");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void touchDigSpellweavingExcludesAxe(GameTestHelper helper) {
        if (succeedWhenMalumIsMissing(helper)) {
            return;
        }
        helper.runAtTickTime(1, () -> {
            var scope = new MiningTestScope(helper);
            var context = prepareTouchDigSpellweavingTest(
                    helper, scope,
                    "touch_dig_spellweaving_axe_test",
                    Blocks.DIRT.defaultBlockState(),
                    "spellweaving_axe",
                    true,
                    true
            );
            castTouchDig(helper, context, 1, true);
            assertNoSpellweavingActivator(helper, context,
                    "Touch Dig should not extend the tool mismatch bypass to Spellweaving Axe");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void touchDigSpellweavingDoesNotDuplicateNativeMatch(GameTestHelper helper) {
        if (succeedWhenMalumIsMissing(helper)) {
            return;
        }
        helper.runAtTickTime(1, () -> {
            var scope = new MiningTestScope(helper);
            var context = prepareTouchDigSpellweavingTest(
                    helper, scope,
                    "touch_dig_spellweaving_native_match_test",
                    Blocks.STONE.defaultBlockState(),
                    "spellweaving_pickaxe",
                    true,
                    true
            );
            castTouchDig(helper, context, 1, true);
            var activatorCount = MalumSpellweavingGameTestHelper.countActivators(
                    context.scope(), context.player());
            helper.assertTrue(activatorCount == 1,
                    "Touch Dig should keep the single native Spellweaving activation for pickaxe-mineable stone: "
                            + activatorCount);
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void touchDigSpellweavingKeepsTouchDigHarvestLimit(GameTestHelper helper) {
        if (succeedWhenMalumIsMissing(helper)) {
            return;
        }
        helper.runAtTickTime(1, () -> {
            var scope = new MiningTestScope(helper);
            var context = prepareTouchDigSpellweavingTest(
                    helper, scope,
                    "touch_dig_spellweaving_harvest_limit_test",
                    Blocks.OBSIDIAN.defaultBlockState(),
                    "spellweaving_pickaxe",
                    true,
                    true
            );
            castTouchDig(helper, context, 1, false);
            helper.assertTrue(helper.getLevel().getBlockState(context.targetPos()).is(Blocks.OBSIDIAN),
                    "Touch Dig level 1 should keep its iron harvest limit for obsidian");
            assertNoSpellweavingActivator(helper, context,
                    "Failed Touch Dig harvest should not activate Spellweaving");
            helper.succeed();
        });
    }

    private static TouchDigSpellweavingTestContext prepareTouchDigSpellweavingTest(
            GameTestHelper helper,
            MiningTestScope scope,
            String profileName,
            BlockState state,
            String toolId,
            boolean primed,
            boolean equipCraftsmansDelight
    ) {
        var playerPos = new BlockPos(4, 2, 4);

        // Malum の採掘 Entity は owner UUID を Level から引き直すため、完走検証では追跡済み FakePlayer が必要。
        var player = scope.track(createTrackedEquipmentTestPlayer(helper, playerPos, profileName));
        player.setYRot(0.0F);
        player.setXRot(-90.0F);
        player.setYHeadRot(0.0F);
        player.setYBodyRot(0.0F);
        if (equipCraftsmansDelight) {
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
        }
        MalumSpellweavingGameTestHelper.equipTool(player, toolId, primed);

        var targetPos = helper.absolutePos(new BlockPos(4, 5, 4));
        var neighborPos = targetPos.east();
        helper.getLevel().setBlock(targetPos, state, 3);
        helper.getLevel().setBlock(neighborPos, state, 3);
        scope.watchDrops(neighborPos);
        return new TouchDigSpellweavingTestContext(
                player,
                new TouchDigSpell(),
                MagicData.getPlayerMagicData(player),
                targetPos,
                neighborPos, scope
        );
    }

    private static void castTouchDig(
            GameTestHelper helper,
            TouchDigSpellweavingTestContext context,
            int spellLevel,
            boolean expectedPreCastResult
    ) {
        helper.assertTrue(context.magicData() != null,
                "Touch Dig Spellweaving test could not resolve player magic data");
        helper.assertTrue(context.spell().checkPreCastConditions(
                        helper.getLevel(), spellLevel, context.player(), context.magicData()) == expectedPreCastResult,
                "Touch Dig Spellweaving pre-cast result did not match the test setup");
        context.scope().run(() -> context.spell().onCast(
                helper.getLevel(), spellLevel, context.player(), CastSource.SPELLBOOK, context.magicData()));
    }

    private static void assertNoSpellweavingActivator(
            GameTestHelper helper,
            TouchDigSpellweavingTestContext context,
            String message
    ) {
        helper.assertTrue(MalumSpellweavingGameTestHelper.countActivators(
                        context.scope(), context.player()) == 0,
                message);
        helper.assertFalse(helper.getLevel().getBlockState(context.neighborPos()).isAir(),
                "Negative Spellweaving setup should leave its adjacent block unchanged");
    }

    private static boolean succeedWhenMalumIsMissing(GameTestHelper helper) {
        if (ModList.get().isLoaded(MALUM_MOD_ID)) {
            return false;
        }
        helper.succeed();
        return true;
    }

    private record TouchDigSpellweavingTestContext(
            FakePlayer player,
            TouchDigSpell spell,
            MagicData magicData,
            BlockPos targetPos,
            BlockPos neighborPos,
            MiningTestScope scope
    ) {
    }

    private static void equipRingCurio(ServerPlayer player, ItemStack ringStack) {
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for curio equip test"));
        curiosInventory.setEquippedCurio(Curios.RING_SLOT, 0, ringStack);
    }
}
