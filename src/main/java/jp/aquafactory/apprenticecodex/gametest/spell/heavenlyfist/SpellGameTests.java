package jp.aquafactory.apprenticecodex.gametest.spell.heavenlyfist;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.heavenly_fist_isolated", timeoutTicks = 60)
    public static void heavenlyFistImpactAabbAppliesDamageAndGravityBound(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistImpactAabbAppliesDamageAndGravityBound(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.heavenly_fist_isolated", timeoutTicks = 60)
    public static void heavenlyFistImpactDamagesNonLivingCombatTarget(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistImpactDamagesNonLivingCombatTarget(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.heavenly_fist_isolated", timeoutTicks = 60)
    public static void heavenlyFistImpactDoesNotTrackMovedTarget(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistImpactDoesNotTrackMovedTarget(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.heavenly_fist_isolated", timeoutTicks = 60)
    public static void heavenlyFistProcessesCreateDepotItems(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistProcessesCreateDepotItems(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.heavenly_fist_isolated", timeoutTicks = 60)
    public static void heavenlyFistLeavesDroppedCreateItemsOutsideProcessArea(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistLeavesDroppedCreateItemsOutsideProcessArea(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.heavenly_fist_create_pressing_denylist", timeoutTicks = 60)
    public static void heavenlyFistCreatePressingDenylistLeavesDepotItems(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistCreatePressingDenylistLeavesDepotItems(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.heavenly_fist_isolated", timeoutTicks = 60)
    public static void heavenlyFistProcessesCreateBasinCompacting(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistProcessesCreateBasinCompacting(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.heavenly_fist_isolated", timeoutTicks = 60)
    public static void heavenlyFistCreateBasinCompactingConsumesOneBudgetPerRecipe(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistCreateBasinCompactingConsumesOneBudgetPerRecipe(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.heavenly_fist_create_compacting_denylist", timeoutTicks = 60)
    public static void heavenlyFistCreateCompactingDenylistLeavesBasinItems(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistCreateCompactingDenylistLeavesBasinItems(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.heavenly_fist_isolated", timeoutTicks = 60)
    public static void heavenlyFistSkipsCreateBasinCompressionCrafting(GameTestHelper helper) {
        HeavenlyFistScenarios.heavenlyFistSkipsCreateBasinCompressionCrafting(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.heavenly_fist_isolated", timeoutTicks = 40)
    public static void gravityBoundPullsAirborneTargetsDown(GameTestHelper helper) {
        HeavenlyFistScenarios.gravityBoundPullsAirborneTargetsDown(helper);
    }
}
