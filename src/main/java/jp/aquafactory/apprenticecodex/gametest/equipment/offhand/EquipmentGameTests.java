package jp.aquafactory.apprenticecodex.gametest.equipment.offhand;

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
    public static void copperSpellAmplifierStartsWithShock(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.copperSpellAmplifierStartsWithShock(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void reflectcastShieldImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.reflectcastShieldImbuedSpellStaysRemovableAfterNormalization(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void reflectcastShieldImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.reflectcastShieldImbuedSpellStaysRemovableAfterSaveLoad(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void reflectcastShieldDurabilityRulesMatchGuardTuning(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.reflectcastShieldDurabilityRulesMatchGuardTuning(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void soulstainedSteelSpellAmplifierFollowsMalumAvailability(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.soulstainedSteelSpellAmplifierFollowsMalumAvailability(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void upgradeWhitelistCoversTargetAbstractItems(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.upgradeWhitelistCoversTargetAbstractItems(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void offhandMagicItemsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.offhandMagicItemsKeepExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void betterCombatSpellbreakerIsTwoHandedAndAmplifierHasOffhandSpellPower(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatSpellbreakerIsTwoHandedAndAmplifierHasOffhandSpellPower(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void betterCombatOffhandOnlyGauntletDoesNotForceDualWielding(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatOffhandOnlyGauntletDoesNotForceDualWielding(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellSideEdgeBetterCombatTooltipFollowsLoadedMod(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.spellSideEdgeBetterCombatTooltipFollowsLoadedMod(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void betterCombatSpellSideEdgeSuppressesNonMirrorOffhand(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatSpellSideEdgeSuppressesNonMirrorOffhand(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void betterCombatOffhandRescueIncludesEnchantAndImbueDerivedModifiers(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatOffhandRescueIncludesEnchantAndImbueDerivedModifiers(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void betterCombatRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatRescueUsesPhysicalOffhandInventoryStack(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void betterCombatSelectionUiCanChooseLogicalOrPhysicalOffhand(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatSelectionUiCanChooseLogicalOrPhysicalOffhand(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void betterCombatHiddenNonOffhandMagicItemDoesNotApplyTranscendence(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios
                .betterCombatHiddenNonOffhandMagicItemDoesNotApplyTranscendence(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void betterCombatSpellSelectionRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatSpellSelectionRescueUsesPhysicalOffhandInventoryStack(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void betterCombatScrollcasterGauntletRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.betterCombatScrollcasterGauntletRescueUsesPhysicalOffhandInventoryStack(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void enchantedCircletKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.enchantedCircletKeepsExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void enchantedCircletKeepsBaseCurioPenalty(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.enchantedCircletKeepsBaseCurioPenalty(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void enchantedCircletWorkbenchExtractionTagDoesNotAffectAshenCirclet(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.enchantedCircletWorkbenchExtractionTagDoesNotAffectAshenCirclet(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void enchantedCircletWisdomMatchesArmorRate(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.enchantedCircletWisdomMatchesArmorRate(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void wisdomAppliesToBlockBreakExperienceAndRoundsUp(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.wisdomAppliesToBlockBreakExperienceAndRoundsUp(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void uniteLunaStaffStartsWithUniteLunaAndExpectedMainhandBonuses(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.uniteLunaStaffStartsWithUniteLunaAndExpectedMainhandBonuses(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(helper);
    }
}
