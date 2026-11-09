package jp.aquafactory.apprenticecodex.gametest.equipment.revolvercaststaff;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.runtime.CoreGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellCalibrationBenchStoresScrollsOnRevolvercastStaff(GameTestHelper helper) {
        RevolvercastStaffScenarios.spellCalibrationBenchStoresScrollsOnRevolvercastStaff(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void revolvercastStaffSelectedScrollNormalizesAndDrivesSpellWheel(GameTestHelper helper) {
        RevolvercastStaffScenarios.revolvercastStaffSelectedScrollNormalizesAndDrivesSpellWheel(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void revolvercastStaffCooldownFailureAdvancesOnlyInSkipMode(GameTestHelper helper) {
        RevolvercastStaffScenarios.revolvercastStaffCooldownFailureAdvancesOnlyInSkipMode(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void revolvercastStaffSuccessfulCastAdvancesAfterCompletionTick(GameTestHelper helper) {
        RevolvercastStaffScenarios.revolvercastStaffSuccessfulCastAdvancesAfterCompletionTick(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void revolvercastStaffPendingAdvanceSurvivesUnrelatedCastCompletion(GameTestHelper helper) {
        RevolvercastStaffScenarios.revolvercastStaffPendingAdvanceSurvivesUnrelatedCastCompletion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void revolvercastStaffCancelledCastDoesNotAdvancePendingSelection(GameTestHelper helper) {
        RevolvercastStaffScenarios.revolvercastStaffCancelledCastDoesNotAdvancePendingSelection(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void revolvercastStaffBlocksArcaneAnvilAndUsesDiamondSwingcastRestrictions(GameTestHelper helper) {
        RevolvercastStaffScenarios.revolvercastStaffBlocksArcaneAnvilAndUsesDiamondSwingcastRestrictions(helper);
    }
}
