package jp.aquafactory.apprenticecodex.gametest.spell.catchflame;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.catch_flame_isolated")
    public static void catchFlameDamagesAndPenetratesBurningTargetIFrames(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameDamagesAndPenetratesBurningTargetIFrames(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.catch_flame_isolated")
    public static void catchFlameExtendsThermalSunderedOnlyAfterSuccessfulDamage(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameExtendsThermalSunderedOnlyAfterSuccessfulDamage(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.catch_flame_isolated")
    public static void catchFlameRejectsFireImmuneTargetsIncludingEndCrystal(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameRejectsFireImmuneTargetsIncludingEndCrystal(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.catch_flame_isolated")
    public static void catchFlameTargetsFireImmuneEndCrystalWithoutDamagingIt(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameTargetsFireImmuneEndCrystalWithoutDamagingIt(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.catch_flame_isolated")
    public static void catchFlameAreaRejectsTargetsBehindThinCover(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameAreaRejectsTargetsBehindThinCover(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.catch_flame_isolated")
    public static void catchFlameUsesSeparateDispenserAndNonPlayerIgnitionRules(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameUsesSeparateDispenserAndNonPlayerIgnitionRules(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.catch_flame_isolated")
    public static void catchFlameRejectsIgnitionFromUnresolvedRemoteOwner(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameRejectsIgnitionFromUnresolvedRemoteOwner(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.catch_flame_isolated")
    public static void catchFlameEssenceSmokerBranchNeverFallsBackToSurfaceFire(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameEssenceSmokerBranchNeverFallsBackToSurfaceFire(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.catch_flame_isolated")
    public static void catchFlameIgnitesEssenceSmokerWhileSneakingWithoutSurfaceFire(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameIgnitesEssenceSmokerWhileSneakingWithoutSurfaceFire(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.catch_flame_isolated")
    public static void catchFlamePreservesCompletedEssenceSmokerContents(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlamePreservesCompletedEssenceSmokerContents(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.catch_flame_isolated")
    public static void catchFlameRespectsCancelledEssenceSmokerInteraction(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameRespectsCancelledEssenceSmokerInteraction(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.catch_flame_isolated")
    public static void catchFlameUsesFlintAndSteelIgnitionBehavior(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameUsesFlintAndSteelIgnitionBehavior(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.catch_flame_isolated")
    public static void catchFlameRespectsPlaceEventWhenIgnitingFire(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameRespectsPlaceEventWhenIgnitingFire(helper);
    }
}
