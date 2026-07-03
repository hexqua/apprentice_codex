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
    private static final String CRAFTSMANS_DELIGHT_GRACED_RAIN_DENYLIST_CONFIG_BATCH =
            "apprenticecodex.craftsmans_delight_graced_rain_denylist_config";
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
    private static final String SPELL_CAST_PARRYING_RING_CONFIG_BATCH =
            "apprenticecodex.spell_cast_parrying_ring_config";
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
    private static final String MANA_THRUSTER_CONFIG_BATCH =
            "apprenticecodex.mana_thruster_config";
    private static final String MAGI_COMPRESSOR_GADGET_CONFIG_BATCH =
            "apprenticecodex.magi_compressor_gadget_config";
    private static final String JUMPCAST_CHARM_CONFIG_BATCH =
            "apprenticecodex.jumpcast_charm_config";
    private static final String SPELLCHARGED_GREATSWORD_CONFIG_BATCH =
            "apprenticecodex.spellcharged_greatsword_config";

    private ApprenticeCodexEquipmentAndEnchantGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void copperSpellAmplifierStartsWithBallLightningAndStacksAttunement(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.copperSpellAmplifierStartsWithBallLightningAndStacksAttunement(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeStartsWithEdgeDancerAndExpectedStats(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeStartsWithEdgeDancerAndExpectedStats(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeKeepsExpectedTagsAndEnchantments(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeKeepsExpectedTagsAndEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeUsesArcaneIngotRepairAndMirrorIsNotRepairable(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeUsesArcaneIngotRepairAndMirrorIsNotRepairable(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeBridgeUsesHigherComparableMainhandAttribute(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeUsesHigherComparableMainhandAttribute(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeBridgeSkipsMultiplyTotalWhenOffhandAlreadyHasAttribute(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeSkipsMultiplyTotalWhenOffhandAlreadyHasAttribute(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeBridgeIgnoresVanillaAttackAttributes(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeIgnoresVanillaAttackAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeBridgeIncludesStackAttributeModifiers(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeIncludesStackAttributeModifiers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeBridgeDoesNotDoubleApplyUpgradeOrbModifiers(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeDoesNotDoubleApplyUpgradeOrbModifiers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeBridgeResyncsChangedStackAttributeAmounts(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeResyncsChangedStackAttributeAmounts(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeBridgeSyncsOnlyWhileHeldInMainhand(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeBridgeSyncsOnlyWhileHeldInMainhand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void edgeDancerRequiresMainhandSpellSideEdge(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerRequiresMainhandSpellSideEdge(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void edgeDancerGeneratesMirrorAndRestoresEmptyOffhand(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerGeneratesMirrorAndRestoresEmptyOffhand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void edgeDancerMirrorCopiesEnchantmentsAndReplacesSpell(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerMirrorCopiesEnchantmentsAndReplacesSpell(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void edgeDancerRestoresOccupiedOffhand(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerRestoresOccupiedOffhand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void edgeDancerDeactivatesWhenSpellSideEdgeLeavesInventory(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerDeactivatesWhenSpellSideEdgeLeavesInventory(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void edgeDancerMainhandDropCancelsAndOnlyDeactivates(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerMainhandDropCancelsAndOnlyDeactivates(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void edgeDancerOffhandSwapCancelsAndOnlyDeactivates(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.edgeDancerOffhandSwapCancelsAndOnlyDeactivates(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeMirrorOffhandDoesNotApplyVanillaAttackModifiers(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeMirrorOffhandDoesNotApplyVanillaAttackModifiers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeMirrorPairBypassesVanillaTargetIframe(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeMirrorPairBypassesVanillaTargetIframe(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeRequiresMirrorForVanillaTargetIframeBypass(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeRequiresMirrorForVanillaTargetIframeBypass(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeVanillaIframeBypassOnlyAppliesToRecordedTarget(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeVanillaIframeBypassOnlyAppliesToRecordedTarget(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeVanillaIframeBypassDisabledWithCombatOverhauls(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeVanillaIframeBypassDisabledWithCombatOverhauls(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void anchorBlinkRequiresGeneratedMirrorAndRestoresOffhandOnCast(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkRequiresGeneratedMirrorAndRestoresOffhandOnCast(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void anchorBlinkReadyAnchorOnlyMatchesOwner(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkReadyAnchorOnlyMatchesOwner(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void anchorBlinkImpactedDaggerDoesNotDamageAgain(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkImpactedDaggerDoesNotDamageAgain(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void anchorBlinkPostTeleportProtectionOnlyBlocksEnemyDamage(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkPostTeleportProtectionOnlyBlocksEnemyDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void anchorBlinkPostTeleportProtectionSurvivesOtherDimensionCleanup(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkPostTeleportProtectionSurvivesOtherDimensionCleanup(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void anchorBlinkImpactBeyondMaximumRangeDiscardsDagger(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkImpactBeyondMaximumRangeDiscardsDagger(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void anchorBlinkMaximumRangeOnlyChecksAtImpact(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.anchorBlinkMaximumRangeOnlyChecksAtImpact(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void fireResistantEquipmentContractsStayInSync(GameTestHelper helper) {
        EquipmentFireResistanceGameTestScenarios.fireResistantEquipmentContractsStayInSync(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void copperSwingcastStaffStartsWithBallLightningLevelOne(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.copperSwingcastStaffStartsWithBallLightningLevelOne(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void goldSpellcasterGunImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.goldSpellcasterGunImbuedSpellStaysRemovableAfterNormalization(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcasterGunRecastImbueRestrictionsMatchTier(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunRecastImbueRestrictionsMatchTier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcasterGunAbilityTooltipUsesInstantLongCastOnly(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunAbilityTooltipUsesInstantLongCastOnly(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void reflectcastShieldAbilityTooltipShowsManaBypass(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.reflectcastShieldAbilityTooltipShowsManaBypass(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellgunServerConfigDefaultsMatchCurrentHardcodedValues(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunServerConfigDefaultsMatchCurrentHardcodedValues(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLGUN_CONFIG_BATCH)
    public static void spellgunZeroImbueCooldownLimitDisablesOnlyCooldownLimit(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunZeroImbueCooldownLimitDisablesOnlyCooldownLimit(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLGUN_CONFIG_BATCH)
    public static void spellgunZeroCastCooldownConfigForcesZeroCooldown(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellgunZeroCastCooldownConfigForcesZeroCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcasterGunRecastCastBypassesAmmoRequirement(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.spellcasterGunRecastCastBypassesAmmoRequirement(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void copperSwingcastStaffReplacementSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.copperSwingcastStaffReplacementSpellStaysRemovableAfterNormalization(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void reflectcastShieldImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.reflectcastShieldImbuedSpellStaysRemovableAfterNormalization(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void goldSpellcasterGunImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.goldSpellcasterGunImbuedSpellStaysRemovableAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void ironSwingcastStaffImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.ironSwingcastStaffImbuedSpellStaysRemovableAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void copperSwingcastStaffPresetEquivalentSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.copperSwingcastStaffPresetEquivalentSpellStaysRemovableAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void reflectcastShieldImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.reflectcastShieldImbuedSpellStaysRemovableAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void photonSiphonStartsWithLockedManaChargeAndIsNotUnique(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.photonSiphonStartsWithLockedManaChargeAndIsNotUnique(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void photonSiphonCalibrationRepairUnlocksLegacyReplacementOnly(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.photonSiphonCalibrationRepairUnlocksLegacyReplacementOnly(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCalibrationBenchTargetsExposeExpectedSlots(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.spellCalibrationBenchTargetsExposeExpectedSlots(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCalibrationBenchImbueOnlySupportsExtractableTargets(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.spellCalibrationBenchImbueOnlySupportsExtractableTargets(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void reflectcastShieldDurabilityRulesMatchGuardTuning(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.reflectcastShieldDurabilityRulesMatchGuardTuning(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletStartsWithSingleHiddenSpellSlotAndLoadedAllowlist(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletStartsWithSingleHiddenSpellSlotAndLoadedAllowlist(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmUsesCharmSlotAndAppearsInCreativeTab(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmUsesCharmSlotAndAppearsInCreativeTab(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmExclusiveEnchantmentsStayMutuallyExclusive(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmExclusiveEnchantmentsStayMutuallyExclusive(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmFullyNegatesDamageAndPreservesArmorDurability(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmFullyNegatesDamageAndPreservesArmorDurability(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmBurnedOutFullNegateCancelsHitAndStartsCooldown(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmBurnedOutFullNegateCancelsHitAndStartsCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmLowManaBurnedOutFullNegateStillCancelsHit(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmLowManaBurnedOutFullNegateStillCancelsHit(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmDoesNotRespendManaDuringVanillaStyleIFrame(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmDoesNotRespendManaDuringVanillaStyleIFrame(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmPartialReductionEntersCooldownAndKeepsArmorMitigation(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmPartialReductionEntersCooldownAndKeepsArmorMitigation(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmCooldownRecoversAtOneHundredMana(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmCooldownRecoversAtOneHundredMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmShellUsesArmorOnlyOnNormalDamageAndWearsArmor(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmShellUsesArmorOnlyOnNormalDamageAndWearsArmor(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmShellLowManaBurnoutStillUsesArmorPath(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmShellLowManaBurnoutStillUsesArmorPath(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmSynchronizationChargesEnchantReductionBeforeNormalBarrier(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmSynchronizationChargesEnchantReductionBeforeNormalBarrier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmSynchronizationBurnoutStopsAfterEnchantReduction(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmSynchronizationBurnoutStopsAfterEnchantReduction(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmSynchronizationLowManaBurnoutStopsAfterEnchantStage(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmSynchronizationLowManaBurnoutStopsAfterEnchantStage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmSynchronizationLowManaBurnoutAfterBarrierStage(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmSynchronizationLowManaBurnoutAfterBarrierStage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmNeutralizationAbsorbsBypassArmorDamageDuringCooldown(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmNeutralizationAbsorbsBypassArmorDamageDuringCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_FREE_COST_CONFIG_BATCH)
    public static void manaShieldCharmFreeManaCostConfigAbsorbsWithoutDepletionCooldown(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmFreeManaCostConfigAbsorbsWithoutDepletionCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_RECOVERY_CONFIG_BATCH)
    public static void manaShieldCharmZeroRecoveryThresholdDisablesDepletionCooldown(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmZeroRecoveryThresholdDisablesDepletionCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_SYNCHRONIZATION_CONFIG_BATCH)
    public static void manaShieldCharmSynchronizationManaCostUsesServerConfig(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmSynchronizationManaCostUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_NEUTRALIZATION_CONFIG_BATCH)
    public static void manaShieldCharmNeutralizationZeroRecoveryStillNullifies(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmNeutralizationZeroRecoveryStillNullifies(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_SHELL_CONFIG_BATCH)
    public static void manaShieldCharmShellArmorDurabilityDamageUsesServerConfig(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmShellArmorDurabilityDamageUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_IFRAME_CONFIG_BATCH)
    public static void manaShieldCharmInvulnerableTimeUsesServerConfig(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmInvulnerableTimeUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaThrusterUsesFeetSlotAndDedicatedImplementation(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterUsesFeetSlotAndDedicatedImplementation(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_THRUSTER_CONFIG_BATCH)
    public static void manaThrusterAppliesFixedThrustAndUsesServerConfigManaCost(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterAppliesFixedThrustAndUsesServerConfigManaCost(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_THRUSTER_CONFIG_BATCH)
    public static void manaThrusterInsufficientManaDoesNotAccelerate(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterInsufficientManaDoesNotAccelerate(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_THRUSTER_CONFIG_BATCH)
    public static void manaThrusterSuppressesManaRecoveryUntilLanding(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterSuppressesManaRecoveryUntilLanding(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_THRUSTER_CONFIG_BATCH)
    public static void manaThrusterGroundHeldJumpDoesNotStartAfterTakeoff(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterGroundHeldJumpDoesNotStartAfterTakeoff(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_THRUSTER_CONFIG_BATCH)
    public static void manaThrusterCreativeMayflyAndFlyingDisableThrust(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterCreativeMayflyAndFlyingDisableThrust(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_THRUSTER_CONFIG_BATCH)
    public static void manaThrusterCreativeFlyingClearsManaRecoverySuppression(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterCreativeFlyingClearsManaRecoverySuppression(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_THRUSTER_CONFIG_BATCH)
    public static void manaThrusterSwimmingAcceleratesForwardWithoutSuppressingRecovery(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterSwimmingAcceleratesForwardWithoutSuppressingRecovery(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_THRUSTER_CONFIG_BATCH)
    public static void manaThrusterWaterToAirResumesManaRecoverySuppression(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterWaterToAirResumesManaRecoverySuppression(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_THRUSTER_CONFIG_BATCH)
    public static void manaThrusterElytraFlightAcceleratesForwardAndSuppressesRecovery(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterElytraFlightAcceleratesForwardAndSuppressesRecovery(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_THRUSTER_CONFIG_BATCH)
    public static void manaThrusterVanillaJumpMovementContextsDoNotActivate(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterVanillaJumpMovementContextsDoNotActivate(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_THRUSTER_CONFIG_BATCH)
    public static void manaThrusterLavaAllowsUpwardEscapeWithoutSuppressingRecovery(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterLavaAllowsUpwardEscapeWithoutSuppressingRecovery(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void magiCompressorGadgetUsesBeltSlotAndDedicatedImplementation(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetUsesBeltSlotAndDedicatedImplementation(helper);
    }

    @GameTest(template = TEMPLATE, batch = MAGI_COMPRESSOR_GADGET_CONFIG_BATCH)
    public static void magiCompressorGadgetCreateUnavailableDoesNotSpendManaOrStoreAir(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetCreateUnavailableDoesNotSpendManaOrStoreAir(helper);
    }

    @GameTest(template = TEMPLATE, batch = MAGI_COMPRESSOR_GADGET_CONFIG_BATCH)
    public static void magiCompressorGadgetConvertsConfiguredManaIntoAir(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetConvertsConfiguredManaIntoAir(helper);
    }

    @GameTest(template = TEMPLATE, batch = MAGI_COMPRESSOR_GADGET_CONFIG_BATCH)
    public static void magiCompressorGadgetConvertsProportionallyUpToMaxAir(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetConvertsProportionallyUpToMaxAir(helper);
    }

    @GameTest(template = TEMPLATE, batch = MAGI_COMPRESSOR_GADGET_CONFIG_BATCH)
    public static void magiCompressorGadgetInsufficientManaDoesNotStoreAir(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetInsufficientManaDoesNotStoreAir(helper);
    }

    @GameTest(template = TEMPLATE, batch = MAGI_COMPRESSOR_GADGET_CONFIG_BATCH)
    public static void magiCompressorGadgetBacktankSupplierClampsLegacyAirTag(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetBacktankSupplierClampsLegacyAirTag(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void magiCompressorGadgetRejectsEnchantments(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetRejectsEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void jumpcastCharmUsesFeetSlotAndSupportsCalibrationImbue(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmUsesFeetSlotAndSupportsCalibrationImbue(helper);
    }

    @GameTest(template = TEMPLATE, batch = JUMPCAST_CHARM_CONFIG_BATCH)
    public static void jumpcastCharmInstantSpellCastsInAirAndConsumesMana(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmInstantSpellCastsInAirAndConsumesMana(helper);
    }

    @GameTest(template = TEMPLATE, batch = JUMPCAST_CHARM_CONFIG_BATCH)
    public static void jumpcastCharmLongSpellFailsWithoutSpendingOrCooldown(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmLongSpellFailsWithoutSpendingOrCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = JUMPCAST_CHARM_CONFIG_BATCH)
    public static void jumpcastCharmInsufficientManaFailsWithoutSpending(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmInsufficientManaFailsWithoutSpending(helper);
    }

    @GameTest(template = TEMPLATE, batch = JUMPCAST_CHARM_CONFIG_BATCH)
    public static void jumpcastCharmPreservesExistingCast(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmPreservesExistingCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = JUMPCAST_CHARM_CONFIG_BATCH)
    public static void jumpcastCharmBlockedMovementContextsDoNotCast(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmBlockedMovementContextsDoNotCast(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNormalizationDropsBlockedSpellsAndClampsSlots(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNormalizationDropsBlockedSpellsAndClampsSlots(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletWisdomShardIsAdjustmentOnlyProfileGate(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletWisdomShardIsAdjustmentOnlyProfileGate(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletWisdomShardBlocksUnprofiledAutoCast(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletWisdomShardBlocksUnprofiledAutoCast(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletWisdomShardProfileConditionsUseAndSemantics(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletWisdomShardProfileConditionsUseAndSemantics(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletSpellSlotUpgradeEnablesFourSlotsAndKeepsOrder(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletSpellSlotUpgradeEnablesFourSlotsAndKeepsOrder(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletKeepsDisabledSlotSpellsAfterRemovingUpgrade(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletKeepsDisabledSlotSpellsAfterRemovingUpgrade(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletWorkbenchExtractionUsesLastSpellAndKeepsSlotCount(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletWorkbenchExtractionUsesLastSpellAndKeepsSlotCount(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletAutoCastStartsOnFirstIntervalAfterEquip(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletAutoCastStartsOnFirstIntervalAfterEquip(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletInsufficientManaDelaysRetryAndSkipsErroredSlotOnce(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletInsufficientManaDelaysRetryAndSkipsErroredSlotOnce(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletCreativeCastIgnoresManaCost(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletCreativeCastIgnoresManaCost(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletCooldownIgnoresSwordMultiplier(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletCooldownIgnoresSwordMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletLongSpellCompletesImmediately(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletLongSpellCompletesImmediately(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNotificationControllerSchedulesCastAndThresholds(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNotificationControllerSchedulesCastAndThresholds(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNotificationControllerSkipsUnreachedThresholds(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNotificationControllerSkipsUnreachedThresholds(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNotificationControllerSkipsCooldownsUnderFiveSeconds(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNotificationControllerSkipsCooldownsUnderFiveSeconds(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNotificationControllerQueuesInOrderAndKeepsDelayedLabel(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNotificationControllerQueuesInOrderAndKeepsDelayedLabel(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNotificationControllerUpdatesLinearBuildRemaining(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNotificationControllerUpdatesLinearBuildRemaining(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void ironSpellcasterGunExtractedSpellStaysClearedAfterSaveLoad(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.ironSpellcasterGunExtractedSpellStaysClearedAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void ironSwingcastStaffExtractedSpellStaysClearedAfterSaveLoad(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.ironSwingcastStaffExtractedSpellStaysClearedAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void goldSpellcasterGunLegacyLockedReplacementIsRecoveredAfterSaveLoad(GameTestHelper helper) {
        EquipmentSpellGunGameTestScenarios.goldSpellcasterGunLegacyLockedReplacementIsRecoveredAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void ironSwingcastStaffLegacyLockedReplacementIsRecoveredAfterSaveLoad(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.ironSwingcastStaffLegacyLockedReplacementIsRecoveredAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void diamondAndNetheriteSpellAmplifierExposeNewAttributeBonuses(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.diamondAndNetheriteSpellAmplifierExposeNewAttributeBonuses(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void upgradeWhitelistCoversTargetAbstractItems(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.upgradeWhitelistCoversTargetAbstractItems(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellGunsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellGunsKeepExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void offhandMagicItemsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.offhandMagicItemsKeepExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void swingMagicWeaponsUseBaseAttackModifierIds(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.swingMagicWeaponsUseBaseAttackModifierIds(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffUpgradeMergesMainhandMeleeDamage(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffUpgradeMergesMainhandMeleeDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaForceBladeAttunementAndUpgradeMergeForTooltip(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeAttunementAndUpgradeMergeForTooltip(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaForceBladeAttackManaCostIsOncePerTick(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeAttackManaCostIsOncePerTick(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaForceBladeConfigScalesDamageAndManaFormulas(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeConfigScalesDamageAndManaFormulas(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaForceBladeReleaseCooldownUsesServerConfig(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeReleaseCooldownUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaForceBladePerfectGuardReleaseCooldownGraceIsSingleUse(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladePerfectGuardReleaseCooldownGraceIsSingleUse(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatSpellbreakerIsTwoHandedAndAmplifierHasOffhandSpellPower(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatSpellbreakerIsTwoHandedAndAmplifierHasOffhandSpellPower(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatOffhandOnlyGauntletDoesNotForceDualWielding(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatOffhandOnlyGauntletDoesNotForceDualWielding(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeBetterCombatTooltipFollowsLoadedMod(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.spellSideEdgeBetterCombatTooltipFollowsLoadedMod(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatSpellSideEdgeSuppressesNonMirrorOffhand(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatSpellSideEdgeSuppressesNonMirrorOffhand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatOffhandRescueIncludesEnchantAndImbueDerivedModifiers(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatOffhandRescueIncludesEnchantAndImbueDerivedModifiers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatRescueUsesPhysicalOffhandInventoryStack(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatSpellSelectionRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatSpellSelectionRescueUsesPhysicalOffhandInventoryStack(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatScrollcasterGauntletRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatScrollcasterGauntletRescueUsesPhysicalOffhandInventoryStack(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void dynamicCastingMobilityEffectRebalancesAgainstExternalCastingMoveSpeed(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.dynamicCastingMobilityEffectRebalancesAgainstExternalCastingMoveSpeed(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantedCircletKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.enchantedCircletKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantedCircletCurioBonusesMirrorOffhandMagicEnchantments(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.enchantedCircletCurioBonusesMirrorOffhandMagicEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantedCircletWorkbenchExtractionTagDoesNotAffectAshenCirclet(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.enchantedCircletWorkbenchExtractionTagDoesNotAffectAshenCirclet(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantedCircletWisdomMatchesArmorRate(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.enchantedCircletWisdomMatchesArmorRate(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void wisdomAppliesToBlockBreakExperienceAndRoundsUp(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.wisdomAppliesToBlockBreakExperienceAndRoundsUp(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void rightClickMagicWeaponsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        RightClickMagicWeaponGameTestScenarios.rightClickMagicWeaponsKeepExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void rightClickMagicWeaponTooltipsStartWithOffhandPriorityHint(GameTestHelper helper) {
        RightClickMagicWeaponGameTestScenarios.rightClickMagicWeaponTooltipsStartWithOffhandPriorityHint(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void rightClickMagicWeaponPrioritizesSupportedOffhandUseItems(GameTestHelper helper) {
        RightClickMagicWeaponGameTestScenarios.rightClickMagicWeaponPrioritizesSupportedOffhandUseItems(helper);
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
    public static void focusStaffbowUpdatesArtisanSmashSplashRadiusOnChargedRelease(GameTestHelper helper) {
        FocusStaffbowGameTestScenarios.focusStaffbowUpdatesArtisanSmashSplashRadiusOnChargedRelease(helper);
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
    public static void spellCastParryingRingBlocksFrontLongCastWithinWindow(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingBlocksFrontLongCastWithinWindow(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingWithoutRingDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingWithoutRingDoesNotBlock(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELL_CAST_PARRYING_RING_CONFIG_BATCH)
    public static void spellCastParryingRingAfterWindowDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingAfterWindowDoesNotBlock(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingBackAttackDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingBackAttackDoesNotBlock(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingNormalInstantAndContinuousDoNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingNormalInstantAndContinuousDoNotBlock(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingStoppedLongCastDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingStoppedLongCastDoesNotBlock(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingFocusStaffbowInstantPendingBlocks(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingFocusStaffbowInstantPendingBlocks(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingFocusStaffbowContinuousPendingDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingFocusStaffbowContinuousPendingDoesNotBlock(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingBypassShieldDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingBypassShieldDoesNotBlock(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingDiscardsBlockedProjectile(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingDiscardsBlockedProjectile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaForceBladeKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ManaForceBladeGameTestScenarios.manaForceBladeKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffExposesExpectedMainhandAttributes(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffExposesExpectedMainhandAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffResolveThrownDamageIncludesApplicableEnchantments(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffResolveThrownDamageIncludesApplicableEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffThrowConsumesMana(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffThrowConsumesMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffLoyaltyReducesThrowManaCost(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffLoyaltyReducesThrowManaCost(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffRiptideWorksOnDryGroundWithoutProjectile(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffRiptideWorksOnDryGroundWithoutProjectile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactForwardUsesHistoryAndFallback(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactForwardUsesHistoryAndFallback(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerCastsInstantAndLongSpells(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerCastsInstantAndLongSpells(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerCastsInstantWhileOwnerBusy(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerCastsInstantWhileOwnerBusy(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffBusyFallbackDoesNotBypassCooldown(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffBusyFallbackDoesNotBypassCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellThrowableCardImpactCastManagerCastsInstantWhileOwnerBusy(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.spellThrowableCardImpactCastManagerCastsInstantWhileOwnerBusy(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffRemoteOwnerDenylistBlocksRuntimeWithoutFallback(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffRemoteOwnerDenylistBlocksRuntimeWithoutFallback(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerCastsPlayerSelfProfile(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerCastsPlayerSelfProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffCreativeImpactCastUsesRemoteOwnerProfileWithZeroMana(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffCreativeImpactCastUsesRemoteOwnerProfileWithZeroMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffCreativeImpactCastUsesStaffProfileWithZeroMana(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffCreativeImpactCastUsesStaffProfileWithZeroMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerCastsInitialRaiseDeadProfile(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerCastsInitialRaiseDeadProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerBlocksRaiseDeadWhenRecastExists(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerBlocksRaiseDeadWhenRecastExists(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffRaiseDeadPreservesWheelSelectionAfterRecast(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffRaiseDeadPreservesWheelSelectionAfterRecast(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerRejectsUnprofiledSpell(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerRejectsUnprofiledSpell(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerStartsContinuousSpells(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerStartsContinuousSpells(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffContinuousRemoteOwnerIgnoresMissingDispenserProfile(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffContinuousRemoteOwnerIgnoresMissingDispenserProfile(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void chargedTwinBladeStaffContinuousThrowableCardUsesWeaponImbueCooldown(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffContinuousThrowableCardUsesWeaponImbueCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerSkipsWhenOwnerCannotCast(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerSkipsWhenOwnerCannotCast(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void meditationPotionsExposeExpectedEffectsAndDurations(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.meditationPotionsExposeExpectedEffectsAndDurations(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void swingcastStaffTiersExposeRequestedImbueRules(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.swingcastStaffTiersExposeRequestedImbueRules(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffStartsWithHiddenManaSlash(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffStartsWithHiddenManaSlash(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffMissSwingCastsManaSlash(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffMissSwingCastsManaSlash(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffHitSwingDoesNotCastManaSlash(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffHitSwingDoesNotCastManaSlash(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffVanillaAttackEntityHitDoesNotCastManaSlash(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffVanillaAttackEntityHitDoesNotCastManaSlash(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffDelayedHitDoesNotCastManaSlash(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffDelayedHitDoesNotCastManaSlash(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffMissTriggerDoesNotUseSwappedStack(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffMissTriggerDoesNotUseSwappedStack(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffPendingMissTriggerKeepsEarlierHand(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffPendingMissTriggerKeepsEarlierHand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffMainHandHitDoesNotSuppressOffhandMiss(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffMainHandHitDoesNotSuppressOffhandMiss(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffLegacyWheelPresetIsHiddenWhenHeld(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffLegacyWheelPresetIsHiddenWhenHeld(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffLegacyWheelPresetIsHiddenWhenHeldInOffhand(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffLegacyWheelPresetIsHiddenWhenHeldInOffhand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffLegacyWheelReplacementStaysRemovableWhenHeld(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffLegacyWheelReplacementStaysRemovableWhenHeld(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaSlashOffhandSwingUsesOffhandCatalystAttackDamage(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.manaSlashOffhandSwingUsesOffhandCatalystAttackDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaSlashCatalystDamageUsesStackAttributeModifiers(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.manaSlashCatalystDamageUsesStackAttributeModifiers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaSlashCatalystDamageAppliesAttributeEventOnce(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.manaSlashCatalystDamageAppliesAttributeEventOnce(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaSlashDamageMultiplierAppliesAfterMinimumDamage(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.manaSlashDamageMultiplierAppliesAfterMinimumDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaSlashAllowsNonSwingcastPrecondition(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.manaSlashAllowsNonSwingcastPrecondition(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaSlashRequiresSwingcastCatalystWhenContextIsActive(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.manaSlashRequiresSwingcastCatalystWhenContextIsActive(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightAppliesToExternalSpellManaAndCooldown(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.craftsmansDelightAppliesToExternalSpellManaAndCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightAppliesToHarvestMoonAndEarthForgeManaAndCooldown(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.craftsmansDelightAppliesToHarvestMoonAndEarthForgeManaAndCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightScrollcasterGauntletCooldownUsesSwordMultiplier(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.craftsmansDelightScrollcasterGauntletCooldownUsesSwordMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightGracedRainAcceleratesBabyGrowth(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.craftsmansDelightGracedRainAcceleratesBabyGrowth(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightGracedRainReducesBreedingCooldown(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.craftsmansDelightGracedRainReducesBreedingCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void gracedRainWithoutCraftsmansDelightLeavesMobAgeUnchanged(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.gracedRainWithoutCraftsmansDelightLeavesMobAgeUnchanged(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void gracedRainUndeadTargetsKeepDamageBehavior(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.gracedRainUndeadTargetsKeepDamageBehavior(helper);
    }

    @GameTest(template = TEMPLATE, batch = CRAFTSMANS_DELIGHT_GRACED_RAIN_DENYLIST_CONFIG_BATCH)
    public static void craftsmansDelightGracedRainGrowthDenylistBlocksOnlyGrowth(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.craftsmansDelightGracedRainGrowthDenylistBlocksOnlyGrowth(helper);
    }

    @GameTest(template = TEMPLATE, batch = CRAFTSMANS_DELIGHT_GRACED_RAIN_DENYLIST_CONFIG_BATCH)
    public static void craftsmansDelightGracedRainBreedingCooldownDenylistBlocksOnlyCooldown(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.craftsmansDelightGracedRainBreedingCooldownDenylistBlocksOnlyCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightGracedRainDoesNotReduceAllayDuplicationCooldown(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.craftsmansDelightGracedRainDoesNotReduceAllayDuplicationCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void magiAgentSuitBootsCooldownReducesTargetSpell(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.magiAgentSuitBootsCooldownReducesTargetSpell(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void magiAgentSuitBootsCooldownKeepsCraftsmansDelightBestValue(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.magiAgentSuitBootsCooldownKeepsCraftsmansDelightBestValue(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void magiAgentSuitBootsCooldownPreservesExistingAdditiveCooldown(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.magiAgentSuitBootsCooldownPreservesExistingAdditiveCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void strongestLimitedBaseCooldownSelectionIgnoresStacking(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.strongestLimitedBaseCooldownSelectionIgnoresStacking(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void craftsmansDelightExtendsTouchDigRange(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.craftsmansDelightExtendsTouchDigRange(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void touchDigMergesRingMiningEnchantments(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.touchDigMergesRingMiningEnchantments(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void touchDigUsesRingMiningEnchantmentsWhenCastBareHanded(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.touchDigUsesRingMiningEnchantmentsWhenCastBareHanded(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void spectralHammerUsesCraftsmansDelightRingMiningEnchantments(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.spectralHammerUsesCraftsmansDelightRingMiningEnchantments(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void heavenlyFistWithCraftsmansDelightHarvestsSilkTouchedBuddingCrystal(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.heavenlyFistWithCraftsmansDelightHarvestsSilkTouchedBuddingCrystal(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void heavenlyFistWithoutCraftsmansDelightLeavesBuddingCrystal(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.heavenlyFistWithoutCraftsmansDelightLeavesBuddingCrystal(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void heavenlyFistSkipsCrystalNotGrowingFromHarvestSource(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.heavenlyFistSkipsCrystalNotGrowingFromHarvestSource(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void heavenlyFistSkipsImmatureAmethystBuds(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.heavenlyFistSkipsImmatureAmethystBuds(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void tinyLumberjackWithCraftsmansDelightMovesJobDropsToOrigin(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.tinyLumberjackWithCraftsmansDelightMovesJobDropsToOrigin(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void tinyLumberjackDropMoveFollowsCurrentCraftsmansDelightEquipment(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.tinyLumberjackDropMoveFollowsCurrentCraftsmansDelightEquipment(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void worldFlatterPenetratedArmorEffectAndDamageTags(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.worldFlatterPenetratedArmorEffectAndDamageTags(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void worldFlatterBlockTargetFilterMatchesPickaxeOrShovel(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.worldFlatterBlockTargetFilterMatchesPickaxeOrShovel(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void worldFlatterEntityAttackRequiresArrivalAndHitsSingleTarget(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.worldFlatterEntityAttackRequiresArrivalAndHitsSingleTarget(helper);
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
        SpellCalibrationEquipmentGameTestScenarios.mithrilFreecastStaffBlocksArcaneAnvilImbueViaSpellValidator(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void mithrilFreecastStaffCooldownUsesSelectedSource(GameTestHelper helper) {
        SpellCalibrationEquipmentGameTestScenarios.mithrilFreecastStaffCooldownUsesSelectedSource(helper);
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
        EquipmentEnchantmentSurfaceGameTestScenarios.reflectcastShieldKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordKeepsExpectedStatsTagsAndEnchantments(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellchargedGreatswordKeepsExpectedStatsTagsAndEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordChargeMathDecayAndAttributes(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellchargedGreatswordChargeMathDecayAndAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordOverchargeModeConsumesChargeAndUsesConfiguredAttributes(
            GameTestHelper helper
    ) {
        EquipmentEnchantmentSurfaceGameTestScenarios
                .spellchargedGreatswordOverchargeModeConsumesChargeAndUsesConfiguredAttributes(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCHARGED_GREATSWORD_CONFIG_BATCH)
    public static void spellchargedGreatswordServerConfigOverridesCombatStats(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellchargedGreatswordServerConfigOverridesCombatStats(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordOverchargeActivationPausesDecayUntilRelease(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellchargedGreatswordOverchargeActivationPausesDecayUntilRelease(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordSweepingEdgeBonusAndSweepHitbox(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellchargedGreatswordSweepingEdgeBonusAndSweepHitbox(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordBetterCombatUsesChargedWeaponAttributes(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellchargedGreatswordBetterCombatUsesChargedWeaponAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordEpicFightUsesSweepingEdgeAndOverchargeRefillsInnate(
            GameTestHelper helper
    ) {
        EquipmentEnchantmentSurfaceGameTestScenarios
                .spellchargedGreatswordEpicFightUsesSweepingEdgeAndOverchargeRefillsInnate(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordChargeEventRequiresMainhand(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellchargedGreatswordChargeEventRequiresMainhand(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void spellchargedGreatswordContinuousRecastRefreshesDecayWithoutExtraCharge(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios
                .spellchargedGreatswordContinuousRecastRefreshesDecayWithoutExtraCharge(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void spellchargedGreatswordRecastRefreshesDecayWithoutExtraCharge(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios
                .spellchargedGreatswordRecastRefreshesDecayWithoutExtraCharge(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.spellcastersFlaskKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.alchemistsFlaskKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void apprenticeEnchantmentsKeepExpectedAcquisitionFlags(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.apprenticeEnchantmentsKeepExpectedAcquisitionFlags(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void randomApplicableBookEnchantmentsExcludeFlaskEnchantments(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.randomApplicableBookEnchantmentsExcludeFlaskEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void magicArmorKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.magicArmorKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void scrollcasterGauntletKeepsExpectedStatsAndBenchEnchantingRules(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.scrollcasterGauntletKeepsExpectedStatsAndBenchEnchantingRules(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void scrollcasterGauntletOffhandUseCastsSelectedScrollWhenMainHandDoesNotConsumeUse(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.scrollcasterGauntletOffhandUseCastsSelectedScrollWhenMainHandDoesNotConsumeUse(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void scrollcasterGauntletMainhandPrioritizesSupportedOffhandUseItems(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.scrollcasterGauntletMainhandPrioritizesSupportedOffhandUseItems(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void apprenticeMageRobeKeepsExpectedAttributeBonuses(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.apprenticeMageRobeKeepsExpectedAttributeBonuses(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chromaticMagiaDressKeepsExpectedStatsAndImbueSurface(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.chromaticMagiaDressKeepsExpectedStatsAndImbueSurface(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementMaidenRobeKeepsExpectedStatsImbueAndMagicEnchantments(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.elementMaidenRobeKeepsExpectedStatsImbueAndMagicEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void magiAgentSuitKeepsExpectedStatsImbueAndCalibrationRune(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.magiAgentSuitKeepsExpectedStatsImbueAndCalibrationRune(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementMaidenRobeSchoolSpellPowerDistributesSpellbookSchools(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.elementMaidenRobeSchoolSpellPowerDistributesSpellbookSchools(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementMaidenRobeSchoolSpellPowerSplitsEmptySlotsBetweenTiedSchools(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.elementMaidenRobeSchoolSpellPowerSplitsEmptySlotsBetweenTiedSchools(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementMaidenRobeSchoolSpellPowerIgnoresHandsAndZeroConfig(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.elementMaidenRobeSchoolSpellPowerIgnoresHandsAndZeroConfig(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementMaidenRobeSchoolSpellPowerRefreshesArchivistsAndEnderGrimoireSources(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.elementMaidenRobeSchoolSpellPowerRefreshesArchivistsAndEnderGrimoireSources(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void stealthRuneArmorKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.stealthRuneArmorKeepsExpectedAttributeBonusesAndImbueSurface(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chromaticMagiaDressRecordsCastHistoryByArmorTypeAndIgnoresRecasts(GameTestHelper helper) {
        EquipmentEnchantmentSurfaceGameTestScenarios.chromaticMagiaDressRecordsCastHistoryByArmorTypeAndIgnoresRecasts(helper);
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
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffKeepsExpectedStatsAndEnchantingRules(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleKeepsExpectedStatsAndEnchantingRules(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleKeepsExpectedStatsAndEnchantingRules(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleTooltipShowsControlsBeforeShiftHint(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleTooltipShowsControlsBeforeShiftHint(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleSpecialCooldownPolicyMatchesDefaults(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleSpecialCooldownPolicyMatchesDefaults(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleUsesDedicatedAmmoAndCasingReturnPolicy(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleUsesDedicatedAmmoAndCasingReturnPolicy(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleRecastSkipsAmmoConsumption(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleRecastSkipsAmmoConsumption(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleKeepsNormalManaCost(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleKeepsNormalManaCost(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleInstantCastConsumesAmmoAndAppliesCooldownPolicy(GameTestHelper helper) {
        MultipurposeStaffrifleGameTestScenarios.multipurposeStaffrifleInstantCastConsumesAmmoAndAppliesCooldownPolicy(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void circuitHeatStaffAdditionalManaScalesWithSkippedCooldown(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffAdditionalManaScalesWithSkippedCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_ADDITIONAL_MANA_CONFIG_BATCH)
    public static void circuitHeatStaffAdditionalManaUsesServerConfig(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffAdditionalManaUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void circuitHeatStaffOverheatUsesCastCooldownPlusSkippedCooldown(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffOverheatUsesCastCooldownPlusSkippedCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_OVERHEAT_MIN_CONFIG_BATCH)
    public static void circuitHeatStaffOverheatDurationUsesServerMinTicks(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffOverheatDurationUsesServerMinTicks(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_OVERHEAT_CAP_CONFIG_BATCH)
    public static void circuitHeatStaffOverheatDurationUsesServerCapTicks(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffOverheatDurationUsesServerCapTicks(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void circuitHeatStaffBypassKeepsBaseManaGate(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffBypassKeepsBaseManaGate(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_COOLDOWN_LIMIT_CONFIG_BATCH)
    public static void circuitHeatStaffCooldownLimitBlocksBypass(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffCooldownLimitBlocksBypass(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_SPELL_DENYLIST_CONFIG_BATCH)
    public static void circuitHeatStaffSpellDenylistBlocksBypass(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffSpellDenylistBlocksBypass(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void circuitHeatStaffRightClickBypassesCooldownButShortcutDoesNot(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.circuitHeatStaffRightClickBypassesCooldownButShortcutDoesNot(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void circuitHeatStaffContinuousBypassKeepsOverheatManaCost(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffContinuousBypassKeepsOverheatManaCost(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void circuitHeatStaffRecastDoesNotTouchBypassOverheatState(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffRecastDoesNotTouchBypassOverheatState(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingConsumesWaterSource(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingConsumesWaterSource(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_COOLING_DISABLED_CONFIG_BATCH, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingDisabledByServerConfig(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingDisabledByServerConfig(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingIgnoresFlowingWater(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingIgnoresFlowingWater(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_WATER_CONSUMPTION_CONFIG_BATCH, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingKeepsWaterSourceWhenConsumptionDisabled(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingKeepsWaterSourceWhenConsumptionDisabled(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingConsumesCauldronLevel(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingConsumesCauldronLevel(helper);
    }

    @GameTest(template = TEMPLATE, batch = CIRCUIT_HEAT_STAFF_CAULDRON_CONSUMPTION_CONFIG_BATCH, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingKeepsWaterCauldronWhenConsumptionDisabled(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingKeepsWaterCauldronWhenConsumptionDisabled(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingKeepsPowderSnowBlock(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingKeepsPowderSnowBlock(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingKeepsPowderSnowCauldronLevel(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingKeepsPowderSnowCauldronLevel(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingIgnoresNonOverheatedStaffInPowderSnow(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingIgnoresNonOverheatedStaffInPowderSnow(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingIgnoresNonOverheatedStaff(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingIgnoresNonOverheatedStaff(helper);
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
        OffhandAndBetterCombatGameTestScenarios.uniteLunaStaffStartsWithUniteLunaAndExpectedMainhandBonuses(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void rightClickMagicWeaponsExposeBaseAttributesAsComponents(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.rightClickMagicWeaponsExposeBaseAttributesAsComponents(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void mainhandUpgradeBridgeAppliesStoredUpgradeDataToSpellGunsAndWeapons(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mainhandUpgradeBridgeAppliesStoredUpgradeDataToSpellGunsAndWeapons(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void castingMoveSpeedAdjustmentStopsAtNormalSpeedWithoutNegativeCorrections(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.castingMoveSpeedAdjustmentStopsAtNormalSpeedWithoutNegativeCorrections(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void longStrideMobilityStillAddsBaseMovementSpeedBonus(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.longStrideMobilityStillAddsBaseMovementSpeedBonus(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void comfortBerriesProvideManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.comfortBerriesProvideManaRegenerationAndExpectedFoodValues(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void comfortSandwichProvidesManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.comfortSandwichProvidesManaRegenerationAndExpectedFoodValues(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaSlashCatalystDamageUsesApplicableSlotGroups(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.manaSlashCatalystDamageUsesApplicableSlotGroups(helper);
    }

}
