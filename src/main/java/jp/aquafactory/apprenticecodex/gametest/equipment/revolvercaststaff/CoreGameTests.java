package jp.aquafactory.apprenticecodex.gametest.equipment.revolvercaststaff;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void spellCalibrationBenchStoresScrollsOnRevolvercastStaff(GameTestHelper helper) {
        RevolvercastStaffScenarios.spellCalibrationBenchStoresScrollsOnRevolvercastStaff(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void revolvercastStaffSelectedScrollNormalizesAndDrivesSpellWheel(GameTestHelper helper) {
        RevolvercastStaffScenarios.revolvercastStaffSelectedScrollNormalizesAndDrivesSpellWheel(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void revolvercastStaffCooldownFailureAdvancesOnlyInSkipMode(GameTestHelper helper) {
        RevolvercastStaffScenarios.revolvercastStaffCooldownFailureAdvancesOnlyInSkipMode(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void revolvercastStaffSuccessfulCastAdvancesAfterCompletionTick(GameTestHelper helper) {
        RevolvercastStaffScenarios.revolvercastStaffSuccessfulCastAdvancesAfterCompletionTick(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void revolvercastStaffPendingAdvanceSurvivesUnrelatedCastCompletion(GameTestHelper helper) {
        RevolvercastStaffScenarios.revolvercastStaffPendingAdvanceSurvivesUnrelatedCastCompletion(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void revolvercastStaffCancelledCastDoesNotAdvancePendingSelection(GameTestHelper helper) {
        RevolvercastStaffScenarios.revolvercastStaffCancelledCastDoesNotAdvancePendingSelection(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void revolvercastStaffBlocksArcaneAnvilAndUsesDiamondSwingcastRestrictions(GameTestHelper helper) {
        RevolvercastStaffScenarios.revolvercastStaffBlocksArcaneAnvilAndUsesDiamondSwingcastRestrictions(helper);
    }
}
