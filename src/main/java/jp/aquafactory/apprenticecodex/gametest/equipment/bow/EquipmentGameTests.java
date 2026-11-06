package jp.aquafactory.apprenticecodex.gametest.equipment.bow;

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
    public static void elementalBowKeepsVanillaBowEnchantmentSurfaces(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowKeepsVanillaBowEnchantmentSurfaces(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowBuildsSelectionViewsFromHeldAmmo(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowBuildsSelectionViewsFromHeldAmmo(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowInventoryOverlayReflectsCurrentSelection(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowInventoryOverlayReflectsCurrentSelection(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.elemental_bow_overheat", timeoutTicks = 80)
    public static void elementalBowSelectionViewExposesOverheatOverlayState(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowSelectionViewExposesOverheatOverlayState(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowKeepsCurrentEmptySpecialSelectionOnlyWhileSelected(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowKeepsCurrentEmptySpecialSelectionOnlyWhileSelected(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowRequiresManaBeforeStartingElementalDraw(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowRequiresManaBeforeStartingElementalDraw(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowDoesNotConsumeResourcesBeforeFullDraw(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowDoesNotConsumeResourcesBeforeFullDraw(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowDoesNotCastWhenArrowDisappearsDuringDraw(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowDoesNotCastWhenArrowDisappearsDuringDraw(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowInfinityAllowsVanillaDrawWithoutArrows(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowInfinityAllowsVanillaDrawWithoutArrows(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowInfinityAllowsArrowModeDrawWithoutArrows(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowInfinityAllowsArrowModeDrawWithoutArrows(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowVanillaModeInfinityDoesNotConsumeNormalArrows(GameTestHelper helper) {
        ElementalBowAmmoScenarios.elementalBowVanillaModeInfinityDoesNotConsumeNormalArrows(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowArrowModeInfinityDoesNotConsumeNormalArrows(GameTestHelper helper) {
        ElementalBowAmmoScenarios.elementalBowArrowModeInfinityDoesNotConsumeNormalArrows(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowSpecialModeConsumesLastArrowAndKeepsSelection(GameTestHelper helper) {
        ElementalBowAmmoScenarios.elementalBowSpecialModeConsumesLastArrowAndKeepsSelection(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowMagicModeIgnoresInfinityWithoutAmmo(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowMagicModeIgnoresInfinityWithoutAmmo(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowAcceptsSynthesisEnchantmentsAndTooltip(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowAcceptsSynthesisEnchantmentsAndTooltip(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowSynthesisAllowsMagicModeWithoutArrows(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowSynthesisAllowsMagicModeWithoutArrows(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowCreativeAllowsMagicModeWithoutArrows(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowCreativeAllowsMagicModeWithoutArrows(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowMagicArrowCatalystItemsAllowsConfiguredSpecialArrow(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowMagicArrowCatalystItemsAllowsConfiguredSpecialArrow(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowSynthesisDoesNotConsumeMagicModeArrows(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowSynthesisDoesNotConsumeMagicModeArrows(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowNonMagicModesHideDerivedSpellPresentation(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowNonMagicModesHideDerivedSpellPresentation(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.elemental_bow_overheat")
    public static void elementalBowCooldownHelperIgnoresWeaponMultiplierButKeepsPlayerCooldownReduction(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowCooldownHelperIgnoresWeaponMultiplierButKeepsPlayerCooldownReduction(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.elemental_bow_overheat")
    public static void elementalBowSuppressesElementalArrowCooldown(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowSuppressesElementalArrowCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.elemental_bow_overheat")
    public static void elementalBowConsumesAdditionalManaWhileOverheated(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowConsumesAdditionalManaWhileOverheated(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.elemental_bow_overheat")
    public static void elementalBowSharesOverheatAcrossSpellsAndBows(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowSharesOverheatAcrossSpellsAndBows(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.elemental_bow_overheat", timeoutTicks = 80)
    public static void elementalBowOverheatRefreshesDurationAfterRepeatedCast(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowOverheatRefreshesDurationAfterRepeatedCast(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowRejectsOffhandUse(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowRejectsOffhandUse(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowAllowsMainhandUseWithOffhandSelection(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowAllowsMainhandUseWithOffhandSelection(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowRejectsUseWithoutArrowCatalyst(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowRejectsUseWithoutArrowCatalyst(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowShowsLongSummonWeaponDuringPendingCast(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowShowsLongSummonWeaponDuringPendingCast(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowUpdatesArtisanSmashSplashRadiusOnChargedRelease(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowUpdatesArtisanSmashSplashRadiusOnChargedRelease(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowReevaluatesSummonWeaponAttackValuesOnChargedCast(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowReevaluatesSummonWeaponAttackValuesOnChargedCast(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowCancelsPendingSummonWeaponBeforeRequiredCharge(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowCancelsPendingSummonWeaponBeforeRequiredCharge(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowLethalAssaultWaitsForReleaseBeforeFiring(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowLethalAssaultWaitsForReleaseBeforeFiring(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowHiganbanaWaitsForReleaseBeforeSlashing(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowHiganbanaWaitsForReleaseBeforeSlashing(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowContinuousCastStaysActivePastSpellDuration(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowContinuousCastStaysActivePastSpellDuration(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowContinuousCastUsesStandardCastTimeWithoutAttributeAdjustment(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowContinuousCastUsesStandardCastTimeWithoutAttributeAdjustment(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowContinuousCastStopsWhenManaRunsOut(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowContinuousCastStopsWhenManaRunsOut(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowInstantImmediateReleaseConsumesBaseMana(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowInstantImmediateReleaseConsumesBaseMana(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowShortLongReleaseStaysAtBaseMultiplier(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowShortLongReleaseStaysAtBaseMultiplier(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowConfigCurveAndManaFormulaUsesFixedTimeToMax(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowConfigCurveAndManaFormulaUsesFixedTimeToMax(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowStillRejectsCastWhenBaseManaIsInsufficient(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowStillRejectsCastWhenBaseManaIsInsufficient(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_arrow_config")
    public static void focusStaffbowArrowRequirementConfigAllowsArrowlessCasting(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowArrowRequirementConfigAllowsArrowlessCasting(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_continuous_config")
    public static void focusStaffbowContinuousConfigRejectsWithoutConsumingArrow(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowContinuousConfigRejectsWithoutConsumingArrow(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_loan_config")
    public static void focusStaffbowManaLoanConfigRejectsBorrowedPendingCast(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowManaLoanConfigRejectsBorrowedPendingCast(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_loan_ratio_config")
    public static void focusStaffbowLoanRatioConfigRejectsExcessBorrowing(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowLoanRatioConfigRejectsExcessBorrowing(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_denylist_config")
    public static void focusStaffbowSpellDenylistBlocksBeforeAmmo(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowSpellDenylistBlocksBeforeAmmo(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowRejectsPreCastSpellPowerDependentSpellsBeforeAmmo(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowRejectsPreCastSpellPowerDependentSpellsBeforeAmmo(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_allowlist_config")
    public static void focusStaffbowSpellAllowlistBlocksMissingSpellBeforeAmmo(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowSpellAllowlistBlocksMissingSpellBeforeAmmo(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowOverchargeLoanConsumesRecoveredMana(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowOverchargeLoanConsumesRecoveredMana(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowCreativeOverchargeDoesNotConsumeManaOrCreateLoan(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowCreativeOverchargeDoesNotConsumeManaOrCreateLoan(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowCreativeContinuousReleaseSkipsCooldown(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowCreativeContinuousReleaseSkipsCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowCreativeInterruptionSkipsPreviousSpellCooldown(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowCreativeInterruptionSkipsPreviousSpellCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowBlocksUseWhileLoanRemains(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowBlocksUseWhileLoanRemains(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowRejectsUseWhileSpellCooldownRemains(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowRejectsUseWhileSpellCooldownRemains(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowLoanMessageUsesExpectedTranslationKey(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowLoanMessageUsesExpectedTranslationKey(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowInsufficientArrowMessageUsesExpectedTranslationKey(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowInsufficientArrowMessageUsesExpectedTranslationKey(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowRejectsUnconfiguredSpecialArrow(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowRejectsUnconfiguredSpecialArrow(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.focus_staffbow_arrow_item_config")
    public static void focusStaffbowArrowCatalystItemListAllowsConfiguredSpecialArrow(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowArrowCatalystItemListAllowsConfiguredSpecialArrow(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowSynthesisAllowsArrowlessCasting(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowSynthesisAllowsArrowlessCasting(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void focusStaffbowExposesExpectedMainhandAttributes(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowExposesExpectedMainhandAttributes(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowFallsBackToNoneWhenLegacyModeCannotResolve(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowFallsBackToNoneWhenLegacyModeCannotResolve(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowUsesStoredScrollWithoutSpellContainer(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowUsesStoredScrollWithoutSpellContainer(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowScrollLevelUsesOnlyTranscendence(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowScrollLevelUsesOnlyTranscendence(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowDoesNotAddDerivedSpellToMainhandSpellWheel(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowDoesNotAddDerivedSpellToMainhandSpellWheel(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowBlocksArcaneAnvilImbueViaSpellValidator(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowBlocksArcaneAnvilImbueViaSpellValidator(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowManaErrorUsesIronsSpellbooksTranslationKey(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowManaErrorUsesIronsSpellbooksTranslationKey(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowInsufficientArrowErrorUsesDedicatedTranslationKey(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowInsufficientArrowErrorUsesDedicatedTranslationKey(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowVanillaModeConsumesSpecialArrowWhenNormalArrowsAreMissing(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowVanillaModeConsumesSpecialArrowWhenNormalArrowsAreMissing(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowArrowModeRequiresNormalArrowsEvenWhenSpecialArrowsExist(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowArrowModeRequiresNormalArrowsEvenWhenSpecialArrowsExist(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.elemental_bow_draw_config")
    public static void elementalBowMagicDrawTicksUseProfileAndServerMultiplier(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowMagicDrawTicksUseProfileAndServerMultiplier(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.elemental_bow_overheat_mana_config")
    public static void elementalBowAdditionalManaUsesServerConfig(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowAdditionalManaUsesServerConfig(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.elemental_bow_overheat_duration_config")
    public static void elementalBowOverheatDurationUsesServerConfig(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowOverheatDurationUsesServerConfig(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.elemental_bow_power_config")
    public static void elementalBowPowerDoesNotIncreaseSpellLevel(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowPowerDoesNotIncreaseSpellLevel(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowClampsPersistedFutureOverheat(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowClampsPersistedFutureOverheat(helper);
    }
}
