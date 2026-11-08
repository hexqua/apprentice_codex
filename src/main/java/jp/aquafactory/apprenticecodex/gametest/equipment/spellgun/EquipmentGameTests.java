package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.SPELLGUN_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellcasterGunRecastImbueRestrictionsMatchTier(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunRecastImbueRestrictionsMatchTier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcasterGunAbilityTooltipUsesInstantLongCastOnly(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunAbilityTooltipUsesInstantLongCastOnly(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellgunFixedCooldownUsesCooldownReductionAttribute(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunFixedCooldownUsesCooldownReductionAttribute(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLGUN_CONFIG_BATCH)
    public static void spellgunZeroImbueCooldownLimitDisablesOnlyCooldownLimit(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunZeroImbueCooldownLimitDisablesOnlyCooldownLimit(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLGUN_CONFIG_BATCH)
    public static void spellgunZeroCooldownSettingsRemainNonNegative(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunZeroCooldownSettingsRemainNonNegative(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcasterGunsAcceptOnlySilverSpellAmplifierCalibration(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunsAcceptOnlySilverSpellAmplifierCalibration(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcasterGunsKeepCalibrationBenchImbueOperationalAndSafe(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunsKeepCalibrationBenchImbueOperationalAndSafe(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcasterGunRecastCastBypassesAmmoRequirement(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunRecastCastBypassesAmmoRequirement(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellgunHandUseContractDoesNotFallback(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunHandUseContractDoesNotFallback(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellgunsUseOneHandRangedEpicFightCapabilityWithoutInnate(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunsUseOneHandRangedEpicFightCapabilityWithoutInnate(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellgunEpicFightOffhandPolicyUsesOnlyValidOneHandMainhands(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunEpicFightOffhandPolicyUsesOnlyValidOneHandMainhands(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcasterGunTooltipsUseCommonOperationDescriptions(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunTooltipsUseCommonOperationDescriptions(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void invalidSpellgunSpellUsesDedicatedError(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.invalidSpellgunSpellUsesDedicatedError(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellgunCastAttemptPreservesExistingCast(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunCastAttemptPreservesExistingCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLGUN_CONFIG_BATCH)
    public static void spellgunMaximumManaBypassIsExplicitAndScoped(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunMaximumManaBypassIsExplicitAndScoped(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLGUN_CONFIG_BATCH)
    public static void malignantSpellgunForcesOnlyActivationPower(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.malignantSpellgunForcesOnlyActivationPower(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void malignantSpellgunSpiritInfusionRecipeFollowsMalumAvailability(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.malignantSpellgunSpiritInfusionRecipeFollowsMalumAvailability(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void goldSpellcasterGunImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.goldSpellcasterGunImbuedSpellStaysRemovableAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void ironSpellcasterGunExtractedSpellStaysClearedAfterSaveLoad(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.ironSpellcasterGunExtractedSpellStaysClearedAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void goldSpellcasterGunLegacyLockedReplacementIsRecoveredAfterSaveLoad(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.goldSpellcasterGunLegacyLockedReplacementIsRecoveredAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffriflePreservesNormalCooldown(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffriflePreservesNormalCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleRateLimitIgnoresLegacyPersistentNbt(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleRateLimitIgnoresLegacyPersistentNbt(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleRecastSkipsAmmoConsumption(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleRecastSkipsAmmoConsumption(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleCastsAtZeroMana(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleCastsAtZeroMana(helper);
    }
}
