package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexEquipmentAndEnchantGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String MINING_SPELL_ISOLATED_BATCH = "apprenticecodex.mining_spell_isolated";
    private static final String FOCUS_STAFFBOW_CONTINUOUS_BATCH = "apprenticecodex.focus_staffbow_continuous";
    private static final String ELEMENTAL_BOW_OVERHEAT_BATCH = "apprenticecodex.elemental_bow_overheat";
    private static final String SPELLCASTER_QUIVER_ISOLATED_BATCH = "apprenticecodex.spellcaster_quiver_isolated";

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
    public static void uniteLunaStaffStartsWithUniteLunaAndExpectedMainhandBonuses(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.uniteLunaStaffStartsWithUniteLunaAndExpectedMainhandBonuses(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatSpellbreakerIsTwoHandedAndAmplifierHasOffhandSpellPower(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.betterCombatSpellbreakerIsTwoHandedAndAmplifierHasOffhandSpellPower(helper);
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
    public static void castingMoveSpeedAdjustmentStopsAtNormalSpeedWithoutNegativeCorrections(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.castingMoveSpeedAdjustmentStopsAtNormalSpeedWithoutNegativeCorrections(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void longStrideMobilityStillAddsBaseMovementSpeedBonus(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.longStrideMobilityStillAddsBaseMovementSpeedBonus(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void dynamicCastingMobilityEffectRebalancesAgainstExternalCastingMoveSpeed(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.dynamicCastingMobilityEffectRebalancesAgainstExternalCastingMoveSpeed(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void comfortBerriesProvideManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.comfortBerriesProvideManaRegenerationAndExpectedFoodValues(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowRejectsOffhandUse(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowRejectsOffhandUse(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowAllowsMainhandUseWithOffhandSelection(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowAllowsMainhandUseWithOffhandSelection(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowRejectsUseWithoutArrowCatalyst(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowRejectsUseWithoutArrowCatalyst(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowShowsLongSummonWeaponDuringPendingCast(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowShowsLongSummonWeaponDuringPendingCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowCancelsPendingSummonWeaponBeforeRequiredCharge(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowCancelsPendingSummonWeaponBeforeRequiredCharge(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowContinuousCastStaysActivePastSpellDuration(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowContinuousCastStaysActivePastSpellDuration(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowContinuousCastUsesStandardCastTimeWithoutAttributeAdjustment(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowContinuousCastUsesStandardCastTimeWithoutAttributeAdjustment(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowContinuousCastStopsWhenManaRunsOut(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowContinuousCastStopsWhenManaRunsOut(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowInstantImmediateReleaseConsumesBaseMana(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowInstantImmediateReleaseConsumesBaseMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowShortLongReleaseStaysAtBaseMultiplier(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowShortLongReleaseStaysAtBaseMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowStillRejectsCastWhenBaseManaIsInsufficient(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowStillRejectsCastWhenBaseManaIsInsufficient(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowOverchargeLoanConsumesRecoveredMana(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowOverchargeLoanConsumesRecoveredMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowCreativeOverchargeDoesNotConsumeManaOrCreateLoan(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowCreativeOverchargeDoesNotConsumeManaOrCreateLoan(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowBlocksUseWhileLoanRemains(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowBlocksUseWhileLoanRemains(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowRejectsUseWhileSpellCooldownRemains(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowRejectsUseWhileSpellCooldownRemains(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowLoanMessageUsesExpectedTranslationKey(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowLoanMessageUsesExpectedTranslationKey(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowInsufficientArrowMessageUsesExpectedTranslationKey(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowInsufficientArrowMessageUsesExpectedTranslationKey(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowFallsBackToSpecialArrowWhenNormalArrowIsMissing(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowFallsBackToSpecialArrowWhenNormalArrowIsMissing(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowSynthesisAllowsArrowlessCasting(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowSynthesisAllowsArrowlessCasting(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void focusStaffbowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowConsumesSpellcasterQuiverArrowsBeforeInventory(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowAcceptsSynthesisEnchantments(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowAcceptsSynthesisEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void focusStaffbowExposesExpectedMainhandAttributes(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.focusStaffbowExposesExpectedMainhandAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffKeepsExpectedEnchantmentSurfaces(helper);
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
    public static void chargedTwinBladeStaffImpactCastManagerCastsPlayerSelfProfile(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffImpactCastManagerCastsPlayerSelfProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffCreativeImpactCastUsesDispenserProfileWithZeroMana(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffCreativeImpactCastUsesDispenserProfileWithZeroMana(helper);
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
    public static void chargedTwinBladeStaffImpactCastManagerSkipsWhenOwnerCannotCast(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.chargedTwinBladeStaffImpactCastManagerSkipsWhenOwnerCannotCast(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void meditationPotionsExposeExpectedEffectsAndDurations(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.meditationPotionsExposeExpectedEffectsAndDurations(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void swingcastStaffTiersExposeRequestedImbueRules(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.swingcastStaffTiersExposeRequestedImbueRules(helper);
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
    public static void elementalBowHeldWisdomAndPlunderWorkInBothHands(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowHeldWisdomAndPlunderWorkInBothHands(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightAppliesToExternalSpellManaAndCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.craftsmansDelightAppliesToExternalSpellManaAndCooldown(helper);
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

    @GameTest(template = TEMPLATE)
    public static void rightClickMagicWeaponsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.rightClickMagicWeaponsKeepExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowKeepsVanillaBowEnchantmentSurfaces(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowKeepsVanillaBowEnchantmentSurfaces(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowBuildsSelectionViewsFromHeldAmmo(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowBuildsSelectionViewsFromHeldAmmo(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowInventoryOverlayReflectsCurrentSelection(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowInventoryOverlayReflectsCurrentSelection(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH, timeoutTicks = 80)
    public static void elementalBowSelectionViewExposesOverheatOverlayState(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowSelectionViewExposesOverheatOverlayState(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowKeepsCurrentEmptySpecialSelectionOnlyWhileSelected(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowKeepsCurrentEmptySpecialSelectionOnlyWhileSelected(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowRequiresManaBeforeStartingElementalDraw(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowRequiresManaBeforeStartingElementalDraw(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowFallsBackToNoneWhenLegacyModeCannotResolve(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowFallsBackToNoneWhenLegacyModeCannotResolve(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowSynchronizesSpellContainerToCurrentMode(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowSynchronizesSpellContainerToCurrentMode(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowSpellContainerAppliesPowerFlameAndClearsInNoneMode(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowSpellContainerAppliesPowerFlameAndClearsInNoneMode(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowDoesNotAddDerivedSpellToMainhandSpellWheel(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowDoesNotAddDerivedSpellToMainhandSpellWheel(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowBlocksArcaneAnvilImbueViaSpellValidator(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowBlocksArcaneAnvilImbueViaSpellValidator(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowManaErrorUsesIronsSpellbooksTranslationKey(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowManaErrorUsesIronsSpellbooksTranslationKey(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowDoesNotConsumeResourcesBeforeFullDraw(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowDoesNotConsumeResourcesBeforeFullDraw(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowInfinityAllowsVanillaDrawWithoutArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowInfinityAllowsVanillaDrawWithoutArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowVanillaModeConsumesSpecialArrowWhenNormalArrowsAreMissing(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowVanillaModeConsumesSpecialArrowWhenNormalArrowsAreMissing(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowArrowModeRequiresNormalArrowsEvenWhenSpecialArrowsExist(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowArrowModeRequiresNormalArrowsEvenWhenSpecialArrowsExist(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowInfinityAllowsArrowModeDrawWithoutArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowInfinityAllowsArrowModeDrawWithoutArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowSpecialModeInfinityKeepsSelectionAndAllowsEmptyReuse(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowSpecialModeInfinityKeepsSelectionAndAllowsEmptyReuse(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowMagicModeIgnoresInfinityWithoutAmmo(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowMagicModeIgnoresInfinityWithoutAmmo(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void spellcasterQuiverUsesBackSlotAndCapsStoredArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellcasterQuiverUsesBackSlotAndCapsStoredArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void equippedSpellcasterQuiverAutoStoresPickedUpArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.equippedSpellcasterQuiverAutoStoresPickedUpArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void elementalBowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowConsumesSpellcasterQuiverArrowsBeforeInventory(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void elementalBowSelectionViewsIncludeSpellcasterQuiverArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowSelectionViewsIncludeSpellcasterQuiverArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void vanillaBowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.vanillaBowConsumesSpellcasterQuiverArrowsBeforeInventory(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void vanillaBowPrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.vanillaBowPrefersHeldSpecialArrowOverQuiverNormalArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void vanillaBowPrefersNormalArrowOverMoreNumerousQuiverSpecialArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.vanillaBowPrefersNormalArrowOverMoreNumerousQuiverSpecialArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void vanillaBowInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.vanillaBowInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void elementalBowVanillaModePrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowVanillaModePrefersHeldSpecialArrowOverQuiverNormalArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void elementalBowVanillaModeInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowVanillaModeInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void spellcasterQuiverSlowdownHelperTracksEquippedBowUse(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellcasterQuiverSlowdownHelperTracksEquippedBowUse(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void spellcasterQuiverSlowdownHelperTracksFocusStaffbowDrawUse(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellcasterQuiverSlowdownHelperTracksFocusStaffbowDrawUse(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowNonMagicModesHideDerivedSpellPresentation(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowNonMagicModesHideDerivedSpellPresentation(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH)
    public static void elementalBowCooldownHelperIgnoresWeaponMultiplierButKeepsPlayerCooldownReduction(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowCooldownHelperIgnoresWeaponMultiplierButKeepsPlayerCooldownReduction(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH)
    public static void elementalBowSuppressesElementalArrowCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowSuppressesElementalArrowCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH)
    public static void elementalBowConsumesAdditionalManaWhileOverheated(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowConsumesAdditionalManaWhileOverheated(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH)
    public static void elementalBowOverheatTracksSchoolsSeparately(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowOverheatTracksSchoolsSeparately(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH, timeoutTicks = 80)
    public static void elementalBowOverheatRefreshesDurationAfterRepeatedCast(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowOverheatRefreshesDurationAfterRepeatedCast(helper);
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
    public static void enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void pastelStaffKeepsItsLocalEnchantingRules(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.pastelStaffKeepsItsLocalEnchantingRules(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void malumHauntedBonusResolvesFromSupportedMainhandWeapons(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.malumHauntedBonusResolvesFromSupportedMainhandWeapons(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void malumHauntedBonusUsesDedicatedDamageType(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.malumHauntedBonusUsesDedicatedDamageType(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void magicDamageTagActuallyScalesWithLodestoneMagicProficiency(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.magicDamageTagActuallyScalesWithLodestoneMagicProficiency(helper);
    }
}
