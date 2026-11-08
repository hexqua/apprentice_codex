package jp.aquafactory.apprenticecodex.gametest.equipment.craftsmansdelight;

import io.redspace.ironsspellbooks.entity.spells.spectral_hammer.SpectralHammer;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static jp.aquafactory.apprenticecodex.gametest.spell.heavenlyfist.HeavenlyFistHarvestGameTests.equipRingCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.BlockDropTestSupport.getFreshItemDrops;
import static jp.aquafactory.apprenticecodex.gametest.support.IsolationTestSupport.prepareMiningSpellIsolationArea;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class CraftsmansDelightMiningToolScenarios {
    private CraftsmansDelightMiningToolScenarios() {}

    static void spectralHammerCopiesCurrentMainHandWithCraftsmansDelight(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "spectral_hammer_main_hand_copy_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var unenchantedTool = new ItemStack(Items.IRON_PICKAXE);
            unenchantedTool.setDamageValue(7);
            player.setItemInHand(InteractionHand.MAIN_HAND, unenchantedTool);
            var copiedTool = CraftsmansDelight.createSpectralHammerTool(player);
            helper.assertTrue(copiedTool.is(Items.IRON_PICKAXE) && copiedTool.getDamageValue() == 7,
                    "Spectral Hammer should copy an unenchanted main-hand tool while CraftsmansDelight is equipped");
            copiedTool.setDamageValue(8);
            helper.assertTrue(unenchantedTool.getDamageValue() == 7,
                    "Spectral Hammer should return an independent copy of the main-hand tool");

            var silkTool = new ItemStack(Items.DIAMOND_PICKAXE);
            silkTool.enchant(Enchantments.SILK_TOUCH, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, silkTool);

            var targetPos = helper.absolutePos(new BlockPos(0, 12, 2));
            helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);

            var hammer = new SpectralHammer(
                    helper.getLevel(),
                    player,
                    new BlockHitResult(Vec3.atCenterOf(targetPos), Direction.NORTH, targetPos, false),
                    0,
                    1
            );
            var hammerPos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 12, 1)));
            hammer.setPos(hammerPos.x, hammerPos.y, hammerPos.z);
            helper.getLevel().addFreshEntity(hammer);

            for (var tick = 0; tick < 20 && !hammer.isRemoved(); tick++) {
                hammer.tick();
            }

            var drops = getFreshItemDrops(helper.getLevel(), targetPos, 2.0D);
            helper.assertTrue(drops.stream().anyMatch(itemEntity -> itemEntity.getItem().is(Blocks.STONE.asItem())),
                    "Spectral Hammer should apply main-hand Silk Touch while CraftsmansDelight is equipped");
            helper.assertTrue(drops.stream().noneMatch(itemEntity -> itemEntity.getItem().is(Blocks.COBBLESTONE.asItem())),
                    "Spectral Hammer with main-hand Silk Touch should not drop cobblestone");
            helper.succeed();
        });
    }
}
