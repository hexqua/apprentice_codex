package jp.aquafactory.apprenticecodex.gametest.spell.dualacrobat;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.dual_acrobat_isolated", timeoutTicks = 80)
    public static void dualAcrobatStartsFiringAfterStartupAndContinuesWhileCasting(GameTestHelper helper) {
        DualAcrobatScenarios.dualAcrobatStartsFiringAfterStartupAndContinuesWhileCasting(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.dual_acrobat_isolated", timeoutTicks = 80)
    public static void dualAcrobatCompletionDiscardsImmediately(GameTestHelper helper) {
        DualAcrobatScenarios.dualAcrobatCompletionDiscardsImmediately(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.dual_acrobat_isolated", timeoutTicks = 40)
    public static void dualAcrobatCancelledInterruptionDiscardsImmediately(GameTestHelper helper) {
        DualAcrobatScenarios.dualAcrobatCancelledInterruptionDiscardsImmediately(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.dual_acrobat_isolated", timeoutTicks = 80)
    public static void dualAcrobatCounterspellInterruptDiscardsImmediately(GameTestHelper helper) {
        DualAcrobatScenarios.dualAcrobatCounterspellInterruptDiscardsImmediately(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.dual_acrobat_isolated", timeoutTicks = 40)
    public static void dualAcrobatCounterspellDoesNotInterruptNearbyOtherOwnerWeapon(GameTestHelper helper) {
        DualAcrobatScenarios.dualAcrobatCounterspellDoesNotInterruptNearbyOtherOwnerWeapon(helper);
    }
}
