package jp.aquafactory.apprenticecodex.gametest.equipment.manamaneuvergear;

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
    public static void manaManeuverGearUsesFeetSlotAndDedicatedImplementation(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.usesFeetSlotAndDedicatedImplementation(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_maneuver_gear_config")
    public static void manaManeuverGearWallJumpUsesConfiguredManaAndPreservesHorizontalVelocity(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.wallJumpUsesConfiguredManaAndPreservesHorizontalVelocity(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_maneuver_gear_config")
    public static void manaManeuverGearWallJumpRejectsGroundInsufficientManaAndRepeatedTick(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.wallJumpRejectsGroundInsufficientManaAndRepeatedTick(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_maneuver_gear_config")
    public static void manaManeuverGearWallJumpRejectsPassengerWithoutSpendingMana(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.wallJumpRejectsPassengerWithoutSpendingMana(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_maneuver_gear_config")
    public static void manaManeuverGearWallJumpRequiresWallButAllowsFreeCreativeFlightContext(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.wallJumpRequiresWallButAllowsFreeCreativeFlightContext(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_maneuver_gear_config")
    public static void manaManeuverGearWallMovementRejectsClimbablesAndScaffolding(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.wallMovementRejectsClimbablesAndScaffolding(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_maneuver_gear_config")
    public static void manaManeuverGearWallSlideClampsFallingSpeedAndResetsFallDistance(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.wallSlideClampsFallingSpeedAndResetsFallDistance(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_maneuver_gear_config")
    public static void manaManeuverGearFallDamageUsesFractionalManaAndKeepsFallDistance(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.fallDamageUsesFractionalManaPerDamageAndKeepsFallDistance(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_maneuver_gear_config")
    public static void manaManeuverGearFallDamageSupportsFullFreeAndZeroManaOutcomes(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.fallDamageSupportsFullFreeAndZeroManaOutcomes(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_maneuver_gear_config")
    public static void manaManeuverGearRunsBeforeManaShieldAndIgnoresOtherDamage(GameTestHelper helper) {
        ManaManeuverGearGameTestScenarios.gearRunsBeforeManaShieldAndIgnoresOtherDamage(helper);
    }
}
