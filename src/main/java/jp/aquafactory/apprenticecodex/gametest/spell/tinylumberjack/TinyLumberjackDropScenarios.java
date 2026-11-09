package jp.aquafactory.apprenticecodex.gametest.spell.tinylumberjack;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjackJob;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static jp.aquafactory.apprenticecodex.gametest.spell.heavenlyfist.HeavenlyFistHarvestGameTests.equipRingCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.BlockDropTestSupport.hasItemEntityWithin;
import static jp.aquafactory.apprenticecodex.gametest.support.IsolationTestSupport.prepareMiningSpellIsolationArea;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class TinyLumberjackDropScenarios {
    private TinyLumberjackDropScenarios() {}

    static void tinyLumberjackWithCraftsmansDelightMovesJobDropsToOrigin(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "tiny_lumberjack_drop_move_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var originPos = helper.absolutePos(new BlockPos(1, 12, 1));
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
            level.addFreshEntity(existingItem);

            var job = new TinyLumberjackJob(originPos, 1, player);
            job.tick(level);

            helper.assertTrue(hasItemEntityWithin(level, Items.OAK_LOG, Vec3.atCenterOf(originPos), 0.25D),
                    "Tiny Lumberjack should move new log drops to the initial chopped block while CraftsmansDelight is equipped");
            helper.assertTrue(!existingItem.isRemoved() && existingItem.position().distanceToSqr(existingItemPos) < 0.01D,
                    "Tiny Lumberjack drop moving should not move ItemEntities that existed before the block break");
            helper.succeed();
        });
    }

    static void tinyLumberjackDropMoveFollowsCurrentCraftsmansDelightEquipment(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "tiny_lumberjack_drop_move_unequip_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var originPos = helper.absolutePos(new BlockPos(1, 12, 1));
            var logPos = originPos.above();
            level.setBlock(originPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState(), 3);

            var job = new TinyLumberjackJob(originPos, 1, player);
            equipRingCurio(player, ItemStack.EMPTY);
            job.tick(level);

            helper.assertFalse(hasItemEntityWithin(level, Items.OAK_LOG, Vec3.atCenterOf(originPos), 0.25D),
                    "Tiny Lumberjack should stop moving job drops after CraftsmansDelight is unequipped");
            helper.assertTrue(hasItemEntityWithin(level, Items.OAK_LOG, Vec3.atCenterOf(logPos), 1.25D),
                    "Tiny Lumberjack should leave log drops near the broken block when CraftsmansDelight is not currently equipped");
            helper.succeed();
        });
    }
}
