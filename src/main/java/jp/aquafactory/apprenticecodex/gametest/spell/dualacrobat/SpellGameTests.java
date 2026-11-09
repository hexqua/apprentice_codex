package jp.aquafactory.apprenticecodex.gametest.spell.dualacrobat;

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

    @GameTest(template = TEMPLATE, batch = DUAL_ACROBAT_STARTUP_BATCH, timeoutTicks = 80)
    public static void dualAcrobatStartsFiringAfterStartupAndContinuesWhileCasting(GameTestHelper helper) {
        DualAcrobatScenarios.dualAcrobatStartsFiringAfterStartupAndContinuesWhileCasting(helper);
    }

    @GameTest(template = TEMPLATE, batch = DUAL_ACROBAT_COMPLETION_BATCH, timeoutTicks = 80)
    public static void dualAcrobatCompletionDiscardsImmediately(GameTestHelper helper) {
        DualAcrobatScenarios.dualAcrobatCompletionDiscardsImmediately(helper);
    }

    // chunk準備の予算を他の射撃テストと揃える。取消後の観測期間は延長・短縮しない。
    @GameTest(template = TEMPLATE, batch = DUAL_ACROBAT_CANCELLED_BATCH, timeoutTicks = 80)
    public static void dualAcrobatCancelledInterruptionDiscardsImmediately(GameTestHelper helper) {
        DualAcrobatScenarios.dualAcrobatCancelledInterruptionDiscardsImmediately(helper);
    }

    @GameTest(template = TEMPLATE, batch = DUAL_ACROBAT_COUNTERSPELL_BATCH, timeoutTicks = 80)
    public static void dualAcrobatCounterspellInterruptDiscardsImmediately(GameTestHelper helper) {
        DualAcrobatScenarios.dualAcrobatCounterspellInterruptDiscardsImmediately(helper);
    }

    @GameTest(template = TEMPLATE, batch = DUAL_ACROBAT_OWNERSHIP_BATCH, timeoutTicks = 40)
    public static void dualAcrobatCounterspellDoesNotInterruptNearbyOtherOwnerWeapon(GameTestHelper helper) {
        DualAcrobatScenarios.dualAcrobatCounterspellDoesNotInterruptNearbyOtherOwnerWeapon(helper);
    }
}
