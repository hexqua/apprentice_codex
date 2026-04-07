package jp.aquafactory.apprenticecodex.compat.lootr;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

public final class LootrTreasureDivinationCompatBridge {
    private LootrTreasureDivinationCompatBridge() {
    }

    public static boolean shouldIgnoreOpenedTarget(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        if (!ModList.get().isLoaded(LootrTreasureDivinationCompat.MOD_ID) || !isLootrBlock(state)) {
            return false;
        }

        return LootrTreasureDivinationCompat.shouldIgnoreOpenedTarget(level, player, pos);
    }

    private static boolean isLootrBlock(BlockState state) {
        var blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return blockId != null && LootrTreasureDivinationCompat.MOD_ID.equals(blockId.getNamespace());
    }
}
