package jp.aquafactory.apprenticecodex.spell.tinylumberjack;

import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class TinyLumberjackBlockClassifier {
    private TinyLumberjackBlockClassifier() {
    }

    public static boolean isLog(BlockState state) {
        return state.is(BlockTags.LOGS)
                || state.is(TagRegistry.Blocks.TINY_LUMBERJACK_FORCED_LOGS)
                || MalumCompatibility.isRunewoodOrSoulwoodLog(state);
    }

    public static boolean isBreakableLeaf(BlockState state) {
        if (state.is(TagRegistry.Blocks.TINY_LUMBERJACK_FORCED_LEAVES)) {
            return true;
        }

        if (!state.is(BlockTags.LEAVES)) {
            return false;
        }

        return !state.hasProperty(LeavesBlock.PERSISTENT) || !state.getValue(LeavesBlock.PERSISTENT);
    }
}
