package jp.aquafactory.apprenticecodex.gametest.support;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static jp.aquafactory.apprenticecodex.gametest.support.PositionTestSupport.prepareHighIsolationPlatform;

public final class IsolationTestSupport {
    private IsolationTestSupport() {}

    public static void forceLoadChunk(ServerLevel level, BlockPos pos) {
        var chunkX = SectionPos.blockToSectionCoord(pos.getX());
        var chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        level.setChunkForced(chunkX, chunkZ, true);
        level.getChunk(chunkX, chunkZ);
    }

    public static void prepareWideSearchIsolationArea(GameTestHelper helper, BlockPos centerPos) {
        prepareHighIsolationPlatform(helper, centerPos);
    }

    public static void prepareMiningSpellIsolationArea(GameTestHelper helper, BlockPos centerPos) {
        prepareHighIsolationPlatform(helper, centerPos);
    }

    public static void prepareSummonedEntityIsolationArea(GameTestHelper helper, BlockPos centerPos) {
        prepareHighIsolationPlatform(helper, centerPos);
    }

    public static BlockPos createRemoteIsolationOrigin(GameTestHelper helper, BlockPos relativePos, int xOffset, int zOffset) {
        return helper.absolutePos(relativePos).offset(xOffset, 0, zOffset);
    }

    public static void prepareAbsoluteIsolationPlatform(ServerLevel level, BlockPos centerPos) {
        var floorY = centerPos.getY() - 1;
        for (var x = -2; x <= 2; ++x) {
            for (var z = -2; z <= 2; ++z) {
                var floorPos = new BlockPos(centerPos.getX() + x, floorY, centerPos.getZ() + z);
                var lowerAirPos = new BlockPos(centerPos.getX() + x, centerPos.getY(), centerPos.getZ() + z);
                var upperAirPos = new BlockPos(centerPos.getX() + x, centerPos.getY() + 1, centerPos.getZ() + z);
                forceLoadChunk(level, floorPos);
                level.setBlock(floorPos, Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(lowerAirPos, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(upperAirPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    public static void prepareAbsoluteIsolationTargetPlatform(ServerLevel level, Vec3 targetCenter) {
        var floorPos = BlockPos.containing(targetCenter.x, targetCenter.y - 1.0D, targetCenter.z);
        forceLoadChunk(level, floorPos);
        level.setBlock(floorPos, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(floorPos.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(floorPos.above(2), Blocks.AIR.defaultBlockState(), 3);
    }
}
