package jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FocusStaffbowManaGameTests {
    private FocusStaffbowManaGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowInstantImmediateReleaseConsumesBaseMana(GameTestHelper helper) {
        FocusStaffbowManaScenarios.focusStaffbowInstantImmediateReleaseConsumesBaseMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowShortLongReleaseStaysAtBaseMultiplier(GameTestHelper helper) {
        FocusStaffbowManaScenarios.focusStaffbowShortLongReleaseStaysAtBaseMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowConfigCurveAndManaFormulaUsesFixedTimeToMax(GameTestHelper helper) {
        FocusStaffbowManaScenarios.focusStaffbowConfigCurveAndManaFormulaUsesFixedTimeToMax(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowOverchargeLoanConsumesRecoveredMana(GameTestHelper helper) {
        FocusStaffbowManaScenarios.focusStaffbowOverchargeLoanConsumesRecoveredMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowCreativeOverchargeDoesNotConsumeManaOrCreateLoan(GameTestHelper helper) {
        FocusStaffbowManaScenarios.focusStaffbowCreativeOverchargeDoesNotConsumeManaOrCreateLoan(helper);
    }
}
