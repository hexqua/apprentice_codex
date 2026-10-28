package jp.aquafactory.apprenticecodex.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashMap;
import java.util.Map;

/** 1.20.1の構造物外に残る地形を一時退避し、照準テストの空間を明示する。 */
final class GameTestAirSpace implements AutoCloseable {
    private final ServerLevel level;
    private final Map<BlockPos, BlockState> previous = new LinkedHashMap<>();

    GameTestAirSpace(ServerLevel level, AABB area) {
        this.level = level;
        for (var pos : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ),
                BlockPos.containing(area.maxX, area.maxY, area.maxZ))) {
            var state = level.getBlockState(pos);
            if (!state.isAir()) {
                previous.put(pos.immutable(), state);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    @Override
    public void close() {
        previous.forEach((pos, state) -> level.setBlock(pos, state, 2));
    }
}
