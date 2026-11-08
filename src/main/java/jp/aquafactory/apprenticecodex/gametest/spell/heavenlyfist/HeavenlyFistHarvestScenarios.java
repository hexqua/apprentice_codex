package jp.aquafactory.apprenticecodex.gametest.spell.heavenlyfist;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.spell.heavenlyfist.HeavenlyFistHarvestGameTests.*;
import static jp.aquafactory.apprenticecodex.gametest.support.BlockDropTestSupport.hasItemEntityWithin;
import static jp.aquafactory.apprenticecodex.gametest.support.IsolationTestSupport.prepareMiningSpellIsolationArea;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class HeavenlyFistHarvestScenarios {
    private HeavenlyFistHarvestScenarios() {}

    static void heavenlyFistWithCraftsmansDelightHarvestsSilkTouchedBuddingCrystal(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createServerEquipmentTestPlayer(helper, playerPos);
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var heldTool = new ItemStack(Items.DIAMOND_PICKAXE);
            heldTool.enchant(Enchantments.SILK_TOUCH, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldTool);

            var sourcePos = helper.absolutePos(new BlockPos(1, 12, 1));
            var clusterPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            spawnHeavenlyFist(level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).isAir(),
                        "Heavenly Fist with CraftsmansDelight should harvest mature crystals growing from budding amethyst");
                helper.assertTrue(level.getBlockState(sourcePos).is(Blocks.BUDDING_AMETHYST),
                        "Heavenly Fist with CraftsmansDelight should leave budding amethyst intact");
                helper.assertTrue(hasItemEntityWithin(level, Blocks.AMETHYST_CLUSTER.asItem(), Vec3.atCenterOf(clusterPos), 1.5D),
                        "Heavenly Fist with main-hand Silk Touch should drop the crystal block itself");
                helper.succeed();
            });
        });
    }

    static void heavenlyFistWithUnresolvedOwnerLeavesBuddingCrystal(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "heavenly_fist_unresolved_owner_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var sourcePos = helper.absolutePos(new BlockPos(1, 12, 1));
            var clusterPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            spawnHeavenlyFist(level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).is(Blocks.AMETHYST_CLUSTER),
                        "Heavenly Fist should not harvest crystals when its owner cannot resolve to an online player");
                helper.succeed();
            });
        });
    }

    static void heavenlyFistInAdventureModeLeavesBuddingCrystal(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createServerEquipmentTestPlayer(helper, playerPos);
            player.gameMode.changeGameModeForPlayer(GameType.ADVENTURE);

            var sourcePos = helper.absolutePos(new BlockPos(1, 12, 1));
            var clusterPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            spawnHeavenlyFist(level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).is(Blocks.AMETHYST_CLUSTER),
                        "Heavenly Fist should respect Adventure mode block restrictions");
                helper.succeed();
            });
        });
    }

    static void heavenlyFistCanceledBreakEventLeavesBuddingCrystal(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createServerEquipmentTestPlayer(helper, playerPos);

            var sourcePos = helper.absolutePos(new BlockPos(1, 12, 1));
            var clusterPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);
            Consumer<BlockEvent.BreakEvent> cancelListener = event -> {
                if (event.getPlayer() == player && event.getPos().equals(clusterPos)) {
                    event.setCanceled(true);
                }
            };
            MinecraftForge.EVENT_BUS.addListener(cancelListener);

            spawnHeavenlyFist(level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                try {
                    helper.assertTrue(level.getBlockState(clusterPos).is(Blocks.AMETHYST_CLUSTER),
                            "Heavenly Fist should respect canceled block break events");
                    helper.succeed();
                } finally {
                    MinecraftForge.EVENT_BUS.unregister(cancelListener);
                }
            });
        });
    }

    static void heavenlyFistWithoutCraftsmansDelightHarvestsWithoutEnchantments(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createServerEquipmentTestPlayer(helper, playerPos);
            var heldTool = new ItemStack(Items.DIAMOND_PICKAXE);
            heldTool.enchant(Enchantments.SILK_TOUCH, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldTool);

            var sourcePos = helper.absolutePos(new BlockPos(1, 12, 1));
            var clusterPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            spawnHeavenlyFist(level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).isAir(),
                        "Heavenly Fist without CraftsmansDelight should harvest mature crystals");
                helper.assertTrue(level.getBlockState(sourcePos).is(Blocks.BUDDING_AMETHYST),
                        "Heavenly Fist without CraftsmansDelight should leave budding amethyst intact");
                helper.assertTrue(hasItemEntityWithin(level, Items.AMETHYST_SHARD, Vec3.atCenterOf(clusterPos), 1.5D),
                        "Heavenly Fist without CraftsmansDelight should drop amethyst shards despite main-hand Silk Touch");
                helper.assertFalse(hasItemEntityWithin(level, Blocks.AMETHYST_CLUSTER.asItem(), Vec3.atCenterOf(clusterPos), 1.5D),
                        "Heavenly Fist without CraftsmansDelight should not copy main-hand Silk Touch");
                helper.succeed();
            });
        });
    }

    static void heavenlyFistSkipsCrystalNotGrowingFromHarvestSource(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createServerEquipmentTestPlayer(helper, playerPos);

            var supportPos = helper.absolutePos(new BlockPos(1, 12, 1));
            var clusterPos = supportPos.east();
            level.setBlock(supportPos, Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            spawnHeavenlyFist(level, player, Vec3.atCenterOf(supportPos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).is(Blocks.AMETHYST_CLUSTER),
                        "Heavenly Fist without CraftsmansDelight should skip crystals not attached to harvest sources");
                helper.succeed();
            });
        });
    }

    static void heavenlyFistSkipsImmatureAmethystBuds(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createServerEquipmentTestPlayer(helper, playerPos);

            var sourcePos = helper.absolutePos(new BlockPos(1, 12, 1));
            var budPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(budPos, Blocks.LARGE_AMETHYST_BUD.defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, Direction.EAST), 3);

            spawnHeavenlyFist(level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(budPos).is(Blocks.LARGE_AMETHYST_BUD),
                        "Heavenly Fist without CraftsmansDelight should skip immature amethyst buds");
                helper.succeed();
            });
        });
    }
}
