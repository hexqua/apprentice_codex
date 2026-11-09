package jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FocusStaffbowConfigGameTests {
    private FocusStaffbowConfigGameTests() {}

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_ARROW_CONFIG_BATCH)
    public static void focusStaffbowArrowRequirementConfigAllowsArrowlessCasting(GameTestHelper helper) {
        FocusStaffbowConfigScenarios.focusStaffbowArrowRequirementConfigAllowsArrowlessCasting(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_CONFIG_BATCH)
    public static void focusStaffbowContinuousConfigRejectsWithoutConsumingArrow(GameTestHelper helper) {
        FocusStaffbowConfigScenarios.focusStaffbowContinuousConfigRejectsWithoutConsumingArrow(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_LOAN_CONFIG_BATCH)
    public static void focusStaffbowManaLoanConfigRejectsBorrowedPendingCast(GameTestHelper helper) {
        FocusStaffbowConfigScenarios.focusStaffbowManaLoanConfigRejectsBorrowedPendingCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_LOAN_RATIO_CONFIG_BATCH)
    public static void focusStaffbowLoanRatioConfigRejectsExcessBorrowing(GameTestHelper helper) {
        FocusStaffbowConfigScenarios.focusStaffbowLoanRatioConfigRejectsExcessBorrowing(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_DENYLIST_CONFIG_BATCH)
    public static void focusStaffbowSpellDenylistBlocksBeforeAmmo(GameTestHelper helper) {
        FocusStaffbowConfigScenarios.focusStaffbowSpellDenylistBlocksBeforeAmmo(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_ALLOWLIST_CONFIG_BATCH)
    public static void focusStaffbowSpellAllowlistBlocksMissingSpellBeforeAmmo(GameTestHelper helper) {
        FocusStaffbowConfigScenarios.focusStaffbowSpellAllowlistBlocksMissingSpellBeforeAmmo(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_ARROW_ITEM_CONFIG_BATCH)
    public static void focusStaffbowArrowCatalystItemListAllowsConfiguredSpecialArrow(GameTestHelper helper) {
        FocusStaffbowConfigScenarios.focusStaffbowArrowCatalystItemListAllowsConfiguredSpecialArrow(helper);
    }
}
