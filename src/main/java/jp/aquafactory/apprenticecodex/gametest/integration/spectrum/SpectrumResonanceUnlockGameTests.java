package jp.aquafactory.apprenticecodex.gametest.integration.spectrum;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// Spectrum 1.12.9では採掘時の使用進捗判定が抜けている。期待する制限を任意テストにし、上流修正で成功へ変わるようにする。
@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpectrumResonanceUnlockGameTests {
    private SpectrumResonanceUnlockGameTests() {}

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated", required = false)
    public static void touchDigMustNotRecoverSpawnerBeforeResonanceUnlock(GameTestHelper helper) {
        logFailureReason(helper);
        if (ModList.get().isLoaded("spectrum")) SpectrumMiningScenarios.touchDig(helper, false);
        helper.succeed();
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated", required = false)
    public static void worldFlatterMustNotRecoverSpawnerBeforeResonanceUnlock(GameTestHelper helper) {
        logFailureReason(helper);
        if (ModList.get().isLoaded("spectrum")) SpectrumMiningScenarios.worldFlatter(helper, false);
        helper.succeed();
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated", required = false)
    public static void psychicDriveGauntletsMustNotRecoverSpawnerBeforeResonanceUnlock(GameTestHelper helper) {
        logFailureReason(helper);
        if (ModList.get().isLoaded("spectrum")) SpectrumMiningScenarios.gauntlets(helper, false);
        helper.succeed();
    }

    private static void logFailureReason(GameTestHelper helper) {
        // 標準の任意失敗サマリはIDのみなので、回収制限以外の準備エラーも区別できるよう原因を残す。
        helper.testInfo.addListener(new GameTestListener() {
            @Override public void testStructureLoaded(GameTestInfo test) {}
            @Override public void testPassed(GameTestInfo test, GameTestRunner runner) {}
            @Override public void testFailed(GameTestInfo test, GameTestRunner runner) {
                ApprenticeCodex.LOGGER.warn("Optional Spectrum Resonance unlock test failed", test.getError());
            }
            @Override public void testAddedForRerun(GameTestInfo previous, GameTestInfo next, GameTestRunner runner) {}
        });
    }
}
