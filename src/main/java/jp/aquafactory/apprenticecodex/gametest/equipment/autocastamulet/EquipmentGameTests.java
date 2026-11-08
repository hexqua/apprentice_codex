package jp.aquafactory.apprenticecodex.gametest.equipment.autocastamulet;

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
    public static void autocastAmuletStartsWithSingleHiddenSpellSlotAndLoadedAllowlist(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletStartsWithSingleHiddenSpellSlotAndLoadedAllowlist(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletNormalizationDropsBlockedSpellsAndClampsSlots(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNormalizationDropsBlockedSpellsAndClampsSlots(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletWisdomShardIsAdjustmentOnlyProfileGate(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletWisdomShardIsAdjustmentOnlyProfileGate(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletWisdomShardBlocksUnprofiledAutoCast(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletWisdomShardBlocksUnprofiledAutoCast(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletWisdomShardProfileConditionsUseAndSemantics(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletWisdomShardProfileConditionsUseAndSemantics(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletSpellSlotUpgradeEnablesFourSlotsAndKeepsOrder(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletSpellSlotUpgradeEnablesFourSlotsAndKeepsOrder(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletKeepsDisabledSlotSpellsAfterRemovingUpgrade(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletKeepsDisabledSlotSpellsAfterRemovingUpgrade(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletWorkbenchExtractionUsesLastSpellAndKeepsSlotCount(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletWorkbenchExtractionUsesLastSpellAndKeepsSlotCount(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletAutoCastStartsOnFirstIntervalAfterEquip(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletAutoCastStartsOnFirstIntervalAfterEquip(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletInsufficientManaDelaysRetryAndSkipsErroredSlotOnce(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletInsufficientManaDelaysRetryAndSkipsErroredSlotOnce(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletCreativeCastIgnoresManaCost(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletCreativeCastIgnoresManaCost(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletCooldownIgnoresSwordMultiplier(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletCooldownIgnoresSwordMultiplier(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletLongSpellCompletesImmediately(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletLongSpellCompletesImmediately(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletNotificationControllerSchedulesCastAndThresholds(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNotificationControllerSchedulesCastAndThresholds(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletNotificationControllerSkipsUnreachedThresholds(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNotificationControllerSkipsUnreachedThresholds(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletNotificationControllerSkipsCooldownsUnderFiveSeconds(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNotificationControllerSkipsCooldownsUnderFiveSeconds(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletNotificationControllerQueuesInOrderAndKeepsDelayedLabel(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNotificationControllerQueuesInOrderAndKeepsDelayedLabel(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletNotificationControllerUpdatesRemainingCounts(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletNotificationControllerUpdatesRemainingCounts(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void autocastAmuletDeletesPersistedFutureRetryTick(GameTestHelper helper) {
        AutocastAmuletGameTestScenarios.autocastAmuletDeletesPersistedFutureRetryTick(helper);
    }
}
