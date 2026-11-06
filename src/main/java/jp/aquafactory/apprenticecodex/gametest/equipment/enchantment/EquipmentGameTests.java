package jp.aquafactory.apprenticecodex.gametest.equipment.enchantment;

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
    public static void enchantmentApplicationSurfacesKeepExpectedMatrix(GameTestHelper helper) {
        EnchantmentApplicationGameTestScenarios.itemSurfacesKeepExpectedMatrix(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void enchantmentDirectApplicationPoliciesKeepExpectedMatrix(GameTestHelper helper) {
        EnchantmentApplicationGameTestScenarios.directApplicationPoliciesKeepExpectedMatrix(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void enchantmentSpecialApplicationRulesStayExplicit(GameTestHelper helper) {
        EnchantmentApplicationGameTestScenarios.specialApplicationRulesStayExplicit(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void apprenticeEnchantmentAcquisitionFlagsKeepExpectedValues(GameTestHelper helper) {
        EnchantmentApplicationGameTestScenarios.acquisitionFlagsKeepExpectedValues(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellGunsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellGunsKeepExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void commonItemTagsExposeIntendedCompatibilitySurface(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.commonItemTagsExposeIntendedCompatibilitySurface(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordKeepsExpectedStatsAndTags(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellchargedGreatswordKeepsExpectedStatsTagsAndEnchantments(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheKeepsExpectedStatsTagsEnchantmentsAndImbueContract(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios
                .spellReaperScytheKeepsExpectedStatsTagsEnchantmentsAndImbueContract(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void reflectcastShieldKeepsExpectedItemContract(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.reflectcastShieldKeepsExpectedItemContract(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordChargeMathDecayAndAttributes(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellchargedGreatswordChargeMathDecayAndAttributes(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordOverchargeModeConsumesChargeAndUsesConfiguredAttributes(
            GameTestHelper helper
    ) {
        EquipmentEnchantmentSurfaceGameTestScenarios
                .spellchargedGreatswordOverchargeModeConsumesChargeAndUsesConfiguredAttributes(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcharged_greatsword_config")
    public static void spellchargedGreatswordServerConfigOverridesCombatStats(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellchargedGreatswordServerConfigOverridesCombatStats(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordOverchargeActivationPausesDecayUntilRelease(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellchargedGreatswordOverchargeActivationPausesDecayUntilRelease(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordSweepingEdgeBonusAndSweepHitbox(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellchargedGreatswordSweepingEdgeBonusAndSweepHitbox(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordBetterCombatUsesChargedWeaponAttributes(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellchargedGreatswordBetterCombatUsesChargedWeaponAttributes(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordEpicFightUsesSweepingEdgeAndOverchargeRefillsInnate(
            GameTestHelper helper
    ) {
        EquipmentEnchantmentSurfaceGameTestScenarios
                .spellchargedGreatswordEpicFightUsesSweepingEdgeAndOverchargeRefillsInnate(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordChargeEventRequiresMainhand(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellchargedGreatswordChargeEventRequiresMainhand(helper);
    }

    @GameTest(template = "gametest/basic_floor", timeoutTicks = 140)
    public static void spellchargedGreatswordContinuousRecastRefreshesDecayWithoutExtraCharge(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios
                .spellchargedGreatswordContinuousRecastRefreshesDecayWithoutExtraCharge(helper);
    }

    @GameTest(template = "gametest/basic_floor", timeoutTicks = 140)
    public static void spellchargedGreatswordRecastRefreshesDecayWithoutExtraCharge(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios
                .spellchargedGreatswordRecastRefreshesDecayWithoutExtraCharge(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void randomApplicableBookEnchantmentsExcludeFlaskEnchantments(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.randomApplicableBookEnchantmentsExcludeFlaskEnchantments(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletKeepsExpectedStatsAndBenchRules(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.scrollcasterGauntletKeepsExpectedStatsAndBenchEnchantingRules(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletOffhandUseCastsSelectedScrollWhenMainHandDoesNotConsumeUse(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.scrollcasterGauntletOffhandUseCastsSelectedScrollWhenMainHandDoesNotConsumeUse(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletMainhandPrioritizesSupportedOffhandUseItems(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.scrollcasterGauntletMainhandPrioritizesSupportedOffhandUseItems(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void apprenticeMageRobeKeepsExpectedAttributeBonuses(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.apprenticeMageRobeKeepsExpectedAttributeBonuses(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void chromaticMagiaDressKeepsExpectedStatsAndImbueSurface(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.chromaticMagiaDressKeepsExpectedStatsAndImbueSurface(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementMaidenRobeKeepsExpectedStatsAndImbueSurface(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.elementMaidenRobeKeepsExpectedStatsAndImbueSurface(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void forcedAttributeEnchantmentsCoexistAndKeepRawPositiveLevels(GameTestHelper helper) {
        AttributeEnchantmentPolicyGameTestScenarios.forcedEnchantmentsCoexistAndKeepRawPositiveLevels(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void configuredAttributeEnchantmentAmountsKeepDefaultsAndScaleLinearly(GameTestHelper helper) {
        AttributeEnchantmentPolicyGameTestScenarios.configuredAmountsPerLevelKeepDefaultsAndScaleLinearly(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attunementResolutionFailureIsNoOp(GameTestHelper helper) {
        AttributeEnchantmentPolicyGameTestScenarios.attunementResolutionFailureIsNoOp(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magicAttributeMergeOnlyNormalizesLinearMagicAttributes(GameTestHelper helper) {
        AttributeEnchantmentPolicyGameTestScenarios.mergeHelperOnlyNormalizesLinearMagicAttributes(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attributeEnchantmentsApplyInMainhand(GameTestHelper helper) {
        AttributeEnchantmentEffectGameTestScenarios.attributeEnchantmentsApplyInMainhand(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attributeEnchantmentsApplyInOffhand(GameTestHelper helper) {
        AttributeEnchantmentEffectGameTestScenarios.attributeEnchantmentsApplyInOffhand(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attributeEnchantmentsApplyOnArmor(GameTestHelper helper) {
        AttributeEnchantmentEffectGameTestScenarios.attributeEnchantmentsApplyOnArmor(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attributeEnchantmentsApplyInCurios(GameTestHelper helper) {
        AttributeEnchantmentEffectGameTestScenarios.attributeEnchantmentsApplyInCurios(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attributeEnchantmentsMergeWithExistingAttributes(GameTestHelper helper) {
        AttributeEnchantmentEffectGameTestScenarios.attributeEnchantmentsMergeWithExistingAttributes(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magiAgentSuitKeepsExpectedStatsImbueAndCalibrationRune(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.magiAgentSuitKeepsExpectedStatsImbueAndCalibrationRune(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementMaidenRobeSchoolSpellPowerDistributesSpellbookSchools(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.elementMaidenRobeSchoolSpellPowerDistributesSpellbookSchools(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementMaidenRobeSchoolSpellPowerSplitsEmptySlotsBetweenTiedSchools(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.elementMaidenRobeSchoolSpellPowerSplitsEmptySlotsBetweenTiedSchools(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementMaidenRobeSchoolSpellPowerIgnoresHandsAndZeroConfig(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.elementMaidenRobeSchoolSpellPowerIgnoresHandsAndZeroConfig(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementMaidenRobeSchoolSpellPowerRefreshesArchivistsAndEnderGrimoireSources(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.elementMaidenRobeSchoolSpellPowerRefreshesArchivistsAndEnderGrimoireSources(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void stealthRuneArmorKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.stealthRuneArmorKeepsExpectedAttributeBonusesAndImbueSurface(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void soulcollectorRobeAddsLodestoneMagicProficiencyAndMalumInfusions(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.soulcollectorRobeAddsLodestoneMagicProficiencyAndMalumInfusions(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void soulwovenPouchEfficientItemsFollowMalumStoragePolicy(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.soulwovenPouchEfficientItemsFollowMalumStoragePolicy(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void allRegisteredArmorMatchesVanillaEnchantmentSurfaces(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.allRegisteredArmorMatchesVanillaEnchantmentSurfaces(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void chromaticMagiaDressRecordsCastHistoryByArmorTypeAndIgnoresRecasts(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.chromaticMagiaDressRecordsCastHistoryByArmorTypeAndIgnoresRecasts(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void pastelStaffUsesSwordEnchantments(GameTestHelper helper) {
        EquipmentEnchantmentScenarios.pastelStaffUsesSwordEnchantments(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void multicastEchoStaffUsesSwordEnchantments(GameTestHelper helper) {
        EquipmentEnchantmentScenarios.multicastEchoStaffUsesSwordEnchantments(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void crystalBladedStaffKeepsItsDedicatedEnchantingRules(GameTestHelper helper) {
        EquipmentEnchantmentScenarios.crystalBladedStaffKeepsItsDedicatedEnchantingRules(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void chargedTwinBladeStaffAndManaForceBladeAcceptMalumMagicCapableEnchantments(GameTestHelper helper) {
        EquipmentEnchantmentScenarios.chargedTwinBladeStaffAndManaForceBladeAcceptMalumMagicCapableEnchantments(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordClampsPersistedFutureOvercharge(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellchargedGreatswordClampsPersistedFutureOvercharge(helper);
    }
}
