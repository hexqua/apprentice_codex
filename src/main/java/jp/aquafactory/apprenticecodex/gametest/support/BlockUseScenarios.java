package jp.aquafactory.apprenticecodex.gametest.support;

import jp.aquafactory.apprenticecodex.utility.BlockTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class BlockUseScenarios {
    private BlockUseScenarios() {}

    static void blockToolsTemporaryUseKeepsOneCountStackAndHands(GameTestHelper helper) {
        var placePos = new BlockPos(2, 3, 2);
        var player = createEquipmentTestPlayer(helper, new BlockPos(1, 4, 1), "block_tools_temporary_use_test");
        var originalMainHand = new ItemStack(Items.STICK);
        var inventoryDirt = new ItemStack(Items.DIRT, 32);
        var virtualDirt = new ItemStack(Items.DIRT);

        helper.setBlock(placePos.below(), Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand.copy());
        player.getInventory().setItem(9, inventoryDirt.copy());

        helper.runAtTickTime(1, () -> {
            var effectiveVirtualDirt = BlockTools.copyForTemporaryUse(virtualDirt);
            helper.assertTrue(
                    effectiveVirtualDirt.is(Items.DIRT) && effectiveVirtualDirt.getCount() == 2,
                    "Temporary stack normalization should leave one item after a single placement"
            );
            helper.assertTrue(
                    virtualDirt.is(Items.DIRT) && virtualDirt.getCount() == 1,
                    "Temporary stack normalization should not mutate its source"
            );

            var result = BlockTools.useItemOnBlockByPlayerMainHand(
                    helper.getLevel(),
                    player,
                    helper.absolutePos(placePos),
                    virtualDirt,
                    Direction.UP
            );

            helper.assertTrue(result.consumesAction(), "Temporary dirt use should consume action");
            helper.assertBlockPresent(Blocks.DIRT, placePos);
            helper.assertTrue(
                    ItemStack.isSameItemSameTags(player.getMainHandItem(), originalMainHand)
                            && player.getMainHandItem().getCount() == originalMainHand.getCount(),
                    "Temporary use should restore original main hand"
            );
            helper.assertTrue(
                    virtualDirt.is(Items.DIRT) && virtualDirt.getCount() == 1,
                    "Temporary one-count stack should not be consumed"
            );
            var storedDirt = player.getInventory().getItem(9);
            helper.assertTrue(
                    storedDirt.is(Items.DIRT) && storedDirt.getCount() == inventoryDirt.getCount(),
                    "Inventory dirt should stay in its original slot"
            );
            helper.succeed();
        });
    }
}
