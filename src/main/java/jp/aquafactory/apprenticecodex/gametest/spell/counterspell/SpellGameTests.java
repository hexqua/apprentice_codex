package jp.aquafactory.apprenticecodex.gametest.spell.counterspell;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.counterspell_compat_isolated")
    public static void counterspellCompatMagicMobEffectsAreMagicMobEffects(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.counterspellCompatMagicMobEffectsAreMagicMobEffects(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.counterspell_compat_isolated")
    public static void spectralWingEffectRemovalClearsFlightState(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.spectralWingEffectRemovalClearsFlightState(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.counterspell_compat_isolated")
    public static void counterspellCompatMagicConstructsDeactivateSafely(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.counterspellCompatMagicConstructsDeactivateSafely(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.counterspell_compat_isolated", timeoutTicks = 40)
    public static void healingBloomAntiMagicUsesDeathCleanup(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.healingBloomAntiMagicUsesDeathCleanup(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.counterspell_compat_isolated", timeoutTicks = 40)
    public static void counterspellCompatProjectilesFizzleHarmlessly(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.counterspellCompatProjectilesFizzleHarmlessly(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.counterspell_compat_isolated", timeoutTicks = 40)
    public static void magicSpearAntiMagicBurstDoesNotRestart(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.magicSpearAntiMagicBurstDoesNotRestart(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.counterspell_compat_isolated", timeoutTicks = 40)
    public static void uniteLunaAntiMagicAmplifiesBurst(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.uniteLunaAntiMagicAmplifiesBurst(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.counterspell_compat_isolated")
    public static void counterspellCompatSpecialPlayerTargetBehaviors(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.counterspellCompatSpecialPlayerTargetBehaviors(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.moon_light_counterspell_isolated")
    public static void moonLightSuccessfulDamageAppliesCounterspellEffects(GameTestHelper helper) {
        MoonLightCounterspellGameTestScenarios.successfulDamageAppliesCounterspellEffects(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.moon_light_counterspell_isolated")
    public static void moonLightFailedDamageDoesNotApplyCounterspellEffects(GameTestHelper helper) {
        MoonLightCounterspellGameTestScenarios.failedDamageDoesNotApplyCounterspellEffects(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.moon_light_counterspell_isolated")
    public static void moonLightCanceledCounterspellEventKeepsAdditionalEffects(GameTestHelper helper) {
        MoonLightCounterspellGameTestScenarios.canceledCounterspellEventKeepsAdditionalEffects(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.moon_light_counterspell_isolated")
    public static void moonLightSuccessfulDamageCancelsMagicEntityCast(GameTestHelper helper) {
        MoonLightCounterspellGameTestScenarios.successfulDamageCancelsMagicEntityCast(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.moon_light_counterspell_isolated", timeoutTicks = 40)
    public static void moonLightChargeCutAppliesCounterspellEffects(GameTestHelper helper) {
        MoonLightCounterspellGameTestScenarios.chargeCutAppliesCounterspellEffects(helper);
    }
}
