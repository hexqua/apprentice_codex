package jp.aquafactory.apprenticecodex.gametest.spell.touchdig;

import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;

import static jp.aquafactory.apprenticecodex.gametest.spell.heavenlyfist.HeavenlyFistHarvestGameTests.equipRingCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.BlockDropTestSupport.getFreshItemDrops;
import static jp.aquafactory.apprenticecodex.gametest.support.BlockDropTestSupport.invokeTouchDigDestroyBlock;
import static jp.aquafactory.apprenticecodex.gametest.support.IsolationTestSupport.prepareMiningSpellIsolationArea;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class TouchDigHarvestScenarios {
    private TouchDigHarvestScenarios() {}

    static void touchDigIgnoresCraftsmansDelightRingEnchantments(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "touch_dig_ignores_ring_enchant_test");
            var heldTool = new ItemStack(Items.DIAMOND_PICKAXE);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldTool);

            var ringStack = new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get());
            ringStack.enchant(Enchantments.SILK_TOUCH, 1);
            equipRingCurio(player, ringStack);

            var blockPos = helper.absolutePos(new BlockPos(0, 12, 1));
            helper.getLevel().setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
            invokeTouchDigDestroyBlock(new TouchDigSpell(), helper.getLevel(), blockPos, player);

            var drops = getFreshItemDrops(helper.getLevel(), blockPos, 1.5D);
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Touch Dig should use the unenchanted main-hand pickaxe and ignore ring Silk Touch");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Touch Dig should not inherit Silk Touch from CraftsmansDelight");
            helper.succeed();
        });
    }
}
