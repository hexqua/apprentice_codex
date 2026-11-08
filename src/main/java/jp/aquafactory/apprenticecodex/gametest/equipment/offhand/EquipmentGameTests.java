package jp.aquafactory.apprenticecodex.gametest.equipment.offhand;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void copperSpellAmplifierStartsWithShock(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.copperSpellAmplifierStartsWithShock(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void reflectcastShieldImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.reflectcastShieldImbuedSpellStaysRemovableAfterNormalization(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void reflectcastShieldImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.reflectcastShieldImbuedSpellStaysRemovableAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void reflectcastShieldDurabilityRulesMatchGuardTuning(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.reflectcastShieldDurabilityRulesMatchGuardTuning(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void soulstainedSteelSpellAmplifierFollowsMalumAvailability(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.soulstainedSteelSpellAmplifierFollowsMalumAvailability(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void upgradeWhitelistCoversTargetAbstractItems(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.upgradeWhitelistCoversTargetAbstractItems(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void uniteLunaStaffStartsWithUniteLunaAndExpectedMainhandBonuses(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.uniteLunaStaffStartsWithUniteLunaAndExpectedMainhandBonuses(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(helper);
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
    public static void betterCombatSelectionUiCanChooseLogicalOrPhysicalOffhand(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatSelectionUiCanChooseLogicalOrPhysicalOffhand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatHiddenNonOffhandMagicItemDoesNotApplyTranscendence(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatHiddenNonOffhandMagicItemDoesNotApplyTranscendence(helper);
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
    public static void enchantedCircletKeepsBaseCurioPenalty(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.enchantedCircletKeepsBaseCurioPenalty(helper);
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
}
