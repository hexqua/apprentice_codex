package jp.aquafactory.apprenticecodex.spell.earthforge;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

final class EarthForgePlacementRules {
    private EarthForgePlacementRules() {
    }

    static boolean canReplaceWithDirt(BlockState state) {
        var fluidState = state.getFluidState();
        if (!fluidState.isEmpty()) {
            // FluidState だけで許可すると waterlogged の非置換ブロックまで壊すため、
            // 水タグかつ液体本体、または水中植物のような置換可能ブロックだけを対象にする。
            return fluidState.is(FluidTags.WATER)
                    && (state.getBlock() instanceof LiquidBlock || state.canBeReplaced());
        }

        return state.isAir() || state.canBeReplaced();
    }
}
