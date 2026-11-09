package jp.aquafactory.apprenticecodex.gametest.spell.autoturret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;

import static jp.aquafactory.apprenticecodex.gametest.spell.autoturret.AutoTurretTestSupport.castAutoTurret;
import static jp.aquafactory.apprenticecodex.gametest.spell.autoturret.AutoTurretTestSupport.getSingleLivingAutoTurret;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class AutoTurretPlacementScenarios {
    private AutoTurretPlacementScenarios() {}

    static void autoTurretCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_slab_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE_SLAB.defaultBlockState()
                    .setValue(SlabBlock.TYPE, SlabType.BOTTOM));

            castAutoTurret(helper, owner, 1, anchorPos);

            var turret = getSingleLivingAutoTurret(helper, owner);
            var expectedY = helper.absolutePos(anchorPos).below().getY() + 0.5D;
            helper.assertTrue(Math.abs(turret.getY() - expectedY) < 0.01D,
                    "AutoTurret should sit on the slab support top: " + turret.getY());
        });
    }

    static void autoTurretCanBePlacedOnSupportedStairs(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_stairs_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, Direction.NORTH)
                    .setValue(StairBlock.HALF, Half.BOTTOM)
                    .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT));

            castAutoTurret(helper, owner, 1, anchorPos);

            var turret = getSingleLivingAutoTurret(helper, owner);
            var expectedY = helper.absolutePos(anchorPos).getY();
            helper.assertTrue(Math.abs(turret.getY() - expectedY) < 0.01D,
                    "AutoTurret should accept a normal stair as support: " + turret.getY());
        });
    }

    static void autoTurretFallsWhenSupportRemoved(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 6, 0), "auto_turret_fall_test");
        var anchorPos = new BlockPos(0, 6, 0);
        helper.setBlock(anchorPos.below(), Blocks.STONE);
        helper.getLevel().addFreshEntity(owner);
        castAutoTurret(helper, owner, 1, anchorPos);
        var turret = getSingleLivingAutoTurret(helper, owner);
        var initialY = turret.getY();

        helper.runAtTickTime(1, () -> helper.setBlock(anchorPos.below(), Blocks.AIR));

        helper.succeedWhen(() -> {
            helper.assertTrue(turret.isAlive() && !turret.isRemoved(),
                    "AutoTurret should remain alive while falling after support removal");
            helper.assertTrue(turret.getY() < initialY - 0.05D,
                    "AutoTurret should fall below its anchored position after support removal: " + turret.getY());
        });
    }
}
