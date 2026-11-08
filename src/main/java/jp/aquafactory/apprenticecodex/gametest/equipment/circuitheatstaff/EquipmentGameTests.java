package jp.aquafactory.apprenticecodex.gametest.equipment.circuitheatstaff;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

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
    public static void circuitHeatStaffClampsPersistedFutureItemOverheat(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffClampsPersistedFutureItemOverheat(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void circuitHeatStaffKeepsStoredLongItemOverheatDuration(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffKeepsStoredLongItemOverheatDuration(helper);
    }
}
