package jp.aquafactory.apprenticecodex.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;

final class GameTestFixtureSupport {
    private GameTestFixtureSupport() {
    }

    static void whenEntityChunksReady(GameTestHelper helper, Runnable action) {
        whenEntityChunksReady(helper, new BlockPos(-4, 0, -4), new BlockPos(8, 0, 8), action);
    }

    static void whenEntityChunksReady(GameTestHelper helper, BlockPos min, BlockPos max, Runnable action) {
        var first = new ChunkPos(helper.absolutePos(min));
        var last = new ChunkPos(helper.absolutePos(max));
        // 1.20.1ではstructure配置時の強制読込だけではentity sectionの公開・tick開始を保証しない。
        // 待機中に生成やcastを再実行せず、準備完了後の操作とassertは一度だけ実行する。
        helper.startSequence().thenWaitUntil(() -> {
            for (int x = Math.min(first.x, last.x); x <= Math.max(first.x, last.x); x++) {
                for (int z = Math.min(first.z, last.z); z <= Math.max(first.z, last.z); z++) {
                    helper.assertTrue(helper.getLevel().isPositionEntityTicking(new BlockPos(x * 16, 0, z * 16)),
                            "Fixture chunk must be entity-ticking before registration: " + x + ", " + z);
                }
            }
        }).thenExecute(action);
    }
}
