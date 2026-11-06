package jp.aquafactory.apprenticecodex.gametest.equipment.casting;

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
    public static void dynamicCastingMobilityEffectRebalancesAgainstExternalCastingMoveSpeed(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.dynamicCastingMobilityEffectRebalancesAgainstExternalCastingMoveSpeed(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void rightClickMagicWeaponsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        RightClickMagicWeaponGameTestScenarios.rightClickMagicWeaponsKeepExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void rightClickMagicWeaponTooltipsStartWithOffhandPriorityHint(GameTestHelper helper) {
        RightClickMagicWeaponGameTestScenarios.rightClickMagicWeaponTooltipsStartWithOffhandPriorityHint(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void rightClickMagicWeaponPrioritizesSupportedOffhandUseItems(GameTestHelper helper) {
        RightClickMagicWeaponGameTestScenarios.rightClickMagicWeaponPrioritizesSupportedOffhandUseItems(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void meditationPotionsExposeExpectedEffectsAndDurations(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.meditationPotionsExposeExpectedEffectsAndDurations(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void craftsmansDelightAppliesToExternalSpellManaAndCooldown(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.craftsmansDelightAppliesToExternalSpellManaAndCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void craftsmansDelightAppliesToHarvestMoonAndEarthForgeManaAndCooldown(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.craftsmansDelightAppliesToHarvestMoonAndEarthForgeManaAndCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void craftsmansDelightScrollcasterGauntletCooldownUsesSwordMultiplier(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.craftsmansDelightScrollcasterGauntletCooldownUsesSwordMultiplier(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magiAgentSuitBootsCooldownReducesTargetSpell(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.magiAgentSuitBootsCooldownReducesTargetSpell(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void equipmentCooldownReductionsDoNotStack(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.equipmentCooldownReductionsDoNotStack(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.equipment_spell_timing_multiplier_config")
    public static void equipmentSpellTimingMultipliersFollowServerConfigAndKeepOneTickMinimum(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios
                .equipmentSpellTimingMultipliersFollowServerConfigAndKeepOneTickMinimum(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magiAgentSuitBootsCooldownPreservesExistingAdditiveCooldown(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.magiAgentSuitBootsCooldownPreservesExistingAdditiveCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void strongestLimitedBaseCooldownSelectionIgnoresStacking(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.strongestLimitedBaseCooldownSelectionIgnoresStacking(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void craftsmansDelightExtendsTouchDigRange(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.craftsmansDelightExtendsTouchDigRange(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated", timeoutTicks = 300)
    public static void craftsmansDelightTouchDigActivatesPrimedSpellweavingPickaxeForDirt(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios
                .craftsmansDelightTouchDigActivatesPrimedSpellweavingPickaxeForDirt(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void touchDigSpellweavingRequiresCraftsmansDelight(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.touchDigSpellweavingRequiresCraftsmansDelight(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void touchDigSpellweavingRequiresPrimedPickaxe(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.touchDigSpellweavingRequiresPrimedPickaxe(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void touchDigSpellweavingExcludesAxe(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.touchDigSpellweavingExcludesAxe(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void touchDigSpellweavingDoesNotDuplicateNativeMatch(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.touchDigSpellweavingDoesNotDuplicateNativeMatch(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void touchDigSpellweavingKeepsTouchDigHarvestLimit(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.touchDigSpellweavingKeepsTouchDigHarvestLimit(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void touchDigIgnoresCraftsmansDelightRingEnchantments(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.touchDigIgnoresCraftsmansDelightRingEnchantments(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void craftsmansDelightDummyToolsCopyAllCurrentMainHandEnchantments(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.craftsmansDelightDummyToolsCopyAllCurrentMainHandEnchantments(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void spectralHammerCopiesCurrentMainHandWithCraftsmansDelight(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.spectralHammerCopiesCurrentMainHandWithCraftsmansDelight(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void heavenlyFistWithCraftsmansDelightHarvestsSilkTouchedBuddingCrystal(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.heavenlyFistWithCraftsmansDelightHarvestsSilkTouchedBuddingCrystal(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void heavenlyFistWithUnresolvedOwnerLeavesBuddingCrystal(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.heavenlyFistWithUnresolvedOwnerLeavesBuddingCrystal(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void heavenlyFistInAdventureModeLeavesBuddingCrystal(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.heavenlyFistInAdventureModeLeavesBuddingCrystal(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void heavenlyFistCanceledBreakEventLeavesBuddingCrystal(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.heavenlyFistCanceledBreakEventLeavesBuddingCrystal(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void heavenlyFistWithoutCraftsmansDelightHarvestsWithoutEnchantments(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.heavenlyFistWithoutCraftsmansDelightHarvestsWithoutEnchantments(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void heavenlyFistSkipsCrystalNotGrowingFromHarvestSource(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.heavenlyFistSkipsCrystalNotGrowingFromHarvestSource(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void heavenlyFistSkipsImmatureAmethystBuds(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.heavenlyFistSkipsImmatureAmethystBuds(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void tinyLumberjackWithCraftsmansDelightMovesJobDropsToOrigin(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.tinyLumberjackWithCraftsmansDelightMovesJobDropsToOrigin(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void tinyLumberjackDropMoveFollowsCurrentCraftsmansDelightEquipment(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.tinyLumberjackDropMoveFollowsCurrentCraftsmansDelightEquipment(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void worldFlatterPenetratedArmorEffectAndDamageTags(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.worldFlatterPenetratedArmorEffectAndDamageTags(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mining_spell_isolated")
    public static void worldFlatterBlockTargetFilterMatchesPickaxeOrShovel(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.worldFlatterBlockTargetFilterMatchesPickaxeOrShovel(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void worldFlatterEntityAttackRequiresArrivalAndHitsSingleTarget(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.worldFlatterEntityAttackRequiresArrivalAndHitsSingleTarget(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void castingMoveSpeedAdjustmentStopsAtNormalSpeedWithoutNegativeCorrections(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.castingMoveSpeedAdjustmentStopsAtNormalSpeedWithoutNegativeCorrections(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void longStrideMobilityStillAddsBaseMovementSpeedBonus(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.longStrideMobilityStillAddsBaseMovementSpeedBonus(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void longStrideBuoyancyRisesWhileSneakingAndPreservesHorizontalMovement(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios
                .longStrideBuoyancyRisesWhileSneakingAndPreservesHorizontalMovement(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void longStrideStandsOnWaterLavaAndFlowingFluid(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.longStrideStandsOnWaterLavaAndFlowingFluid(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void longStrideFluidMovementRespectsExcludedStatesAndLavaDamage(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios
                .longStrideFluidMovementRespectsExcludedStatesAndLavaDamage(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void comfortBerriesProvideManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.comfortBerriesProvideManaRegenerationAndExpectedFoodValues(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void comfortSandwichProvidesManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.comfortSandwichProvidesManaRegenerationAndExpectedFoodValues(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(GameTestHelper helper) {
        EquipmentSpellBehaviorBridgeGameTestScenarios.manaRegenerationEffectAppliesExpectedFinalManaRegenMultiplier(helper);
    }
}
