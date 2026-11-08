package jp.aquafactory.apprenticecodex.gametest.equipment.flask;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FlaskAndGuidebookGameTests {
    private FlaskAndGuidebookGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void spellcastersFlaskAcceptsAllVanillaPotionTypes(GameTestHelper helper) {
        FlaskScenarios.spellcastersFlaskAcceptsAllVanillaPotionTypes(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellcastersFlaskDrinkingLastDoseClearsStoredItem(GameTestHelper helper) {
        FlaskScenarios.spellcastersFlaskDrinkingLastDoseClearsStoredItem(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellcastersFlaskDrinkingGlowEnergyTradesDurationForAmplifier(GameTestHelper helper) {
        FlaskScenarios.spellcastersFlaskDrinkingGlowEnergyTradesDurationForAmplifier(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellcastersFlaskMismatchedVanillaPotionDrinkConsumesExtraDose(GameTestHelper helper) {
        FlaskScenarios.spellcastersFlaskMismatchedVanillaPotionDrinkConsumesExtraDose(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellcastersFlaskBatchExtractionClearsStoredItemAtZero(GameTestHelper helper) {
        FlaskScenarios.spellcastersFlaskBatchExtractionClearsStoredItemAtZero(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellcastersFlaskExtractRecipeClearsStoredItemWhenEmpty(GameTestHelper helper) {
        FlaskScenarios.spellcastersFlaskExtractRecipeClearsStoredItemWhenEmpty(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void alchemistsFlaskStartsWithExtractAndNoSpellWheel(GameTestHelper helper) {
        FlaskScenarios.alchemistsFlaskStartsWithExtractAndNoSpellWheel(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void alchemistsFlaskAllowsInstantLongAndContinuousImbues(GameTestHelper helper) {
        FlaskScenarios.alchemistsFlaskAllowsInstantLongAndContinuousImbues(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void alchemistsFlaskAcceptsAllVanillaPotionTypesAndSimpleElixir(GameTestHelper helper) {
        FlaskScenarios.alchemistsFlaskAcceptsAllVanillaPotionTypesAndSimpleElixir(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void flaskMismatchTooltipOnlyWarnsForVanillaPotionTypeMismatch(GameTestHelper helper) {
        FlaskScenarios.flaskMismatchTooltipOnlyWarnsForVanillaPotionTypeMismatch(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void flaskAutomaticFillAcceptsSupportedMismatchedVanillaPotion(GameTestHelper helper) {
        FlaskScenarios.flaskAutomaticFillAcceptsSupportedMismatchedVanillaPotion(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void alchemistsFlaskUsesDoubleCapacityAndExtractRecipeSupportsSplashPotion(GameTestHelper helper) {
        FlaskScenarios.alchemistsFlaskUsesDoubleCapacityAndExtractRecipeSupportsSplashPotion(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void alchemistsFlaskSmithingConvertsSupportedStoredItemsAndRemovesGuzzle(GameTestHelper helper) {
        FlaskScenarios.alchemistsFlaskSmithingConvertsSupportedStoredItemsAndRemovesGuzzle(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void alchemistsFlaskTippedArrowRecipeConsumesOneDoseAndRejectsSimpleElixir(GameTestHelper helper) {
        FlaskScenarios.alchemistsFlaskTippedArrowRecipeConsumesOneDoseAndRejectsSimpleElixir(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void alchemistsFlaskTippedArrowCraftAwardsAdvancement(GameTestHelper helper) {
        FlaskScenarios.alchemistsFlaskTippedArrowCraftAwardsAdvancement(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void extractPreCastUsesFirstFilledFlaskAcrossHands(GameTestHelper helper) {
        FlaskScenarios.extractPreCastUsesFirstFilledFlaskAcrossHands(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void extractPreCastFailsWithoutFilledAlchemistsFlask(GameTestHelper helper) {
        FlaskScenarios.extractPreCastFailsWithoutFilledAlchemistsFlask(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void extractCastConsumesDoseAndSpawnsExpectedPotionProjectile(GameTestHelper helper) {
        FlaskScenarios.extractCastConsumesDoseAndSpawnsExpectedPotionProjectile(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void extractThrownPotionRespectsGlowRedEnergyAndAmplify(GameTestHelper helper) {
        FlaskScenarios.extractThrownPotionRespectsGlowRedEnergyAndAmplify(helper);
    }
}
