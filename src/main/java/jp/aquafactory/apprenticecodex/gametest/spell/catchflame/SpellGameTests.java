package jp.aquafactory.apprenticecodex.gametest.spell.catchflame;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.CATCH_FLAME_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = CATCH_FLAME_ISOLATED_BATCH)
    public static void catchFlameDamagesAndPenetratesBurningTargetIFrames(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameDamagesAndPenetratesBurningTargetIFrames(helper);
    }

    @GameTest(template = TEMPLATE, batch = CATCH_FLAME_ISOLATED_BATCH)
    public static void catchFlameExtendsThermalSunderedOnlyAfterSuccessfulDamage(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameExtendsThermalSunderedOnlyAfterSuccessfulDamage(helper);
    }

    @GameTest(template = TEMPLATE, batch = CATCH_FLAME_ISOLATED_BATCH)
    public static void catchFlameAreaRejectsTargetsBehindThinCover(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameAreaRejectsTargetsBehindThinCover(helper);
    }

    @GameTest(template = TEMPLATE, batch = CATCH_FLAME_ISOLATED_BATCH)
    public static void catchFlameUsesSeparateDispenserAndNonPlayerIgnitionRules(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameUsesSeparateDispenserAndNonPlayerIgnitionRules(helper);
    }

    @GameTest(template = TEMPLATE, batch = CATCH_FLAME_ISOLATED_BATCH)
    public static void catchFlameRejectsIgnitionFromUnresolvedRemoteOwner(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameRejectsIgnitionFromUnresolvedRemoteOwner(helper);
    }

    @GameTest(template = TEMPLATE, batch = CATCH_FLAME_ISOLATED_BATCH)
    public static void catchFlameEssenceSmokerBranchNeverFallsBackToSurfaceFire(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameEssenceSmokerBranchNeverFallsBackToSurfaceFire(helper);
    }

    @GameTest(template = TEMPLATE, batch = CATCH_FLAME_ISOLATED_BATCH)
    public static void catchFlameIgnitesEssenceSmokerWhileSneakingWithoutSurfaceFire(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameIgnitesEssenceSmokerWhileSneakingWithoutSurfaceFire(helper);
    }

    @GameTest(template = TEMPLATE, batch = CATCH_FLAME_ISOLATED_BATCH)
    public static void catchFlamePreservesCompletedEssenceSmokerContents(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlamePreservesCompletedEssenceSmokerContents(helper);
    }

    @GameTest(template = TEMPLATE, batch = CATCH_FLAME_ISOLATED_BATCH)
    public static void catchFlameRespectsCancelledEssenceSmokerInteraction(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameRespectsCancelledEssenceSmokerInteraction(helper);
    }

    @GameTest(template = TEMPLATE, batch = CATCH_FLAME_ISOLATED_BATCH)
    public static void catchFlameUsesFlintAndSteelIgnitionBehavior(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameUsesFlintAndSteelIgnitionBehavior(helper);
    }

    @GameTest(template = TEMPLATE, batch = CATCH_FLAME_ISOLATED_BATCH)
    public static void catchFlameRespectsPlaceEventWhenIgnitingFire(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameRespectsPlaceEventWhenIgnitingFire(helper);
    }
}
