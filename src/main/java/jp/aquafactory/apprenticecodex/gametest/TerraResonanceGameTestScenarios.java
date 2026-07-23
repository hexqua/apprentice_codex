package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.spell.terraresonance.TerraResonanceSearch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

final class TerraResonanceGameTestScenarios {
    private TerraResonanceGameTestScenarios() {
    }

    static void targetTagIncludesSourcesClustersAndImmatureAmethyst(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(
                Blocks.BUDDING_AMETHYST.defaultBlockState().is(TagRegistry.Blocks.TERRA_RESONANCE_TARGETS),
                "Terra Resonance target tag should inherit Heavenly Fist crystal growth sources"
        );
        helper.assertTrue(
                Blocks.AMETHYST_CLUSTER.defaultBlockState().is(TagRegistry.Blocks.TERRA_RESONANCE_TARGETS),
                "Terra Resonance target tag should inherit Heavenly Fist harvest targets"
        );
        helper.assertTrue(
                Blocks.SMALL_AMETHYST_BUD.defaultBlockState().is(TagRegistry.Blocks.TERRA_RESONANCE_TARGETS)
                        && Blocks.MEDIUM_AMETHYST_BUD.defaultBlockState().is(TagRegistry.Blocks.TERRA_RESONANCE_TARGETS)
                        && Blocks.LARGE_AMETHYST_BUD.defaultBlockState().is(TagRegistry.Blocks.TERRA_RESONANCE_TARGETS),
                "Terra Resonance target tag should include every immature vanilla amethyst stage"
        );
        helper.succeed();
    }

    static void searchExtendsInwardFromEverySelectedFaceAndKeepsSnapshot(GameTestHelper helper) {
        var level = helper.getLevel();
        var anchor = helper.absolutePos(new BlockPos(8, 8, 8));
        var range = 5;

        for (var selectedFace : Direction.values()) {
            var inwardTarget = anchor.relative(selectedFace.getOpposite(), range - 1);
            var outwardTarget = anchor.relative(selectedFace);
            level.setBlock(inwardTarget, Blocks.SMALL_AMETHYST_BUD.defaultBlockState(), 3);
            level.setBlock(outwardTarget, Blocks.SMALL_AMETHYST_BUD.defaultBlockState(), 3);

            var result = TerraResonanceSearch.collect(level, anchor, selectedFace, range);
            helper.assertTrue(result.found(), "Terra Resonance should find a target in the inward search volume");
            helper.assertTrue(
                    result.highlightTargets().contains(inwardTarget),
                    "Terra Resonance should include the inward boundary for face " + selectedFace
            );
            helper.assertFalse(
                    result.highlightTargets().contains(outwardTarget),
                    "Terra Resonance should exclude the outward side for face " + selectedFace
            );

            level.setBlock(inwardTarget, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(outwardTarget, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(
                    result.highlightTargets().contains(inwardTarget),
                    "Terra Resonance should keep its cast-completion snapshot after a target is removed"
            );
        }

        var emptyResult = TerraResonanceSearch.collect(level, anchor, Direction.UP, range);
        helper.assertFalse(emptyResult.found(), "Terra Resonance should report not found when no tagged block remains");
        helper.succeed();
    }
}
