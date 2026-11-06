package jp.aquafactory.apprenticecodex.gametest.equipment.circuitheatstaff;

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
    public static void circuitHeatStaffAdditionalManaScalesWithSkippedCooldown(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffAdditionalManaScalesWithSkippedCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.circuit_heat_staff_additional_mana_config")
    public static void circuitHeatStaffAdditionalManaUsesServerConfig(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffAdditionalManaUsesServerConfig(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void circuitHeatStaffOverheatUsesCastCooldownPlusSkippedCooldown(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffOverheatUsesCastCooldownPlusSkippedCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.circuit_heat_staff_overheat_min_config")
    public static void circuitHeatStaffOverheatDurationUsesServerMinTicks(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffOverheatDurationUsesServerMinTicks(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.circuit_heat_staff_overheat_cap_config")
    public static void circuitHeatStaffOverheatDurationUsesServerCapTicks(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffOverheatDurationUsesServerCapTicks(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void circuitHeatStaffBypassKeepsBaseManaGate(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffBypassKeepsBaseManaGate(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.circuit_heat_staff_cooldown_limit_config")
    public static void circuitHeatStaffCooldownLimitBlocksBypass(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffCooldownLimitBlocksBypass(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.circuit_heat_staff_spell_denylist_config")
    public static void circuitHeatStaffSpellDenylistBlocksBypass(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffSpellDenylistBlocksBypass(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void circuitHeatStaffRightClickBypassesCooldownButShortcutDoesNot(GameTestHelper helper) {
        CircuitHeatStaffBypassScenarios.circuitHeatStaffRightClickBypassesCooldownButShortcutDoesNot(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void circuitHeatStaffContinuousBypassKeepsOverheatManaCost(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffContinuousBypassKeepsOverheatManaCost(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void circuitHeatStaffRecastDoesNotTouchBypassOverheatState(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffRecastDoesNotTouchBypassOverheatState(helper);
    }

    @GameTest(template = "gametest/basic_floor", timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingConsumesWaterSource(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingConsumesWaterSource(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.circuit_heat_staff_cooling_disabled_config", timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingDisabledByServerConfig(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingDisabledByServerConfig(helper);
    }

    @GameTest(template = "gametest/basic_floor", timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingIgnoresFlowingWater(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingIgnoresFlowingWater(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.circuit_heat_staff_water_consumption_config", timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingKeepsWaterSourceWhenConsumptionDisabled(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingKeepsWaterSourceWhenConsumptionDisabled(helper);
    }

    @GameTest(template = "gametest/basic_floor", timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingConsumesCauldronLevel(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingConsumesCauldronLevel(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.circuit_heat_staff_cauldron_consumption_config", timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingKeepsWaterCauldronWhenConsumptionDisabled(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingKeepsWaterCauldronWhenConsumptionDisabled(helper);
    }

    @GameTest(template = "gametest/basic_floor", timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingKeepsPowderSnowBlock(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingKeepsPowderSnowBlock(helper);
    }

    @GameTest(template = "gametest/basic_floor", timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingKeepsPowderSnowCauldronLevel(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingKeepsPowderSnowCauldronLevel(helper);
    }

    @GameTest(template = "gametest/basic_floor", timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingIgnoresNonOverheatedStaffInPowderSnow(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingIgnoresNonOverheatedStaffInPowderSnow(helper);
    }

    @GameTest(template = "gametest/basic_floor", timeoutTicks = 80)
    public static void circuitHeatStaffDropCoolingIgnoresNonOverheatedStaff(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffDropCoolingIgnoresNonOverheatedStaff(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void circuitHeatStaffClampsPersistedFutureItemOverheat(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffClampsPersistedFutureItemOverheat(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void circuitHeatStaffKeepsStoredLongItemOverheatDuration(GameTestHelper helper) {
        CircuitHeatStaffGameTestScenarios.circuitHeatStaffKeepsStoredLongItemOverheatDuration(helper);
    }
}
