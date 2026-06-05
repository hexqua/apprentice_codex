package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexEquipmentAndEnchantGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String MINING_SPELL_ISOLATED_BATCH = "apprenticecodex.mining_spell_isolated";
    private static final String FOCUS_STAFFBOW_CONTINUOUS_BATCH = "apprenticecodex.focus_staffbow_continuous";
    private static final String FOCUS_STAFFBOW_ARROW_CONFIG_BATCH =
            "apprenticecodex.focus_staffbow_arrow_config";
    private static final String FOCUS_STAFFBOW_ARROW_ITEM_CONFIG_BATCH =
            "apprenticecodex.focus_staffbow_arrow_item_config";
    private static final String FOCUS_STAFFBOW_CONTINUOUS_CONFIG_BATCH =
            "apprenticecodex.focus_staffbow_continuous_config";
    private static final String FOCUS_STAFFBOW_LOAN_CONFIG_BATCH =
            "apprenticecodex.focus_staffbow_loan_config";
    private static final String FOCUS_STAFFBOW_LOAN_RATIO_CONFIG_BATCH =
            "apprenticecodex.focus_staffbow_loan_ratio_config";
    private static final String FOCUS_STAFFBOW_DENYLIST_CONFIG_BATCH =
            "apprenticecodex.focus_staffbow_denylist_config";
    private static final String FOCUS_STAFFBOW_ALLOWLIST_CONFIG_BATCH =
            "apprenticecodex.focus_staffbow_allowlist_config";
    private static final String SPELLGUN_CONFIG_BATCH = "apprenticecodex.spellgun_config";
    private static final String ELEMENTAL_BOW_OVERHEAT_BATCH = "apprenticecodex.elemental_bow_overheat";
    private static final String ELEMENTAL_BOW_DRAW_CONFIG_BATCH =
            "apprenticecodex.elemental_bow_draw_config";
    private static final String ELEMENTAL_BOW_OVERHEAT_MANA_CONFIG_BATCH =
            "apprenticecodex.elemental_bow_overheat_mana_config";
    private static final String ELEMENTAL_BOW_OVERHEAT_DURATION_CONFIG_BATCH =
            "apprenticecodex.elemental_bow_overheat_duration_config";
    private static final String ELEMENTAL_BOW_POWER_CONFIG_BATCH =
            "apprenticecodex.elemental_bow_power_config";
    private static final String SPELLCASTER_QUIVER_ISOLATED_BATCH = "apprenticecodex.spellcaster_quiver_isolated";
    private static final String CIRCUIT_HEAT_STAFF_ADDITIONAL_MANA_CONFIG_BATCH =
            "apprenticecodex.circuit_heat_staff_additional_mana_config";
    private static final String CIRCUIT_HEAT_STAFF_OVERHEAT_MIN_CONFIG_BATCH =
            "apprenticecodex.circuit_heat_staff_overheat_min_config";
    private static final String CIRCUIT_HEAT_STAFF_OVERHEAT_CAP_CONFIG_BATCH =
            "apprenticecodex.circuit_heat_staff_overheat_cap_config";
    private static final String CIRCUIT_HEAT_STAFF_COOLDOWN_LIMIT_CONFIG_BATCH =
            "apprenticecodex.circuit_heat_staff_cooldown_limit_config";
    private static final String CIRCUIT_HEAT_STAFF_SPELL_DENYLIST_CONFIG_BATCH =
            "apprenticecodex.circuit_heat_staff_spell_denylist_config";
    private static final String CIRCUIT_HEAT_STAFF_COOLING_DISABLED_CONFIG_BATCH =
            "apprenticecodex.circuit_heat_staff_cooling_disabled_config";
    private static final String CIRCUIT_HEAT_STAFF_WATER_CONSUMPTION_CONFIG_BATCH =
            "apprenticecodex.circuit_heat_staff_water_consumption_config";
    private static final String CIRCUIT_HEAT_STAFF_CAULDRON_CONSUMPTION_CONFIG_BATCH =
            "apprenticecodex.circuit_heat_staff_cauldron_consumption_config";
    private static final String MANA_SHIELD_CHARM_FREE_COST_CONFIG_BATCH =
            "apprenticecodex.mana_shield_charm_free_cost_config";
    private static final String MANA_SHIELD_CHARM_RECOVERY_CONFIG_BATCH =
            "apprenticecodex.mana_shield_charm_recovery_config";
    private static final String MANA_SHIELD_CHARM_SYNCHRONIZATION_CONFIG_BATCH =
            "apprenticecodex.mana_shield_charm_synchronization_config";
    private static final String MANA_SHIELD_CHARM_NEUTRALIZATION_CONFIG_BATCH =
            "apprenticecodex.mana_shield_charm_neutralization_config";
    private static final String MANA_SHIELD_CHARM_SHELL_CONFIG_BATCH =
            "apprenticecodex.mana_shield_charm_shell_config";
    private static final String MANA_SHIELD_CHARM_IFRAME_CONFIG_BATCH =
            "apprenticecodex.mana_shield_charm_iframe_config";

    private ApprenticeCodexEquipmentAndEnchantGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void copperSpellAmplifierStartsWithBallLightningAndStacksAttunement(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.copperSpellAmplifierStartsWithBallLightningAndStacksAttunement(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void copperSwingcastStaffStartsWithBallLightningLevelOne(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.copperSwingcastStaffStartsWithBallLightningLevelOne(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void goldSpellcasterGunImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.goldSpellcasterGunImbuedSpellStaysRemovableAfterNormalization(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcasterGunRecastImbueRestrictionsMatchTier(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellcasterGunRecastImbueRestrictionsMatchTier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellgunServerConfigDefaultsMatchCurrentHardcodedValues(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellgunServerConfigDefaultsMatchCurrentHardcodedValues(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLGUN_CONFIG_BATCH)
    public static void spellgunZeroImbueCooldownLimitDisablesOnlyCooldownLimit(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellgunZeroImbueCooldownLimitDisablesOnlyCooldownLimit(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLGUN_CONFIG_BATCH)
    public static void spellgunZeroCastCooldownConfigForcesZeroCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellgunZeroCastCooldownConfigForcesZeroCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcasterGunRecastCastBypassesAmmoRequirement(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellcasterGunRecastCastBypassesAmmoRequirement(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void copperSwingcastStaffReplacementSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.copperSwingcastStaffReplacementSpellStaysRemovableAfterNormalization(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void reflectcastShieldImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.reflectcastShieldImbuedSpellStaysRemovableAfterNormalization(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void goldSpellcasterGunImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.goldSpellcasterGunImbuedSpellStaysRemovableAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void ironSwingcastStaffImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.ironSwingcastStaffImbuedSpellStaysRemovableAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void copperSwingcastStaffPresetEquivalentSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.copperSwingcastStaffPresetEquivalentSpellStaysRemovableAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void reflectcastShieldImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.reflectcastShieldImbuedSpellStaysRemovableAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void photonSiphonStartsWithLockedManaChargeAndIsNotUnique(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.photonSiphonStartsWithLockedManaChargeAndIsNotUnique(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void photonSiphonCalibrationRepairUnlocksLegacyReplacementOnly(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.photonSiphonCalibrationRepairUnlocksLegacyReplacementOnly(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCalibrationBenchTargetsExposeExpectedSlots(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellCalibrationBenchTargetsExposeExpectedSlots(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCalibrationBenchImbueOnlySupportsExtractableTargets(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellCalibrationBenchImbueOnlySupportsExtractableTargets(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void reflectcastShieldDurabilityRulesMatchGuardTuning(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.reflectcastShieldDurabilityRulesMatchGuardTuning(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletStartsWithSingleHiddenSpellSlotAndLoadedAllowlist(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autocastAmuletStartsWithSingleHiddenSpellSlotAndLoadedAllowlist(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmUsesCharmSlotAndAppearsInCreativeTab(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmUsesCharmSlotAndAppearsInCreativeTab(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmExclusiveEnchantmentsStayMutuallyExclusive(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmExclusiveEnchantmentsStayMutuallyExclusive(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmFullyNegatesDamageAndPreservesArmorDurability(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmFullyNegatesDamageAndPreservesArmorDurability(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmBurnedOutFullNegateCancelsHitAndStartsCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmBurnedOutFullNegateCancelsHitAndStartsCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmLowManaBurnedOutFullNegateStillCancelsHit(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmLowManaBurnedOutFullNegateStillCancelsHit(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmDoesNotRespendManaDuringVanillaStyleIFrame(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmDoesNotRespendManaDuringVanillaStyleIFrame(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmPartialReductionEntersCooldownAndKeepsArmorMitigation(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmPartialReductionEntersCooldownAndKeepsArmorMitigation(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmCooldownRecoversAtOneHundredMana(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmCooldownRecoversAtOneHundredMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmShellUsesArmorOnlyOnNormalDamageAndWearsArmor(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmShellUsesArmorOnlyOnNormalDamageAndWearsArmor(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmShellLowManaBurnoutStillUsesArmorPath(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmShellLowManaBurnoutStillUsesArmorPath(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmSynchronizationChargesEnchantReductionBeforeNormalBarrier(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmSynchronizationChargesEnchantReductionBeforeNormalBarrier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmSynchronizationBurnoutStopsAfterEnchantReduction(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmSynchronizationBurnoutStopsAfterEnchantReduction(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmSynchronizationLowManaBurnoutStopsAfterEnchantStage(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmSynchronizationLowManaBurnoutStopsAfterEnchantStage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmSynchronizationLowManaBurnoutAfterBarrierStage(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmSynchronizationLowManaBurnoutAfterBarrierStage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmNeutralizationAbsorbsBypassArmorDamageDuringCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmNeutralizationAbsorbsBypassArmorDamageDuringCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_FREE_COST_CONFIG_BATCH)
    public static void manaShieldCharmFreeManaCostConfigAbsorbsWithoutDepletionCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmFreeManaCostConfigAbsorbsWithoutDepletionCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_RECOVERY_CONFIG_BATCH)
    public static void manaShieldCharmZeroRecoveryThresholdDisablesDepletionCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmZeroRecoveryThresholdDisablesDepletionCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_SYNCHRONIZATION_CONFIG_BATCH)
    public static void manaShieldCharmSynchronizationManaCostUsesServerConfig(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmSynchronizationManaCostUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_NEUTRALIZATION_CONFIG_BATCH)
    public static void manaShieldCharmNeutralizationZeroRecoveryStillNullifies(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmNeutralizationZeroRecoveryStillNullifies(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_SHELL_CONFIG_BATCH)
    public static void manaShieldCharmShellArmorDurabilityDamageUsesServerConfig(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmShellArmorDurabilityDamageUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_IFRAME_CONFIG_BATCH)
    public static void manaShieldCharmInvulnerableTimeUsesServerConfig(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaShieldCharmInvulnerableTimeUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNormalizationDropsBlockedSpellsAndClampsSlots(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autocastAmuletNormalizationDropsBlockedSpellsAndClampsSlots(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletSpellSlotUpgradeStopsAtThreeAndKeepsOrder(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autocastAmuletSpellSlotUpgradeStopsAtThreeAndKeepsOrder(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletWorkbenchExtractionUsesLastSpellAndKeepsSlotCount(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autocastAmuletWorkbenchExtractionUsesLastSpellAndKeepsSlotCount(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletAutoCastStartsOnFirstIntervalAfterEquip(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autocastAmuletAutoCastStartsOnFirstIntervalAfterEquip(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletInsufficientManaDelaysRetryAndSkipsErroredSlotOnce(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autocastAmuletInsufficientManaDelaysRetryAndSkipsErroredSlotOnce(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletCreativeCastIgnoresManaCost(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autocastAmuletCreativeCastIgnoresManaCost(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletCooldownUsesHelperAmountWithoutSwordMultiplier(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autocastAmuletCooldownUsesHelperAmountWithoutSwordMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletLongSpellCompletesImmediately(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autocastAmuletLongSpellCompletesImmediately(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNotificationControllerSchedulesCastAndThresholds(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autocastAmuletNotificationControllerSchedulesCastAndThresholds(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNotificationControllerSkipsUnreachedThresholds(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autocastAmuletNotificationControllerSkipsUnreachedThresholds(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNotificationControllerQueuesInOrderAndKeepsDelayedLabel(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autocastAmuletNotificationControllerQueuesInOrderAndKeepsDelayedLabel(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void ironSpellcasterGunExtractedSpellStaysClearedAfterSaveLoad(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.ironSpellcasterGunExtractedSpellStaysClearedAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void ironSwingcastStaffExtractedSpellStaysClearedAfterSaveLoad(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.ironSwingcastStaffExtractedSpellStaysClearedAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void goldSpellcasterGunLegacyLockedReplacementIsRecoveredAfterSaveLoad(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.goldSpellcasterGunLegacyLockedReplacementIsRecoveredAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void ironSwingcastStaffLegacyLockedReplacementIsRecoveredAfterSaveLoad(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.ironSwingcastStaffLegacyLockedReplacementIsRecoveredAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void diamondAndNetheriteSpellAmplifierExposeNewAttributeBonuses(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.diamondAndNetheriteSpellAmplifierExposeNewAttributeBonuses(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void upgradeWhitelistCoversTargetAbstractItems(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.upgradeWhitelistCoversTargetAbstractItems(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellGunsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellGunsKeepExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void offhandMagicItemsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.offhandMagicItemsKeepExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void swingMagicWeaponsUseBaseAttackModifierIds(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.swingMagicWeaponsUseBaseAttackModifierIds(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffUpgradeMergesMainhandMeleeDamage(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffUpgradeMergesMainhandMeleeDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaForceBladeAttunementAndUpgradeMergeForTooltip(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaForceBladeAttunementAndUpgradeMergeForTooltip(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaForceBladeAttackManaCostIsOncePerTick(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaForceBladeAttackManaCostIsOncePerTick(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaForceBladeConfigScalesDamageAndManaFormulas(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaForceBladeConfigScalesDamageAndManaFormulas(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatSpellbreakerIsTwoHandedAndAmplifierHasOffhandSpellPower(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.betterCombatSpellbreakerIsTwoHandedAndAmplifierHasOffhandSpellPower(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatOffhandOnlyGauntletDoesNotForceDualWielding(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.betterCombatOffhandOnlyGauntletDoesNotForceDualWielding(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatOffhandRescueIncludesEnchantAndImbueDerivedModifiers(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.betterCombatOffhandRescueIncludesEnchantAndImbueDerivedModifiers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.betterCombatRescueUsesPhysicalOffhandInventoryStack(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatSpellSelectionRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.betterCombatSpellSelectionRescueUsesPhysicalOffhandInventoryStack(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatScrollcasterGauntletRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.betterCombatScrollcasterGauntletRescueUsesPhysicalOffhandInventoryStack(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void dynamicCastingMobilityEffectRebalancesAgainstExternalCastingMoveSpeed(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.dynamicCastingMobilityEffectRebalancesAgainstExternalCastingMoveSpeed(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantedCircletKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.enchantedCircletKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantedCircletCurioBonusesMirrorOffhandMagicEnchantments(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.enchantedCircletCurioBonusesMirrorOffhandMagicEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantedCircletWorkbenchExtractionTagDoesNotAffectAshenCirclet(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.enchantedCircletWorkbenchExtractionTagDoesNotAffectAshenCirclet(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantedCircletWisdomMatchesArmorRate(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.enchantedCircletWisdomMatchesArmorRate(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void wisdomAppliesToBlockBreakExperienceAndRoundsUp(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.wisdomAppliesToBlockBreakExperienceAndRoundsUp(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void rightClickMagicWeaponsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.rightClickMagicWeaponsKeepExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void rightClickMagicWeaponTooltipsStartWithShieldHint(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.rightClickMagicWeaponTooltipsStartWithShieldHint(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowKeepsVanillaBowEnchantmentSurfaces(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowKeepsVanillaBowEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowBuildsSelectionViewsFromHeldAmmo(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowBuildsSelectionViewsFromHeldAmmo(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowInventoryOverlayReflectsCurrentSelection(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowInventoryOverlayReflectsCurrentSelection(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH, timeoutTicks = 80)
    public static void elementalBowSelectionViewExposesOverheatOverlayState(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowSelectionViewExposesOverheatOverlayState(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowKeepsCurrentEmptySpecialSelectionOnlyWhileSelected(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowKeepsCurrentEmptySpecialSelectionOnlyWhileSelected(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowRequiresManaBeforeStartingElementalDraw(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowRequiresManaBeforeStartingElementalDraw(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowDoesNotConsumeResourcesBeforeFullDraw(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowDoesNotConsumeResourcesBeforeFullDraw(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowInfinityAllowsVanillaDrawWithoutArrows(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowInfinityAllowsVanillaDrawWithoutArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowInfinityAllowsArrowModeDrawWithoutArrows(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowInfinityAllowsArrowModeDrawWithoutArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowVanillaModeInfinityDoesNotConsumeNormalArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowVanillaModeInfinityDoesNotConsumeNormalArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowArrowModeInfinityDoesNotConsumeNormalArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowArrowModeInfinityDoesNotConsumeNormalArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowSpecialModeConsumesLastArrowAndKeepsSelection(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowSpecialModeConsumesLastArrowAndKeepsSelection(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowMagicModeIgnoresInfinityWithoutAmmo(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowMagicModeIgnoresInfinityWithoutAmmo(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowAcceptsSynthesisEnchantmentsAndTooltip(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowAcceptsSynthesisEnchantmentsAndTooltip(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowSynthesisAllowsMagicModeWithoutArrows(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowSynthesisAllowsMagicModeWithoutArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowSynthesisDoesNotConsumeMagicModeArrows(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowSynthesisDoesNotConsumeMagicModeArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void spellcasterQuiverUsesBackSlotAndCapsStoredArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.spellcasterQuiverUsesBackSlotAndCapsStoredArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void equippedSpellcasterQuiverAutoStoresPickedUpArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.equippedSpellcasterQuiverAutoStoresPickedUpArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void elementalBowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.elementalBowConsumesSpellcasterQuiverArrowsBeforeInventory(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void elementalBowSelectionViewsIncludeSpellcasterQuiverArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.elementalBowSelectionViewsIncludeSpellcasterQuiverArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void vanillaBowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.vanillaBowConsumesSpellcasterQuiverArrowsBeforeInventory(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void vanillaBowPrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.vanillaBowPrefersHeldSpecialArrowOverQuiverNormalArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void vanillaBowPrefersNormalArrowOverMoreNumerousQuiverSpecialArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.vanillaBowPrefersNormalArrowOverMoreNumerousQuiverSpecialArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void vanillaBowInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.vanillaBowInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void elementalBowVanillaModePrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.elementalBowVanillaModePrefersHeldSpecialArrowOverQuiverNormalArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void elementalBowVanillaModeInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.elementalBowVanillaModeInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowNonMagicModesHideDerivedSpellPresentation(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowNonMagicModesHideDerivedSpellPresentation(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void spellcasterQuiverSlowdownHelperTracksEquippedBowUse(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.spellcasterQuiverSlowdownHelperTracksEquippedBowUse(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void spellcasterQuiverSlowdownHelperTracksFocusStaffbowDrawUse(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.spellcasterQuiverSlowdownHelperTracksFocusStaffbowDrawUse(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH)
    public static void elementalBowCooldownHelperIgnoresWeaponMultiplierButKeepsPlayerCooldownReduction(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowCooldownHelperIgnoresWeaponMultiplierButKeepsPlayerCooldownReduction(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH)
    public static void elementalBowSuppressesElementalArrowCooldown(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowSuppressesElementalArrowCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH)
    public static void elementalBowConsumesAdditionalManaWhileOverheated(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowConsumesAdditionalManaWhileOverheated(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH)
    public static void elementalBowOverheatTracksSchoolsSeparately(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowOverheatTracksSchoolsSeparately(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH, timeoutTicks = 80)
    public static void elementalBowOverheatRefreshesDurationAfterRepeatedCast(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowOverheatRefreshesDurationAfterRepeatedCast(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowRejectsOffhandUse(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowRejectsOffhandUse(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowAllowsMainhandUseWithOffhandSelection(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowAllowsMainhandUseWithOffhandSelection(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowRejectsUseWithoutArrowCatalyst(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowRejectsUseWithoutArrowCatalyst(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowShowsLongSummonWeaponDuringPendingCast(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowShowsLongSummonWeaponDuringPendingCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowCancelsPendingSummonWeaponBeforeRequiredCharge(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowCancelsPendingSummonWeaponBeforeRequiredCharge(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowContinuousCastStaysActivePastSpellDuration(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowContinuousCastStaysActivePastSpellDuration(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowContinuousCastUsesStandardCastTimeWithoutAttributeAdjustment(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowContinuousCastUsesStandardCastTimeWithoutAttributeAdjustment(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowContinuousCastStopsWhenManaRunsOut(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowContinuousCastStopsWhenManaRunsOut(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowInstantImmediateReleaseConsumesBaseMana(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowInstantImmediateReleaseConsumesBaseMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowShortLongReleaseStaysAtBaseMultiplier(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowShortLongReleaseStaysAtBaseMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowConfigCurveAndManaFormulaUsesFixedTimeToMax(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowConfigCurveAndManaFormulaUsesFixedTimeToMax(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowStillRejectsCastWhenBaseManaIsInsufficient(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowStillRejectsCastWhenBaseManaIsInsufficient(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_ARROW_CONFIG_BATCH)
    public static void focusStaffbowArrowRequirementConfigAllowsArrowlessCasting(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowArrowRequirementConfigAllowsArrowlessCasting(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_CONFIG_BATCH)
    public static void focusStaffbowContinuousConfigRejectsWithoutConsumingArrow(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowContinuousConfigRejectsWithoutConsumingArrow(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_LOAN_CONFIG_BATCH)
    public static void focusStaffbowManaLoanConfigRejectsBorrowedPendingCast(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowManaLoanConfigRejectsBorrowedPendingCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_LOAN_RATIO_CONFIG_BATCH)
    public static void focusStaffbowLoanRatioConfigRejectsExcessBorrowing(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowLoanRatioConfigRejectsExcessBorrowing(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_DENYLIST_CONFIG_BATCH)
    public static void focusStaffbowSpellDenylistBlocksBeforeAmmo(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowSpellDenylistBlocksBeforeAmmo(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_ALLOWLIST_CONFIG_BATCH)
    public static void focusStaffbowSpellAllowlistBlocksMissingSpellBeforeAmmo(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowSpellAllowlistBlocksMissingSpellBeforeAmmo(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowOverchargeLoanConsumesRecoveredMana(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowOverchargeLoanConsumesRecoveredMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowCreativeOverchargeDoesNotConsumeManaOrCreateLoan(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowCreativeOverchargeDoesNotConsumeManaOrCreateLoan(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowBlocksUseWhileLoanRemains(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowBlocksUseWhileLoanRemains(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowRejectsUseWhileSpellCooldownRemains(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowRejectsUseWhileSpellCooldownRemains(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowLoanMessageUsesExpectedTranslationKey(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowLoanMessageUsesExpectedTranslationKey(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowInsufficientArrowMessageUsesExpectedTranslationKey(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowInsufficientArrowMessageUsesExpectedTranslationKey(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowRejectsUnconfiguredSpecialArrow(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowRejectsUnconfiguredSpecialArrow(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_ARROW_ITEM_CONFIG_BATCH)
    public static void focusStaffbowArrowCatalystItemListAllowsConfiguredSpecialArrow(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowArrowCatalystItemListAllowsConfiguredSpecialArrow(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowSynthesisAllowsArrowlessCasting(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowSynthesisAllowsArrowlessCasting(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void focusStaffbowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.focusStaffbowConsumesSpellcasterQuiverArrowsBeforeInventory(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowAcceptsSynthesisEnchantments(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowAcceptsSynthesisEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowExposesExpectedMainhandAttributes(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowExposesExpectedMainhandAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaForceBladeKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaForceBladeKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffExposesExpectedMainhandAttributes(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffExposesExpectedMainhandAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffResolveThrownDamageIncludesApplicableEnchantments(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffResolveThrownDamageIncludesApplicableEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffThrowConsumesMana(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffThrowConsumesMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffLoyaltyReducesThrowManaCost(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffLoyaltyReducesThrowManaCost(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffRiptideWorksOnDryGroundWithoutProjectile(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffRiptideWorksOnDryGroundWithoutProjectile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactForwardUsesHistoryAndFallback(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffImpactForwardUsesHistoryAndFallback(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerCastsInstantAndLongSpells(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffImpactCastManagerCastsInstantAndLongSpells(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerCastsInstantWhileOwnerBusy(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffImpactCastManagerCastsInstantWhileOwnerBusy(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffBusyFallbackDoesNotBypassCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffBusyFallbackDoesNotBypassCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellThrowableCardImpactCastManagerCastsInstantWhileOwnerBusy(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellThrowableCardImpactCastManagerCastsInstantWhileOwnerBusy(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffRemoteOwnerDenylistBlocksRuntimeWithoutFallback(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffRemoteOwnerDenylistBlocksRuntimeWithoutFallback(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerCastsPlayerSelfProfile(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffImpactCastManagerCastsPlayerSelfProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffCreativeImpactCastUsesRemoteOwnerProfileWithZeroMana(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffCreativeImpactCastUsesRemoteOwnerProfileWithZeroMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffCreativeImpactCastUsesStaffProfileWithZeroMana(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffCreativeImpactCastUsesStaffProfileWithZeroMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerCastsInitialRaiseDeadProfile(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffImpactCastManagerCastsInitialRaiseDeadProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerBlocksRaiseDeadWhenRecastExists(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffImpactCastManagerBlocksRaiseDeadWhenRecastExists(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffRaiseDeadPreservesWheelSelectionAfterRecast(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffRaiseDeadPreservesWheelSelectionAfterRecast(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerRejectsUnprofiledSpell(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffImpactCastManagerRejectsUnprofiledSpell(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerStartsContinuousSpells(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffImpactCastManagerStartsContinuousSpells(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffContinuousRemoteOwnerIgnoresMissingDispenserProfile(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffContinuousRemoteOwnerIgnoresMissingDispenserProfile(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void chargedTwinBladeStaffContinuousThrowableCardUsesCardCooldownPolicy(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffContinuousThrowableCardUsesCardCooldownPolicy(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerSkipsWhenOwnerCannotCast(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffImpactCastManagerSkipsWhenOwnerCannotCast(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowFallsBackToNoneWhenLegacyModeCannotResolve(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowFallsBackToNoneWhenLegacyModeCannotResolve(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowSynchronizesSpellContainerToCurrentMode(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowSynchronizesSpellContainerToCurrentMode(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowSpellContainerAppliesPowerFlameAndClearsInNoneMode(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowSpellContainerAppliesPowerFlameAndClearsInNoneMode(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowDoesNotAddDerivedSpellToMainhandSpellWheel(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowDoesNotAddDerivedSpellToMainhandSpellWheel(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowBlocksArcaneAnvilImbueViaSpellValidator(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowBlocksArcaneAnvilImbueViaSpellValidator(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void mithrilFreecastStaffBlocksArcaneAnvilImbueViaSpellValidator(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mithrilFreecastStaffBlocksArcaneAnvilImbueViaSpellValidator(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowManaErrorUsesIronsSpellbooksTranslationKey(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowManaErrorUsesIronsSpellbooksTranslationKey(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowVanillaModeConsumesSpecialArrowWhenNormalArrowsAreMissing(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowVanillaModeConsumesSpecialArrowWhenNormalArrowsAreMissing(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowArrowModeRequiresNormalArrowsEvenWhenSpecialArrowsExist(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowArrowModeRequiresNormalArrowsEvenWhenSpecialArrowsExist(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_DRAW_CONFIG_BATCH)
    public static void elementalBowMagicDrawTicksUseProfileAndServerMultiplier(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowMagicDrawTicksUseProfileAndServerMultiplier(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_MANA_CONFIG_BATCH)
    public static void elementalBowAdditionalManaUsesServerConfig(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowAdditionalManaUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_DURATION_CONFIG_BATCH)
    public static void elementalBowOverheatDurationUsesServerConfig(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowOverheatDurationUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_POWER_CONFIG_BATCH)
    public static void elementalBowPowerSpellLevelBonusUsesServerConfig(GameTestHelper helper) {
        ElementalBowGameTestScenarios.elementalBowPowerSpellLevelBonusUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void reflectcastShieldKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.reflectcastShieldKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellcastersFlaskKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.alchemistsFlaskKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void apprenticeEnchantmentsKeepExpectedAcquisitionFlags(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.apprenticeEnchantmentsKeepExpectedAcquisitionFlags(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void randomApplicableBookEnchantmentsExcludeFlaskEnchantments(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.randomApplicableBookEnchantmentsExcludeFlaskEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void magicArmorKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.magicArmorKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void scrollcasterGauntletKeepsExpectedStatsAndBenchEnchantingRules(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.scrollcasterGauntletKeepsExpectedStatsAndBenchEnchantingRules(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void scrollcasterGauntletOffhandUseCastsSelectedScrollWhenMainHandDoesNotConsumeUse(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.scrollcasterGauntletOffhandUseCastsSelectedScrollWhenMainHandDoesNotConsumeUse(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void apprenticeMageRobeKeepsExpectedAttributeBonuses(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.apprenticeMageRobeKeepsExpectedAttributeBonuses(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chromaticMagiaDressKeepsExpectedStatsAndImbueSurface(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chromaticMagiaDressKeepsExpectedStatsAndImbueSurface(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementMaidenRobeKeepsExpectedStatsImbueAndMagicEnchantments(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementMaidenRobeKeepsExpectedStatsImbueAndMagicEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementMaidenRobeSchoolSpellPowerDistributesSpellbookSchools(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementMaidenRobeSchoolSpellPowerDistributesSpellbookSchools(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementMaidenRobeSchoolSpellPowerSplitsEmptySlotsBetweenTiedSchools(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementMaidenRobeSchoolSpellPowerSplitsEmptySlotsBetweenTiedSchools(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementMaidenRobeSchoolSpellPowerIgnoresHandsAndZeroConfig(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementMaidenRobeSchoolSpellPowerIgnoresHandsAndZeroConfig(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementMaidenRobeSchoolSpellPowerRefreshesArchivistsAndEnderGrimoireSources(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementMaidenRobeSchoolSpellPowerRefreshesArchivistsAndEnderGrimoireSources(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void stealthRuneArmorKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.stealthRuneArmorKeepsExpectedAttributeBonusesAndImbueSurface(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chromaticMagiaDressRecordsCastHistoryByArmorTypeAndIgnoresRecasts(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chromaticMagiaDressRecordsCastHistoryByArmorTypeAndIgnoresRecasts(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void pastelStaffKeepsItsExtraMiningEnchantments(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.pastelStaffKeepsItsExtraMiningEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multicastEchoStaffKeepsItsExtraMiningEnchantments(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffKeepsItsExtraMiningEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffKeepsItsDedicatedEnchantingRules(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.crystalBladedStaffKeepsItsDedicatedEnchantingRules(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void circuitHeatStaffKeepsExpectedStatsAndEnchantingRules(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffKeepsExpectedStatsAndEnchantingRules(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleKeepsExpectedStatsAndEnchantingRules(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multipurposeStaffrifleKeepsExpectedStatsAndEnchantingRules(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleTooltipShowsControlsBeforeShiftHint(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multipurposeStaffrifleTooltipShowsControlsBeforeShiftHint(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleSpecialCooldownPolicyMatchesDefaults(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multipurposeStaffrifleSpecialCooldownPolicyMatchesDefaults(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleUsesDedicatedAmmoAndCasingReturnPolicy(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multipurposeStaffrifleUsesDedicatedAmmoAndCasingReturnPolicy(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleRecastSkipsAmmoConsumption(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multipurposeStaffrifleRecastSkipsAmmoConsumption(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleKeepsNormalManaCost(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multipurposeStaffrifleKeepsNormalManaCost(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleInstantCastConsumesAmmoAndAppliesCooldownPolicy(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multipurposeStaffrifleInstantCastConsumesAmmoAndAppliesCooldownPolicy(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void circuitHeatStaffAdditionalManaScalesWithSkippedCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffAdditionalManaScalesWithSkippedCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_ADDITIONAL_MANA_CONFIG_BATCH)
    public static void circuitHeatStaffAdditionalManaUsesServerConfig(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffAdditionalManaUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void circuitHeatStaffOverheatUsesCastCooldownPlusSkippedCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffOverheatUsesCastCooldownPlusSkippedCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_OVERHEAT_MIN_CONFIG_BATCH)
    public static void circuitHeatStaffOverheatDurationUsesServerMinTicks(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffOverheatDurationUsesServerMinTicks(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_OVERHEAT_CAP_CONFIG_BATCH)
    public static void circuitHeatStaffOverheatDurationUsesServerCapTicks(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffOverheatDurationUsesServerCapTicks(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void circuitHeatStaffBypassKeepsBaseManaGate(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffBypassKeepsBaseManaGate(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_COOLDOWN_LIMIT_CONFIG_BATCH)
    public static void circuitHeatStaffCooldownLimitBlocksBypass(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffCooldownLimitBlocksBypass(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_SPELL_DENYLIST_CONFIG_BATCH)
    public static void circuitHeatStaffSpellDenylistBlocksBypass(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffSpellDenylistBlocksBypass(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void circuitHeatStaffRightClickBypassesCooldownButShortcutDoesNot(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffRightClickBypassesCooldownButShortcutDoesNot(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void circuitHeatStaffContinuousBypassKeepsOverheatManaCost(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffContinuousBypassKeepsOverheatManaCost(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void circuitHeatStaffRecastDoesNotTouchBypassOverheatState(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffRecastDoesNotTouchBypassOverheatState(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingConsumesWaterSource(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffDropCoolingConsumesWaterSource(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_COOLING_DISABLED_CONFIG_BATCH, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingDisabledByServerConfig(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffDropCoolingDisabledByServerConfig(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingIgnoresFlowingWater(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffDropCoolingIgnoresFlowingWater(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_WATER_CONSUMPTION_CONFIG_BATCH, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingKeepsWaterSourceWhenConsumptionDisabled(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffDropCoolingKeepsWaterSourceWhenConsumptionDisabled(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingConsumesCauldronLevel(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffDropCoolingConsumesCauldronLevel(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_CAULDRON_CONSUMPTION_CONFIG_BATCH, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingKeepsWaterCauldronWhenConsumptionDisabled(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffDropCoolingKeepsWaterCauldronWhenConsumptionDisabled(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingKeepsPowderSnowBlock(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffDropCoolingKeepsPowderSnowBlock(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingKeepsPowderSnowCauldronLevel(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffDropCoolingKeepsPowderSnowCauldronLevel(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingIgnoresNonOverheatedStaffInPowderSnow(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffDropCoolingIgnoresNonOverheatedStaffInPowderSnow(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingIgnoresNonOverheatedStaff(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffDropCoolingIgnoresNonOverheatedStaff(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffAndManaForceBladeAcceptMalumMagicCapableEnchantments(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffAndManaForceBladeAcceptMalumMagicCapableEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void hauntedBonusDamageTypeStaysOnMagicDamageTagPath(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.hauntedBonusDamageTypeStaysOnMagicDamageTagPath(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void hauntedBonusDamageActuallyScalesWithLodestoneMagicProficiency(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.hauntedBonusDamageActuallyScalesWithLodestoneMagicProficiency(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void uniteLunaStaffStartsWithUniteLunaAndExpectedMainhandBonuses(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.uniteLunaStaffStartsWithUniteLunaAndExpectedMainhandBonuses(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void rightClickMagicWeaponsExposeBaseAttributesAsComponents(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.rightClickMagicWeaponsExposeBaseAttributesAsComponents(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void mainhandUpgradeBridgeAppliesStoredUpgradeDataToSpellGunsAndWeapons(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mainhandUpgradeBridgeAppliesStoredUpgradeDataToSpellGunsAndWeapons(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void castingMoveSpeedAdjustmentStopsAtNormalSpeedWithoutNegativeCorrections(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.castingMoveSpeedAdjustmentStopsAtNormalSpeedWithoutNegativeCorrections(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void longStrideMobilityStillAddsBaseMovementSpeedBonus(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.longStrideMobilityStillAddsBaseMovementSpeedBonus(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void comfortBerriesProvideManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.comfortBerriesProvideManaRegenerationAndExpectedFoodValues(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void comfortSandwichProvidesManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.comfortSandwichProvidesManaRegenerationAndExpectedFoodValues(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void meditationPotionsExposeExpectedEffectsAndDurations(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.meditationPotionsExposeExpectedEffectsAndDurations(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void swingcastStaffTiersExposeRequestedImbueRules(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.swingcastStaffTiersExposeRequestedImbueRules(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void craftsmansDelightAppliesToExternalSpellManaAndCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.craftsmansDelightAppliesToExternalSpellManaAndCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void craftsmansDelightAppliesToHarvestMoonAndEarthForgeManaAndCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.craftsmansDelightAppliesToHarvestMoonAndEarthForgeManaAndCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void craftsmansDelightExtendsTouchDigRange(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.craftsmansDelightExtendsTouchDigRange(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void touchDigMergesRingMiningEnchantments(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.touchDigMergesRingMiningEnchantments(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void touchDigUsesRingMiningEnchantmentsWhenCastBareHanded(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.touchDigUsesRingMiningEnchantmentsWhenCastBareHanded(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void spectralHammerUsesCraftsmansDelightRingMiningEnchantments(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spectralHammerUsesCraftsmansDelightRingMiningEnchantments(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void tinyLumberjackWithCraftsmansDelightMovesJobDropsToOrigin(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.tinyLumberjackWithCraftsmansDelightMovesJobDropsToOrigin(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void tinyLumberjackDropMoveFollowsCurrentCraftsmansDelightEquipment(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.tinyLumberjackDropMoveFollowsCurrentCraftsmansDelightEquipment(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void worldFlatterPenetratedArmorEffectAndDamageTags(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.worldFlatterPenetratedArmorEffectAndDamageTags(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void worldFlatterBlockTargetFilterMatchesPickaxeOrShovel(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.worldFlatterBlockTargetFilterMatchesPickaxeOrShovel(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void worldFlatterEntityAttackRequiresArrivalAndHitsSingleTarget(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.worldFlatterEntityAttackRequiresArrivalAndHitsSingleTarget(helper);
    }
}
