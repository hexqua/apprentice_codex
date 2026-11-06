package jp.aquafactory.apprenticecodex.gametest.equipment.luminousdevice;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceStoresOnlyTaggedItemsAndCapsTotal(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceStoresOnlyTaggedItemsAndCapsTotal(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceUsesConfiguredItemCapacityWithoutTruncatingContents(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceUsesConfiguredItemCapacityWithoutTruncatingContents(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceRefillsManaFromSupportedPotionContainers(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceRefillsManaFromSupportedPotionContainers(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceRejectsUnsupportedPotionContainers(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceRejectsUnsupportedPotionContainers(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceTooltipUsesSyncedCapacityAndCyanManaValues(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceTooltipUsesSyncedCapacityAndCyanManaValues(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceManaBarUsesSyncedCapacityWithoutItemDamage(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceManaBarUsesSyncedCapacityWithoutItemDamage(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceRemovalPrefersSelectionAndUpdatesSelection(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceRemovalPrefersSelectionAndUpdatesSelection(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceUsePlacesSelectedBlocksAndKeepsEmptySelection(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceUsePlacesSelectedBlocksAndKeepsEmptySelection(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceEmptyTooltipAndManualEmptyClearGhostSelection(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceEmptyTooltipAndManualEmptyClearGhostSelection(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceAutoStoresOnlyKnownPickedUpItemsAcrossInventoryDevices(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceAutoStoresOnlyKnownPickedUpItemsAcrossInventoryDevices(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceSupportsAndNormalizesMalumEther(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceSupportsAndNormalizesMalumEther(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceModesUpdateSelectionNameAndTooltip(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceModesUpdateSelectionNameAndTooltip(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceWorkbenchUpgradesAreIndependentAndPreserveState(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceWorkbenchUpgradesAreIndependentAndPreserveState(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceWorkbenchMenuCraftsEveryUpgrade(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceWorkbenchMenuCraftsEveryUpgrade(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceCleanModeRemovesLightsAndRecoversConfiguredMana(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceCleanModeRemovesLightsAndRecoversConfiguredMana(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceSpellModeUsesStoredManaWithoutSpellContainer(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceSpellModeUsesStoredManaWithoutSpellContainer(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void luminousDeviceMageLightProfileScalesManaAndDisablesRedundantExtension(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceMageLightProfileScalesManaAndDisablesRedundantExtension(helper);
    }
}
