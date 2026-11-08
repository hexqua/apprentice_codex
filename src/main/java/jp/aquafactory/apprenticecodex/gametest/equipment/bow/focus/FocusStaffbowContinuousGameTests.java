package jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.FOCUS_STAFFBOW_CONTINUOUS_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FocusStaffbowContinuousGameTests {
    private FocusStaffbowContinuousGameTests() {}

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowContinuousCastStaysActivePastSpellDuration(GameTestHelper helper) {
        FocusStaffbowContinuousScenarios.focusStaffbowContinuousCastStaysActivePastSpellDuration(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowContinuousCastUsesStandardCastTimeWithoutAttributeAdjustment(GameTestHelper helper) {
        FocusStaffbowContinuousScenarios.focusStaffbowContinuousCastUsesStandardCastTimeWithoutAttributeAdjustment(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowContinuousCastStopsWhenManaRunsOut(GameTestHelper helper) {
        FocusStaffbowContinuousScenarios.focusStaffbowContinuousCastStopsWhenManaRunsOut(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowCreativeContinuousReleaseSkipsCooldown(GameTestHelper helper) {
        FocusStaffbowContinuousScenarios.focusStaffbowCreativeContinuousReleaseSkipsCooldown(helper);
    }
}
