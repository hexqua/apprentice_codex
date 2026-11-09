package jp.aquafactory.apprenticecodex.gametest.support;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

public final class PositionTestSupport {
    private PositionTestSupport() {}

    public static void prepareHighIsolationPlatform(GameTestHelper helper, BlockPos centerPos) {
        // basic_floor は 5x3x5 と小さく、batch 近接配置の地形へ探索やレイが吸われやすい。
        // 足場を高所へ自前で作って、各テストが自分の 5x5 領域だけを参照するように固定する。
        var floorY = centerPos.getY() - 1;
        for (var x = -2; x <= 2; ++x) {
            for (var z = -2; z <= 2; ++z) {
                helper.setBlock(new BlockPos(centerPos.getX() + x, floorY, centerPos.getZ() + z), Blocks.STONE);
                helper.setBlock(new BlockPos(centerPos.getX() + x, centerPos.getY(), centerPos.getZ() + z), Blocks.AIR);
                helper.setBlock(new BlockPos(centerPos.getX() + x, centerPos.getY() + 1, centerPos.getZ() + z), Blocks.AIR);
            }
        }
    }
}
