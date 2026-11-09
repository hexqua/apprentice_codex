package jp.aquafactory.apprenticecodex.gametest.crafting.calibration;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.menu.CalibrationImbueGameTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LegacyCalibrationGameTests {
    private LegacyCalibrationGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellCalibrationBenchTargetsExposeExpectedSlots(GameTestHelper helper) {
        CalibrationImbueGameTests.spellCalibrationBenchTargetsExposeExpectedSlots(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCalibrationAdjustmentProfilesEnforceDeclaredRules(GameTestHelper helper) {
        CalibrationImbueGameTests.spellCalibrationAdjustmentProfilesEnforceDeclaredRules(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void declaredCalibrationAdjustmentTargetsProvideMatchingTooltips(GameTestHelper helper) {
        CalibrationImbueGameTests.declaredCalibrationAdjustmentTargetsProvideMatchingTooltips(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void legacyCalibrationAdjustmentFormatsMigrateOnFirstMutation(GameTestHelper helper) {
        CalibrationImbueGameTests.legacyCalibrationAdjustmentFormatsMigrateOnFirstMutation(helper);
    }
}
