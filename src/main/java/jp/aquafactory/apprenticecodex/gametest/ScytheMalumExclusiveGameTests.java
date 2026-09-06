package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheBridge;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ScytheMalumExclusiveGameTests {
    // GameTest登録時の検証でoptional型を解決しないよう、Malum依存の本体を遅延読み込みする。
    @GameTest(template = "gametest/basic_floor")
    public static void maelstromCostBounds(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        ScytheMalumExclusiveGameTestScenarios.maelstromCostBounds(h);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void maelstromReturnsAtFixedSpeedAndChargesOnce(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        ScytheMalumExclusiveGameTestScenarios.maelstromReturnsAtFixedSpeedAndChargesOnce(h);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void reboundDamageAndWeaponSurviveRecall(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        ScytheMalumExclusiveGameTestScenarios.reboundDamageAndWeaponSurviveRecall(h);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void cullingScalesPhysicalBelowHalfOnly(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        ScytheMalumExclusiveGameTestScenarios.cullingScalesPhysicalBelowHalfOnly(h);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void narrowMaelstromPlacesNativeStormOnce(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        ScytheMalumExclusiveGameTestScenarios.narrowMaelstromPlacesNativeStormOnce(h);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void geasTriggersOnlyForReboundAndRetainsDelayedSource(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        ScytheMalumExclusiveGameTestScenarios.geasTriggersOnlyForReboundAndRetainsDelayedSource(h);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void profaneGluttonConsumesStacksOnDelayedCut(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        ScytheMalumExclusiveGameTestScenarios.profaneGluttonConsumesStacksOnDelayedCut(h);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void maelstromPulseAndNativeStormRespectAllies(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        ScytheMalumExclusiveGameTestScenarios.maelstromPulseAndNativeStormRespectAllies(h);
    }
}
