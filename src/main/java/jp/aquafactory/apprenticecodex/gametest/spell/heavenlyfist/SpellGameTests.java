package jp.aquafactory.apprenticecodex.gametest.spell.heavenlyfist;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistImpactAabbAppliesDamageAndGravityBound(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistImpactAabbAppliesDamageAndGravityBound(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistImpactDamagesNonLivingCombatTarget(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistImpactDamagesNonLivingCombatTarget(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistImpactDoesNotTrackMovedTarget(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistImpactDoesNotTrackMovedTarget(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistProcessesCreateDepotItems(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistProcessesCreateDepotItems(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistLeavesDroppedCreateItemsOutsideProcessArea(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistLeavesDroppedCreateItemsOutsideProcessArea(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_CREATE_PRESSING_DENYLIST_BATCH, timeoutTicks = 60)
    public static void heavenlyFistCreatePressingDenylistLeavesDepotItems(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistCreatePressingDenylistLeavesDepotItems(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistProcessesCreateBasinCompacting(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistProcessesCreateBasinCompacting(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistCreateBasinCompactingConsumesOneBudgetPerRecipe(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistCreateBasinCompactingConsumesOneBudgetPerRecipe(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_CREATE_COMPACTING_DENYLIST_BATCH, timeoutTicks = 60)
    public static void heavenlyFistCreateCompactingDenylistLeavesBasinItems(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistCreateCompactingDenylistLeavesBasinItems(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistSkipsCreateBasinCompressionCrafting(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistSkipsCreateBasinCompressionCrafting(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 40)
    public static void gravityBoundPullsAirborneTargetsDown(GameTestHelper helper) {
        HeavenlyFistScenarios.gravityBoundPullsAirborneTargetsDown(helper);
    }
}
