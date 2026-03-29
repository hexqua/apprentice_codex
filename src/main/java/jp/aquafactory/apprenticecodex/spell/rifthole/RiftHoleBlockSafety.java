package jp.aquafactory.apprenticecodex.spell.rifthole;

import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public final class RiftHoleBlockSafety {
    private RiftHoleBlockSafety() {
    }

    public static boolean isTargetBlock(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        // ガラスのような full block 相当は通しつつ、階段などの非直方体は避ける。
        return passesCommonChecks(level, pos, state) && isTunnelFullBlock(level, pos, state);
    }

    public static boolean canReplace(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        // 復元時の事故を避けつつ、ガラスのような full block は許可する。
        return passesCommonChecks(level, pos, state)
                && isTunnelFullBlock(level, pos, state)
                && !hasUnsafeRestoreBehavior(level, pos, state);
    }

    private static boolean passesCommonChecks(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.canBeReplaced()) {
            return false;
        }
        // 開始地点は詠唱失敗、途中は部分トンネルになる既存仕様に合わせて、
        // データパックから追加した拒否対象も共通の安全判定へ寄せる。
        if (state.is(TagRegistry.Blocks.RIFT_HOLE_TUNNEL_DENYLIST)) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        return state.getDestroySpeed(level, pos) >= 0.0f;
    }

    private static boolean isTunnelFullBlock(Level level, BlockPos pos, BlockState state) {
        return Block.isShapeFullBlock(state.getCollisionShape(level, pos));
    }

    private static boolean hasUnsafeRestoreBehavior(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) != null) {
            return true;
        }
        if (state.getPistonPushReaction() != PushReaction.NORMAL) {
            return true;
        }
        return state.hasAnalogOutputSignal() || state.isSignalSource();
    }
}
