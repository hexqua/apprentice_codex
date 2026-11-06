package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

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
    public static void goldSpellcasterGunImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.goldSpellcasterGunImbuedSpellStaysRemovableAfterNormalization(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellcasterGunRecastImbueRestrictionsMatchTier(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunRecastImbueRestrictionsMatchTier(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellcasterGunAbilityTooltipUsesInstantLongCastOnly(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunAbilityTooltipUsesInstantLongCastOnly(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void reflectcastShieldCastRestrictionsFollowCalibration(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.reflectcastShieldCastRestrictionsFollowCalibration(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellgunServerConfigDefaultsMatchBalanceValues(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunServerConfigDefaultsMatchBalanceValues(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellgunFixedCooldownUsesCooldownReductionAttribute(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunFixedCooldownUsesCooldownReductionAttribute(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellgun_config")
    public static void spellgunZeroImbueCooldownLimitDisablesOnlyCooldownLimit(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunZeroImbueCooldownLimitDisablesOnlyCooldownLimit(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellgun_config")
    public static void spellgunZeroCooldownSettingsRemainNonNegative(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunZeroCooldownSettingsRemainNonNegative(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellcasterGunsRemoveBaseSpellPowerButKeepSurge(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunsRemoveBaseSpellPowerButKeepSurge(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellcasterGunsAcceptOnlySilverSpellAmplifierCalibration(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunsAcceptOnlySilverSpellAmplifierCalibration(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellcasterGunsKeepCalibrationBenchImbueOperationalAndSafe(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunsKeepCalibrationBenchImbueOperationalAndSafe(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void silverSpellAmplifierMovesAllSpellgunAttributesToOffhand(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.silverSpellAmplifierMovesAllSpellgunAttributesToOffhand(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void silverSpellAmplifierKeepsDualSpellgunModifiersIndependent(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.silverSpellAmplifierKeepsDualSpellgunModifiersIndependent(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void silverSpellAmplifierMovesUpgradeOrbModifiersToOffhand(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.silverSpellAmplifierMovesUpgradeOrbModifiersToOffhand(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellcasterGunRecastCastBypassesAmmoRequirement(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunRecastCastBypassesAmmoRequirement(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellgunHandUseContractDoesNotFallback(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunHandUseContractDoesNotFallback(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellgunsUseOneHandRangedEpicFightCapabilityWithoutInnate(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunsUseOneHandRangedEpicFightCapabilityWithoutInnate(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellgunEpicFightOffhandPolicyUsesOnlyValidOneHandMainhands(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunEpicFightOffhandPolicyUsesOnlyValidOneHandMainhands(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellcasterGunTooltipsUseCommonOperationDescriptions(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunTooltipsUseCommonOperationDescriptions(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellcasterRoundTooltipsUseSharedKeys(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterRoundTooltipsUseSharedKeys(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void invalidSpellgunSpellUsesDedicatedError(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.invalidSpellgunSpellUsesDedicatedError(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellgunCastAttemptPreservesExistingCast(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunCastAttemptPreservesExistingCast(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellgun_config")
    public static void spellgunMaximumManaBypassIsExplicitAndScoped(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunMaximumManaBypassIsExplicitAndScoped(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellgun_config")
    public static void malignantSpellgunForcesOnlyActivationPower(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.malignantSpellgunForcesOnlyActivationPower(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void malignantSpellgunSpiritInfusionRecipeFollowsMalumAvailability(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.malignantSpellgunSpiritInfusionRecipeFollowsMalumAvailability(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void goldSpellcasterGunImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.goldSpellcasterGunImbuedSpellStaysRemovableAfterSaveLoad(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void ironSpellcasterGunExtractedSpellStaysClearedAfterSaveLoad(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.ironSpellcasterGunExtractedSpellStaysClearedAfterSaveLoad(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void goldSpellcasterGunLegacyLockedReplacementIsRecoveredAfterSaveLoad(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.goldSpellcasterGunLegacyLockedReplacementIsRecoveredAfterSaveLoad(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void multipurposeStaffrifleKeepsExpectedStats(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleKeepsExpectedStats(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void multipurposeStaffrifleTooltipShowsControlsBeforeShiftHint(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleTooltipShowsControlsBeforeShiftHint(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void multipurposeStaffriflePreservesNormalCooldown(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffriflePreservesNormalCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void multipurposeStaffrifleRateLimitIgnoresLegacyPersistentNbt(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleRateLimitIgnoresLegacyPersistentNbt(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void multipurposeStaffrifleUsesDedicatedAmmoAndCasingReturnPolicy(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleUsesDedicatedAmmoAndCasingReturnPolicy(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void multipurposeStaffrifleRecastSkipsAmmoConsumption(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleRecastSkipsAmmoConsumption(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void multipurposeStaffrifleCastsAtZeroMana(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleCastsAtZeroMana(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void multipurposeStaffrifleInstantCastConsumesAmmoAndAppliesCooldownPolicy(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleInstantCastConsumesAmmoAndAppliesCooldownPolicy(helper);
    }
}
