package jp.aquafactory.apprenticecodex.gametest.equipment.manathruster;

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
    public static void manaThrusterUsesFeetSlotAndDedicatedImplementation(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterUsesFeetSlotAndDedicatedImplementation(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_thruster_config")
    public static void manaThrusterAppliesFixedThrustAndUsesServerConfigManaCost(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterAppliesFixedThrustAndUsesServerConfigManaCost(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_thruster_config")
    public static void manaThrusterInsufficientManaDoesNotAccelerate(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterInsufficientManaDoesNotAccelerate(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_thruster_config")
    public static void manaThrusterKeepsNaturalManaRecoveryAfterThrust(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterKeepsNaturalManaRecoveryAfterThrust(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_thruster_config")
    public static void manaThrusterGroundHeldJumpDoesNotStartAfterTakeoff(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterGroundHeldJumpDoesNotStartAfterTakeoff(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_thruster_config")
    public static void manaThrusterCreativeMayflyAndFlyingDisableThrust(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterCreativeMayflyAndFlyingDisableThrust(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_thruster_config")
    public static void manaThrusterSwimmingAcceleratesForward(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterSwimmingAcceleratesForward(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_thruster_config")
    public static void manaThrusterElytraFlightAcceleratesForward(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterElytraFlightAcceleratesForward(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_thruster_config")
    public static void manaThrusterVanillaJumpMovementContextsDoNotActivate(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterVanillaJumpMovementContextsDoNotActivate(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_thruster_config")
    public static void manaThrusterLavaAllowsUpwardEscape(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterLavaAllowsUpwardEscape(helper);
    }
}
