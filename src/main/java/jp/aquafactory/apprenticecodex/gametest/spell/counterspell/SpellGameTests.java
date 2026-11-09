package jp.aquafactory.apprenticecodex.gametest.spell.counterspell;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH)
    public static void counterspellCompatMagicMobEffectsAreMagicMobEffects(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.counterspellCompatMagicMobEffectsAreMagicMobEffects(helper);
    }

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH)
    public static void spectralWingEffectRemovalClearsFlightState(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.spectralWingEffectRemovalClearsFlightState(helper);
    }

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH)
    public static void counterspellCompatMagicConstructsDeactivateSafely(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.counterspellCompatMagicConstructsDeactivateSafely(helper);
    }

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH, timeoutTicks = 40)
    public static void healingBloomAntiMagicUsesDeathCleanup(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.healingBloomAntiMagicUsesDeathCleanup(helper);
    }

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH, timeoutTicks = 40)
    public static void counterspellCompatProjectilesFizzleHarmlessly(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.counterspellCompatProjectilesFizzleHarmlessly(helper);
    }

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH, timeoutTicks = 40)
    public static void magicSpearAntiMagicBurstDoesNotRestart(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.magicSpearAntiMagicBurstDoesNotRestart(helper);
    }

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH, timeoutTicks = 40)
    public static void uniteLunaAntiMagicAmplifiesBurst(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.uniteLunaAntiMagicAmplifiesBurst(helper);
    }

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH)
    public static void counterspellCompatSpecialPlayerTargetBehaviors(GameTestHelper helper) {
        CounterspellCompatibilityScenarios.counterspellCompatSpecialPlayerTargetBehaviors(helper);
    }

    @GameTest(template = TEMPLATE, batch = MOON_LIGHT_COUNTERSPELL_ISOLATED_BATCH)
    public static void moonLightSuccessfulDamageAppliesCounterspellEffects(GameTestHelper helper) {
        MoonLightCounterspellGameTestScenarios.successfulDamageAppliesCounterspellEffects(helper);
    }

    @GameTest(template = TEMPLATE, batch = MOON_LIGHT_COUNTERSPELL_ISOLATED_BATCH)
    public static void moonLightFailedDamageDoesNotApplyCounterspellEffects(GameTestHelper helper) {
        MoonLightCounterspellGameTestScenarios.failedDamageDoesNotApplyCounterspellEffects(helper);
    }

    @GameTest(template = TEMPLATE, batch = MOON_LIGHT_COUNTERSPELL_ISOLATED_BATCH)
    public static void moonLightCanceledCounterspellEventKeepsAdditionalEffects(GameTestHelper helper) {
        MoonLightCounterspellGameTestScenarios.canceledCounterspellEventKeepsAdditionalEffects(helper);
    }

    @GameTest(template = TEMPLATE, batch = MOON_LIGHT_COUNTERSPELL_ISOLATED_BATCH)
    public static void moonLightSuccessfulDamageCancelsMagicEntityCast(GameTestHelper helper) {
        MoonLightCounterspellGameTestScenarios.successfulDamageCancelsMagicEntityCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = MOON_LIGHT_COUNTERSPELL_ISOLATED_BATCH, timeoutTicks = 40)
    public static void moonLightChargeCutAppliesCounterspellEffects(GameTestHelper helper) {
        MoonLightCounterspellGameTestScenarios.chargeCutAppliesCounterspellEffects(helper);
    }
}
