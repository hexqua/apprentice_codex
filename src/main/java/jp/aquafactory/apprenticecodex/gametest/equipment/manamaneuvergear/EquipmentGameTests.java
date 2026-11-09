package jp.aquafactory.apprenticecodex.gametest.equipment.manamaneuvergear;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.MANA_MANEUVER_GEAR_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void manaManeuverGearUsesFeetSlotAndDedicatedImplementation(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.usesFeetSlotAndDedicatedImplementation(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_MANEUVER_GEAR_CONFIG_BATCH)
    public static void manaManeuverGearWallJumpUsesConfiguredManaAndPreservesHorizontalVelocity(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.wallJumpUsesConfiguredManaAndPreservesHorizontalVelocity(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_MANEUVER_GEAR_CONFIG_BATCH)
    public static void manaManeuverGearWallJumpRejectsGroundInsufficientManaAndRepeatedTick(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.wallJumpRejectsGroundInsufficientManaAndRepeatedTick(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_MANEUVER_GEAR_CONFIG_BATCH)
    public static void manaManeuverGearWallJumpRejectsPassengerWithoutSpendingMana(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.wallJumpRejectsPassengerWithoutSpendingMana(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_MANEUVER_GEAR_CONFIG_BATCH)
    public static void manaManeuverGearWallJumpRequiresWallButAllowsFreeCreativeFlightContext(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.wallJumpRequiresWallButAllowsFreeCreativeFlightContext(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_MANEUVER_GEAR_CONFIG_BATCH)
    public static void manaManeuverGearWallMovementRejectsClimbablesAndScaffolding(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.wallMovementRejectsClimbablesAndScaffolding(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_MANEUVER_GEAR_CONFIG_BATCH)
    public static void manaManeuverGearWallSlideClampsFallingSpeedAndResetsFallDistance(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.wallSlideClampsFallingSpeedAndResetsFallDistance(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_MANEUVER_GEAR_CONFIG_BATCH)
    public static void manaManeuverGearFallDamageUsesFractionalManaAndKeepsFallDistance(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.fallDamageUsesFractionalManaPerDamageAndKeepsFallDistance(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_MANEUVER_GEAR_CONFIG_BATCH)
    public static void manaManeuverGearFallDamageSupportsFullFreeAndZeroManaOutcomes(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.fallDamageSupportsFullFreeAndZeroManaOutcomes(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_MANEUVER_GEAR_CONFIG_BATCH)
    public static void manaManeuverGearRunsBeforeManaShieldAndIgnoresOtherDamage(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.gearRunsBeforeManaShieldAndIgnoresOtherDamage(helper);
    }
}
