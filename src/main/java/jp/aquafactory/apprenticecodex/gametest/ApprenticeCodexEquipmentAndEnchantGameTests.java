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

    @GameTest(template = TEMPLATE)
    public static void elementalBowKeepsCurrentEmptySpecialSelectionOnlyWhileSelected(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowKeepsCurrentEmptySpecialSelectionOnlyWhileSelected(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowRequiresManaBeforeStartingElementalDraw(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowRequiresManaBeforeStartingElementalDraw(helper);
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
    public static void elementalBowSpecialModeConsumesLastArrowAndKeepsSelection(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowSpecialModeConsumesLastArrowAndKeepsSelection(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowMagicModeIgnoresInfinityWithoutAmmo(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowMagicModeIgnoresInfinityWithoutAmmo(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void vanillaBowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.vanillaBowConsumesSpellcasterQuiverArrowsBeforeInventory(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void vanillaBowPrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.vanillaBowPrefersHeldSpecialArrowOverQuiverNormalArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void vanillaBowPrefersNormalArrowOverMoreNumerousQuiverSpecialArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.vanillaBowPrefersNormalArrowOverMoreNumerousQuiverSpecialArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void vanillaBowInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.vanillaBowInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowVanillaModePrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowVanillaModePrefersHeldSpecialArrowOverQuiverNormalArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowVanillaModeInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowVanillaModeInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowNonMagicModesHideDerivedSpellPresentation(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowNonMagicModesHideDerivedSpellPresentation(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowSuppressesElementalArrowCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.elementalBowSuppressesElementalArrowCooldown(helper);
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
    public static void pastelStaffKeepsItsExtraMiningEnchantments(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.pastelStaffKeepsItsExtraMiningEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffKeepsItsDedicatedEnchantingRules(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.crystalBladedStaffKeepsItsDedicatedEnchantingRules(helper);
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

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightAppliesToExternalSpellManaAndCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.craftsmansDelightAppliesToExternalSpellManaAndCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightExtendsTouchDigRange(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.craftsmansDelightExtendsTouchDigRange(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void touchDigMergesRingMiningEnchantments(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.touchDigMergesRingMiningEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void touchDigUsesRingMiningEnchantmentsWhenCastBareHanded(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.touchDigUsesRingMiningEnchantmentsWhenCastBareHanded(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spectralHammerUsesCraftsmansDelightRingMiningEnchantments(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spectralHammerUsesCraftsmansDelightRingMiningEnchantments(helper);
    }
}
