package jp.aquafactory.apprenticecodex.gametest.equipment.autocastamulet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletStartsWithSingleHiddenSpellSlotAndLoadedAllowlist(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletStartsWithSingleHiddenSpellSlotAndLoadedAllowlist(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNormalizationDropsBlockedSpellsAndClampsSlots(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNormalizationDropsBlockedSpellsAndClampsSlots(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletWisdomShardIsAdjustmentOnlyProfileGate(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletWisdomShardIsAdjustmentOnlyProfileGate(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletWisdomShardBlocksUnprofiledAutoCast(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletWisdomShardBlocksUnprofiledAutoCast(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletWisdomShardProfileConditionsUseAndSemantics(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletWisdomShardProfileConditionsUseAndSemantics(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletSpellSlotUpgradeEnablesFourSlotsAndKeepsOrder(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletSpellSlotUpgradeEnablesFourSlotsAndKeepsOrder(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletKeepsDisabledSlotSpellsAfterRemovingUpgrade(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletKeepsDisabledSlotSpellsAfterRemovingUpgrade(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletWorkbenchExtractionUsesLastSpellAndKeepsSlotCount(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletWorkbenchExtractionUsesLastSpellAndKeepsSlotCount(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletAutoCastStartsOnFirstIntervalAfterEquip(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletAutoCastStartsOnFirstIntervalAfterEquip(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletInsufficientManaDelaysRetryAndSkipsErroredSlotOnce(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletInsufficientManaDelaysRetryAndSkipsErroredSlotOnce(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletCreativeCastIgnoresManaCost(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletCreativeCastIgnoresManaCost(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletCooldownIgnoresSwordMultiplier(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletCooldownIgnoresSwordMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletLongSpellCompletesImmediately(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletLongSpellCompletesImmediately(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNotificationControllerSchedulesCastAndThresholds(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNotificationControllerSchedulesCastAndThresholds(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNotificationControllerSkipsUnreachedThresholds(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNotificationControllerSkipsUnreachedThresholds(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNotificationControllerSkipsCooldownsUnderFiveSeconds(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNotificationControllerSkipsCooldownsUnderFiveSeconds(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNotificationControllerQueuesInOrderAndKeepsDelayedLabel(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNotificationControllerQueuesInOrderAndKeepsDelayedLabel(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletNotificationControllerUpdatesRemainingCounts(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNotificationControllerUpdatesRemainingCounts(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void autocastAmuletDeletesPersistedFutureRetryTick(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletDeletesPersistedFutureRetryTick(helper);
    }
}
