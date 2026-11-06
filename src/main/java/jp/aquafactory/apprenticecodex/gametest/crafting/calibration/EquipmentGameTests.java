package jp.aquafactory.apprenticecodex.gametest.crafting.calibration;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void photonSiphonStartsWithLockedManaChargeAndIsNotUnique(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.photonSiphonStartsWithLockedManaChargeAndIsNotUnique(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void photonSiphonCalibrationRepairUnlocksLegacyReplacementOnly(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.photonSiphonCalibrationRepairUnlocksLegacyReplacementOnly(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCalibrationBenchTargetsExposeExpectedSlots(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.spellCalibrationBenchTargetsExposeExpectedSlots(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCalibrationAdjustmentProfilesEnforceDeclaredRules(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.spellCalibrationAdjustmentProfilesEnforceDeclaredRules(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void endgameArmorCalibrationAppliesSharedRulesAndAttributes(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.endgameArmorCalibrationAppliesSharedRulesAndAttributes(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void endgameArmorScrollwovenSlotsPersistAndFollowSelectionOrder(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios
                .endgameArmorScrollwovenSlotsPersistAndFollowSelectionOrder(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void declaredCalibrationAdjustmentTargetsProvideMatchingTooltips(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.declaredCalibrationAdjustmentTargetsProvideMatchingTooltips(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletTooltipExcludesCalibrationScrolls(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.scrollcasterGauntletTooltipExcludesCalibrationScrolls(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void legacyCalibrationAdjustmentFormatsMigrateOnFirstMutation(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.legacyCalibrationAdjustmentFormatsMigrateOnFirstMutation(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCalibrationBenchImbueStatesSeparateInsertionFromCurrentUsability(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios
                .spellCalibrationBenchImbueStatesSeparateInsertionFromCurrentUsability(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCalibrationBenchImbueOnlySupportsExtractableTargets(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.spellCalibrationBenchImbueOnlySupportsExtractableTargets(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void mithrilFreecastStaffBlocksArcaneAnvilImbueViaSpellValidator(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.mithrilFreecastStaffBlocksArcaneAnvilImbueViaSpellValidator(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void mithrilFreecastStaffCooldownUsesSelectedSource(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.mithrilFreecastStaffCooldownUsesSelectedSource(helper);
    }
}
