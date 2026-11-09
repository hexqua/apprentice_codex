package jp.aquafactory.apprenticecodex.gametest.equipment.spellchargedgreatsword;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.SPELLCHARGED_GREATSWORD_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellchargedGreatswordGameTests {
    private SpellchargedGreatswordGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordKeepsExpectedStatsAndTags(GameTestHelper helper) {
        SpellchargedGreatswordScenarios.spellchargedGreatswordKeepsExpectedStatsAndTags(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordChargeMathDecayAndAttributes(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordChargeMathDecayAndAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordOverchargeModeConsumesChargeAndUsesConfiguredAttributes(
            GameTestHelper helper
    ) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordOverchargeModeConsumesChargeAndUsesConfiguredAttributes(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCHARGED_GREATSWORD_CONFIG_BATCH)
    public static void spellchargedGreatswordServerConfigOverridesCombatStats(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordServerConfigOverridesCombatStats(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordOverchargeActivationPausesDecayUntilRelease(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordOverchargeActivationPausesDecayUntilRelease(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordSweepingEdgeBonusAndSweepHitbox(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordSweepingEdgeBonusAndSweepHitbox(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordBetterCombatUsesChargedWeaponAttributes(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordBetterCombatUsesChargedWeaponAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordEpicFightUsesSweepingEdgeAndOverchargeRefillsInnate(
            GameTestHelper helper
    ) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordEpicFightUsesSweepingEdgeAndOverchargeRefillsInnate(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordChargeEventRequiresMainhand(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordChargeEventRequiresMainhand(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void spellchargedGreatswordContinuousRecastRefreshesDecayWithoutExtraCharge(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordContinuousRecastRefreshesDecayWithoutExtraCharge(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void spellchargedGreatswordRecastRefreshesDecayWithoutExtraCharge(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordRecastRefreshesDecayWithoutExtraCharge(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellchargedGreatswordClampsPersistedFutureOvercharge(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordClampsPersistedFutureOvercharge(helper);
    }
}
