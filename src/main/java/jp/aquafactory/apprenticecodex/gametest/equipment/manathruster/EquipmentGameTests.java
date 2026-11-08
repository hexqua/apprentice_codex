package jp.aquafactory.apprenticecodex.gametest.equipment.manathruster;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.MANA_THRUSTER_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

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
    public static void manaThrusterKeepsNaturalManaRecoveryAfterThrust(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterKeepsNaturalManaRecoveryAfterThrust(helper);
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
    public static void manaThrusterSwimmingAcceleratesForward(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterSwimmingAcceleratesForward(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_THRUSTER_CONFIG_BATCH)
    public static void manaThrusterElytraFlightAcceleratesForward(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterElytraFlightAcceleratesForward(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_THRUSTER_CONFIG_BATCH)
    public static void manaThrusterVanillaJumpMovementContextsDoNotActivate(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterVanillaJumpMovementContextsDoNotActivate(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_THRUSTER_CONFIG_BATCH)
    public static void manaThrusterLavaAllowsUpwardEscape(GameTestHelper helper) {
        ManaThrusterGameTestScenarios.manaThrusterLavaAllowsUpwardEscape(helper);
    }
}
