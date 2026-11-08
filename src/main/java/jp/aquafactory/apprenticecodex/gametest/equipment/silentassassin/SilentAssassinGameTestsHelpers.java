package jp.aquafactory.apprenticecodex.gametest.equipment.silentassassin;

import net.minecraft.gametest.framework.GameTestHelper;

final class SilentAssassinGameTestsHelpers {
    private SilentAssassinGameTestsHelpers() {}

    static void assertClose(GameTestHelper helper, float actual, float expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.001F,
                message + ": actual=" + actual + " expected=" + expected);
    }
}
