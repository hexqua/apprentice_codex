package jp.aquafactory.apprenticecodex.gametest.equipment.broom.support;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;

public final class BroomTestCleanup {
    private BroomTestCleanup() {}

    public static void onTestEnd(GameTestHelper helper, Runnable cleanup) {
        // 最終の成功callbackへ到達しないassertion失敗・timeoutでもテスト用資源を解放する。
        helper.testInfo.addListener(new GameTestListener() {
            private boolean closed;

            private void close() {
                if (!closed) {
                    closed = true;
                    cleanup.run();
                }
            }

            @Override public void testStructureLoaded(GameTestInfo test) {}
            @Override public void testPassed(GameTestInfo test, GameTestRunner runner) { close(); }
            @Override public void testFailed(GameTestInfo test, GameTestRunner runner) { close(); }
            @Override public void testAddedForRerun(GameTestInfo previous, GameTestInfo next, GameTestRunner runner) {
                close();
            }
        });
    }
}
