package jp.aquafactory.apprenticecodex.gametest.equipment.luminousdevice;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.LUMINOUS_DEVICE_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceStoresOnlyTaggedItemsAndCapsTotal(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceStoresOnlyTaggedItemsAndCapsTotal(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceUsesConfiguredItemCapacityWithoutTruncatingContents(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceUsesConfiguredItemCapacityWithoutTruncatingContents(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceRefillsManaFromSupportedPotionContainers(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceRefillsManaFromSupportedPotionContainers(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceRejectsUnsupportedPotionContainers(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceRejectsUnsupportedPotionContainers(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceTooltipUsesSyncedCapacityAndCyanManaValues(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceTooltipUsesSyncedCapacityAndCyanManaValues(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceManaBarUsesSyncedCapacityWithoutItemDamage(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceManaBarUsesSyncedCapacityWithoutItemDamage(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceRemovalPrefersSelectionAndUpdatesSelection(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceRemovalPrefersSelectionAndUpdatesSelection(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceUsePlacesSelectedBlocksAndKeepsEmptySelection(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceUsePlacesSelectedBlocksAndKeepsEmptySelection(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceEmptyTooltipAndManualEmptyClearGhostSelection(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceEmptyTooltipAndManualEmptyClearGhostSelection(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceAutoStoresOnlyKnownPickedUpItemsAcrossInventoryDevices(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceAutoStoresOnlyKnownPickedUpItemsAcrossInventoryDevices(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceSupportsAndNormalizesMalumEther(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceSupportsAndNormalizesMalumEther(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceModesUpdateSelectionNameAndTooltip(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceModesUpdateSelectionNameAndTooltip(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceWorkbenchUpgradesAreIndependentAndPreserveState(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceWorkbenchUpgradesAreIndependentAndPreserveState(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceWorkbenchMenuCraftsEveryUpgrade(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceWorkbenchMenuCraftsEveryUpgrade(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceCleanModeRemovesLightsAndRecoversConfiguredMana(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceCleanModeRemovesLightsAndRecoversConfiguredMana(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceSpellModeUsesStoredManaWithoutSpellContainer(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceSpellModeUsesStoredManaWithoutSpellContainer(helper);
    }

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void luminousDeviceMageLightProfileScalesManaAndDisablesRedundantExtension(GameTestHelper helper) {
        LuminousDeviceGameTestScenarios.luminousDeviceMageLightProfileScalesManaAndDisablesRedundantExtension(helper);
    }
}
