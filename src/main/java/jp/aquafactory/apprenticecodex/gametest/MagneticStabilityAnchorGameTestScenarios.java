package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.block.magneticstabilityanchor.MagneticStabilityAnchorBlock;
import jp.aquafactory.apprenticecodex.block.magneticstabilityanchor.MagneticStabilityAnchorBlockEntity;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

final class MagneticStabilityAnchorGameTestScenarios {
    private static final BlockPos ANCHOR_POS = new BlockPos(4, 2, 4);

    private MagneticStabilityAnchorGameTestScenarios() {
    }

    static void protectsItemsByPositionWithoutBlockingOwner(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.setBlock(ANCHOR_POS, BlockRegistry.MAGNETIC_STABILITY_ANCHOR.get());
        var owner = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper, ANCHOR_POS, "magnetic_anchor_owner_inside_test");
        var familiar = new AutoMagnetFamiliarEntity(
                EntityRegistry.AUTO_MAGNET_FAMILIAR.get(), level, owner, 6.0D, 0.0D);
        var insidePosition = helper.absoluteVec(new Vec3(6.999D, 2.5D, 4.5D));
        var outsidePosition = helper.absoluteVec(new Vec3(7.0D, 2.5D, 4.5D));
        var protectedItem = spawnNoGravityItem(level, insidePosition);
        var boundaryItem = spawnNoGravityItem(level, outsidePosition);
        level.addFreshEntity(owner);

        helper.runAtTickTime(1, () -> {
            familiar.tickOnServer(level);
            helper.assertTrue(protectedItem.position().distanceToSqr(insidePosition) < 0.001D,
                    "Magnetic Stability Anchor should protect an item just inside its maximum boundary");
            helper.assertTrue(boundaryItem.position().distanceToSqr(owner.position()) < 0.001D,
                    "Magnetic Stability Anchor maximum boundary should be exclusive");
            helper.succeed();
        });
    }

    static void removalRestoresItemCollectionAndExperienceRemainsUnaffected(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.setBlock(ANCHOR_POS, BlockRegistry.MAGNETIC_STABILITY_ANCHOR.get());
        var owner = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 4), "magnetic_anchor_removal_test");
        var familiar = new AutoMagnetFamiliarEntity(
                EntityRegistry.AUTO_MAGNET_FAMILIAR.get(), level, owner, 6.0D, 0.0D);
        // ブロックの衝突による押し出しを避けつつ、保護AABB内に置く。
        var protectedPosition = helper.absoluteVec(new Vec3(6.0D, 2.5D, 4.5D));
        var protectedItem = spawnNoGravityItem(level, protectedPosition);
        var orb = new ExperienceOrb(level, protectedPosition.x, protectedPosition.y, protectedPosition.z + 0.5D, 3);
        orb.setNoGravity(true);
        level.addFreshEntity(orb);
        level.addFreshEntity(owner);

        helper.runAtTickTime(1, () -> {
            var positionBeforeCollection = protectedItem.position();
            familiar.tickOnServer(level);
            helper.assertTrue(protectedItem.position().distanceToSqr(positionBeforeCollection) < 0.001D,
                    "Magnetic Stability Anchor should leave protected item entities in place");
            helper.assertTrue(orb.position().distanceToSqr(owner.position()) < 0.001D,
                    "Magnetic Stability Anchor should not affect experience orb collection");

            helper.setBlock(ANCHOR_POS, Blocks.AIR);
            familiar.tickOnServer(level);
            helper.assertTrue(protectedItem.position().distanceToSqr(owner.position()) < 0.001D,
                    "Removing Magnetic Stability Anchor should immediately restore item collection");
            helper.succeed();
        });
    }

    static void supportsWaterloggingAndAlwaysDropsPlainItem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var absolutePos = helper.absolutePos(ANCHOR_POS);
            var anchor = (MagneticStabilityAnchorBlock) BlockRegistry.MAGNETIC_STABILITY_ANCHOR.get();
            level.setBlockAndUpdate(absolutePos, anchor.defaultBlockState());
            var blockEntity = level.getBlockEntity(absolutePos);
            helper.assertTrue(blockEntity instanceof MagneticStabilityAnchorBlockEntity,
                    "Magnetic Stability Anchor should create its block entity");
            helper.assertTrue(anchor.placeLiquid(
                            level, absolutePos, level.getBlockState(absolutePos), Fluids.WATER.getSource(false)),
                    "Magnetic Stability Anchor should accept source water");

            var state = level.getBlockState(absolutePos);
            helper.assertTrue(state.getValue(MagneticStabilityAnchorBlock.WATERLOGGED),
                    "Magnetic Stability Anchor should become waterlogged");
            helper.assertTrue(state.getFluidState().isSource(),
                    "Waterlogged Magnetic Stability Anchor should retain source water");
            helper.assertTrue(level.getBlockEntity(absolutePos) == blockEntity,
                    "Waterlogging should preserve the Magnetic Stability Anchor block entity");

            var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                    helper, new BlockPos(1, 2, 1), "magnetic_anchor_drop_test");
            player.setOnGround(true);
            var bareHandProgress = state.getDestroyProgress(player, level, absolutePos);
            helper.assertTrue(bareHandProgress >= 0.045F && bareHandProgress <= 0.055F,
                    "Magnetic Stability Anchor should take about 20 ticks to break by hand");
            assertPlainDrop(helper, state, level.getBlockEntity(absolutePos), player, ItemStack.EMPTY);

            var pickaxe = new ItemStack(Items.IRON_PICKAXE);
            player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
            helper.assertTrue(state.getDestroyProgress(player, level, absolutePos) > bareHandProgress,
                    "A pickaxe should break Magnetic Stability Anchor faster than a bare hand");
            assertPlainDrop(helper, state, level.getBlockEntity(absolutePos), player, pickaxe);
        });
    }

    private static ItemEntity spawnNoGravityItem(net.minecraft.server.level.ServerLevel level, Vec3 position) {
        var item = new ItemEntity(level, position.x, position.y, position.z, new ItemStack(Items.IRON_INGOT));
        item.setNoGravity(true);
        item.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(item);
        return item;
    }

    private static void assertPlainDrop(GameTestHelper helper, net.minecraft.world.level.block.state.BlockState state,
                                        net.minecraft.world.level.block.entity.BlockEntity blockEntity,
                                        net.minecraft.world.entity.player.Player player, ItemStack tool) {
        var drops = Block.getDrops(state, helper.getLevel(), blockEntity.getBlockPos(), blockEntity, player, tool);
        var expected = new ItemStack(ItemRegistry.MAGNETIC_STABILITY_ANCHOR.get());
        helper.assertTrue(drops.size() == 1 && drops.get(0).getCount() == 1
                        && ItemStack.isSameItemSameTags(drops.get(0), expected),
                "Magnetic Stability Anchor should always drop one plain, stackable block item");
    }
}
