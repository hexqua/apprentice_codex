package jp.aquafactory.apprenticecodex.gametest.spell.tinylumberjack;

import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.support.MiningTestScope;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjackJob;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class TinyLumberjackDropGameTests {
    private TinyLumberjackDropGameTests() {}

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void tinyLumberjackWithCraftsmansDelightMovesJobDropsToOrigin(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var scope = new MiningTestScope(helper);
            var level = helper.getLevel();
            var playerPos = new BlockPos(4, 2, 4);

            var player = scope.track(createEquipmentTestPlayer(helper, playerPos, "tiny_lumberjack_drop_move_test"));
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var originPos = helper.absolutePos(new BlockPos(5, 2, 5));
            var logPos = originPos.above();
            level.setBlock(originPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState(), 3);

            var existingItemPos = Vec3.atCenterOf(logPos);
            var existingItem = new ItemEntity(
                    level,
                    existingItemPos.x,
                    existingItemPos.y,
                    existingItemPos.z,
                    new ItemStack(Items.COBBLESTONE)
            );
            scope.track(existingItem);
            level.addFreshEntity(existingItem);

            var job = new TinyLumberjackJob(originPos, 1, player);
            scope.run(() -> job.tick(level));
            helper.assertTrue(level.getBlockState(logPos).isAir(), "Job must break its own log");

            helper.assertTrue(scope.drops(Items.OAK_LOG).stream().anyMatch(drop -> drop.position().distanceToSqr(Vec3.atCenterOf(originPos)) < 0.0625D),
                    "Tiny Lumberjack should move new log drops to the initial chopped block while CraftsmansDelight is equipped");
            helper.assertTrue(!existingItem.isRemoved() && existingItem.position().distanceToSqr(existingItemPos) < 0.01D,
                    "Tiny Lumberjack drop moving should not move ItemEntities that existed before the block break");
            helper.assertTrue(scope.count(Items.OAK_LOG) == 1, "Breaking one log must preserve exactly one owned drop");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void tinyLumberjackDropMoveFollowsCurrentCraftsmansDelightEquipment(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var scope = new MiningTestScope(helper);
            var level = helper.getLevel();
            var playerPos = new BlockPos(4, 2, 4);

            var player = scope.track(createEquipmentTestPlayer(helper, playerPos, "tiny_lumberjack_drop_move_unequip_test"));
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var originPos = helper.absolutePos(new BlockPos(5, 2, 5));
            var logPos = originPos.above();
            level.setBlock(originPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState(), 3);

            var job = new TinyLumberjackJob(originPos, 1, player);
            equipRingCurio(player, ItemStack.EMPTY);
            scope.run(() -> job.tick(level));
            helper.assertTrue(level.getBlockState(logPos).isAir(), "Job must break its own log");

            helper.assertFalse(scope.drops(Items.OAK_LOG).stream().anyMatch(drop -> drop.position().distanceToSqr(Vec3.atCenterOf(originPos)) < 0.0625D),
                    "Tiny Lumberjack should stop moving job drops after CraftsmansDelight is unequipped");
            helper.assertTrue(scope.drops(Items.OAK_LOG).stream().anyMatch(drop -> drop.position().distanceToSqr(Vec3.atCenterOf(logPos)) < 1.5625D),
                    "Tiny Lumberjack should leave log drops near the broken block when CraftsmansDelight is not currently equipped");
            helper.assertTrue(scope.count(Items.OAK_LOG) == 1, "Breaking one log must preserve exactly one owned drop");
            helper.succeed();
        });
    }

    private static void equipRingCurio(ServerPlayer player, ItemStack ringStack) {
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for curio equip test"));
        curiosInventory.setEquippedCurio(Curios.RING_SLOT, 0, ringStack);
    }
}
