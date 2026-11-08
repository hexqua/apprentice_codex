package jp.aquafactory.apprenticecodex.gametest.equipment.spellchargedgreatsword;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellchargedGreatswordGameTests {
    private SpellchargedGreatswordGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordKeepsExpectedStatsAndTags(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordKeepsExpectedStatsTagsAndEnchantments(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordChargeMathDecayAndAttributes(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordChargeMathDecayAndAttributes(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordOverchargeModeConsumesChargeAndUsesConfiguredAttributes(
            GameTestHelper helper
    ) {
        SpellchargedGreatswordGameTestScenarios
                .spellchargedGreatswordOverchargeModeConsumesChargeAndUsesConfiguredAttributes(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcharged_greatsword_config")
    public static void spellchargedGreatswordServerConfigOverridesCombatStats(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordServerConfigOverridesCombatStats(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordOverchargeActivationPausesDecayUntilRelease(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordOverchargeActivationPausesDecayUntilRelease(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordSweepingEdgeBonusAndSweepHitbox(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordSweepingEdgeBonusAndSweepHitbox(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordBetterCombatUsesChargedWeaponAttributes(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordBetterCombatUsesChargedWeaponAttributes(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordEpicFightUsesSweepingEdgeAndOverchargeRefillsInnate(
            GameTestHelper helper
    ) {
        SpellchargedGreatswordGameTestScenarios
                .spellchargedGreatswordEpicFightUsesSweepingEdgeAndOverchargeRefillsInnate(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordChargeEventRequiresMainhand(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordChargeEventRequiresMainhand(helper);
    }

    @GameTest(template = "gametest/basic_floor", timeoutTicks = 140)
    public static void spellchargedGreatswordContinuousRecastRefreshesDecayWithoutExtraCharge(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios
                .spellchargedGreatswordContinuousRecastRefreshesDecayWithoutExtraCharge(helper);
    }

    @GameTest(template = "gametest/basic_floor", timeoutTicks = 140)
    public static void spellchargedGreatswordRecastRefreshesDecayWithoutExtraCharge(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios
                .spellchargedGreatswordRecastRefreshesDecayWithoutExtraCharge(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellchargedGreatswordClampsPersistedFutureOvercharge(GameTestHelper helper) {
        SpellchargedGreatswordGameTestScenarios.spellchargedGreatswordClampsPersistedFutureOvercharge(helper);
    }
}
