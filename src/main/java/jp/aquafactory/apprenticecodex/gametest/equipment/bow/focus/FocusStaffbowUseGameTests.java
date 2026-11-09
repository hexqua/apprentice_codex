package jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FocusStaffbowUseGameTests {
    private FocusStaffbowUseGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowRejectsOffhandUse(GameTestHelper helper) {
        FocusStaffbowUseScenarios.focusStaffbowRejectsOffhandUse(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowAllowsMainhandUseWithOffhandSelection(GameTestHelper helper) {
        FocusStaffbowUseScenarios.focusStaffbowAllowsMainhandUseWithOffhandSelection(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowRejectsUseWithoutArrowCatalyst(GameTestHelper helper) {
        FocusStaffbowUseScenarios.focusStaffbowRejectsUseWithoutArrowCatalyst(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowStillRejectsCastWhenBaseManaIsInsufficient(GameTestHelper helper) {
        FocusStaffbowUseScenarios.focusStaffbowStillRejectsCastWhenBaseManaIsInsufficient(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowRejectsPreCastSpellPowerDependentSpellsBeforeAmmo(GameTestHelper helper) {
        FocusStaffbowUseScenarios.focusStaffbowRejectsPreCastSpellPowerDependentSpellsBeforeAmmo(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowCreativeInterruptionSkipsPreviousSpellCooldown(GameTestHelper helper) {
        FocusStaffbowUseScenarios.focusStaffbowCreativeInterruptionSkipsPreviousSpellCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowBlocksUseWhileLoanRemains(GameTestHelper helper) {
        FocusStaffbowUseScenarios.focusStaffbowBlocksUseWhileLoanRemains(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowRejectsUseWhileSpellCooldownRemains(GameTestHelper helper) {
        FocusStaffbowUseScenarios.focusStaffbowRejectsUseWhileSpellCooldownRemains(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowLoanMessageUsesExpectedTranslationKey(GameTestHelper helper) {
        FocusStaffbowUseScenarios.focusStaffbowLoanMessageUsesExpectedTranslationKey(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowRejectsUnconfiguredSpecialArrow(GameTestHelper helper) {
        FocusStaffbowUseScenarios.focusStaffbowRejectsUnconfiguredSpecialArrow(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowSynthesisAllowsArrowlessCasting(GameTestHelper helper) {
        FocusStaffbowUseScenarios.focusStaffbowSynthesisAllowsArrowlessCasting(helper);
    }
}
