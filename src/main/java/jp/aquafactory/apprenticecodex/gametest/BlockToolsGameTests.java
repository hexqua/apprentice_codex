package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class BlockToolsGameTests {
    private BlockToolsGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void nestedBreaksRestoreHeldContextAfterCancellationAndException(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(1, 2, 1), "block_tools_context");
        var otherPlayer = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(2, 2, 1), "block_tools_other");
        otherPlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var outerPos = helper.absolutePos(new BlockPos(1, 3, 1));
        var innerPos = outerPos.east();
        var unrelatedPos = innerPos.east();
        var held = new ItemStack(ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get());
        var outerTool = new ItemStack(Items.DIAMOND_PICKAXE);
        var innerTool = new ItemStack(Items.IRON_PICKAXE);
        for (boolean fail : new boolean[]{false, true}) {
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            level.setBlockAndUpdate(outerPos, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(innerPos, Blocks.STONE.defaultBlockState());
            var failure = new IllegalStateException("Expected failure to verify block break context cleanup");
            int[] calls = {0};
            Consumer<BlockEvent.BreakEvent> listener = event -> {
                if (event.getPlayer() != player) return;
                calls[0]++;
                helper.assertTrue(BlockTools.getMainHandForBlockBreak(player, level, event.getPos()) == held,
                        "Nested breaks must retain the original held item instead of a temporary tool");
                helper.assertTrue(BlockTools.getMainHandForBlockBreak(otherPlayer, level, event.getPos()).isEmpty(),
                        "Break contexts must not affect another player");
                helper.assertTrue(BlockTools.getMainHandForBlockBreak(player, level, unrelatedPos) == player.getMainHandItem(),
                        "Break contexts must not affect an unrelated position");
                if (event.getPos().equals(innerPos)) {
                    event.setCanceled(true);
                    return;
                }
                helper.assertTrue(!BlockTools.tryBreakBlockByPlayerHands(level, player, innerPos, innerTool),
                        "Canceled nested breaks must report failure");
                helper.assertTrue(player.getMainHandItem() == outerTool
                                && BlockTools.getMainHandForBlockBreak(player, level, outerPos) == held
                                && BlockTools.getMainHandForBlockBreak(player, level, innerPos) == outerTool,
                        "Returning from a nested break must restore the outer tool and context");
                // イベント中の例外でも finally による手持ち・参照元の復元を保証する。
                if (fail) throw failure;
                event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try {
                try {
                    boolean broken = BlockTools.tryBreakBlockByPlayerHands(level, player, outerPos, outerTool);
                    helper.assertTrue(!fail && !broken, "Canceled breaks must fail and injected exceptions must propagate");
                } catch (IllegalStateException exception) {
                    if (exception != failure) throw exception;
                }
                helper.assertTrue(calls[0] == 2 && player.getMainHandItem() == held,
                        "Both break callbacks must run and the original hand must be restored");
                helper.assertTrue(level.getBlockState(outerPos).is(Blocks.STONE)
                                && level.getBlockState(innerPos).is(Blocks.STONE),
                        "Canceled or failed breaks must leave both blocks intact");
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                helper.assertTrue(BlockTools.getMainHandForBlockBreak(player, level, outerPos).isEmpty()
                                && BlockTools.getMainHandForBlockBreak(player, level, innerPos).isEmpty(),
                        "Completed, canceled, or failed breaks must not leak held contexts");
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
            }
        }
        helper.succeed();
    }
}
