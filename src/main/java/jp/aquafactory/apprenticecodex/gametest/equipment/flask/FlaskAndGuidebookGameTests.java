package jp.aquafactory.apprenticecodex.gametest.equipment.flask;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FlaskAndGuidebookGameTests {
    private FlaskAndGuidebookGameTests() {}

    public static final String TEMPLATE = "gametest/basic_floor";

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskAcceptsAllVanillaPotionTypes(GameTestHelper helper) {
        FlaskScenarios.spellcastersFlaskAcceptsAllVanillaPotionTypes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskDrinkingLastDoseClearsStoredItem(GameTestHelper helper) {
        FlaskScenarios.spellcastersFlaskDrinkingLastDoseClearsStoredItem(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskDrinkingGlowEnergyTradesDurationForAmplifier(GameTestHelper helper) {
        FlaskScenarios.spellcastersFlaskDrinkingGlowEnergyTradesDurationForAmplifier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskMismatchedVanillaPotionDrinkConsumesExtraDose(GameTestHelper helper) {
        FlaskScenarios.spellcastersFlaskMismatchedVanillaPotionDrinkConsumesExtraDose(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskBatchExtractionClearsStoredItemAtZero(GameTestHelper helper) {
        FlaskScenarios.spellcastersFlaskBatchExtractionClearsStoredItemAtZero(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskExtractRecipeClearsStoredItemWhenEmpty(GameTestHelper helper) {
        FlaskScenarios.spellcastersFlaskExtractRecipeClearsStoredItemWhenEmpty(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskStartsWithExtractAndNoSpellWheel(GameTestHelper helper) {
        FlaskScenarios.alchemistsFlaskStartsWithExtractAndNoSpellWheel(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskAllowsInstantLongAndContinuousImbues(GameTestHelper helper) {
        FlaskScenarios.alchemistsFlaskAllowsInstantLongAndContinuousImbues(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskAcceptsAllVanillaPotionTypesAndSimpleElixir(GameTestHelper helper) {
        FlaskScenarios.alchemistsFlaskAcceptsAllVanillaPotionTypesAndSimpleElixir(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void flaskMismatchTooltipOnlyWarnsForVanillaPotionTypeMismatch(GameTestHelper helper) {
        FlaskScenarios.flaskMismatchTooltipOnlyWarnsForVanillaPotionTypeMismatch(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void flaskAutomaticFillAcceptsSupportedMismatchedVanillaPotion(GameTestHelper helper) {
        FlaskScenarios.flaskAutomaticFillAcceptsSupportedMismatchedVanillaPotion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskUsesDoubleCapacityAndExtractRecipeSupportsSplashPotion(GameTestHelper helper) {
        FlaskScenarios.alchemistsFlaskUsesDoubleCapacityAndExtractRecipeSupportsSplashPotion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskSmithingConvertsSupportedStoredItemsAndRemovesGuzzle(GameTestHelper helper) {
        FlaskScenarios.alchemistsFlaskSmithingConvertsSupportedStoredItemsAndRemovesGuzzle(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskTippedArrowRecipeConsumesOneDoseAndRejectsSimpleElixir(GameTestHelper helper) {
        FlaskScenarios.alchemistsFlaskTippedArrowRecipeConsumesOneDoseAndRejectsSimpleElixir(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskTippedArrowCraftAwardsAdvancement(GameTestHelper helper) {
        FlaskScenarios.alchemistsFlaskTippedArrowCraftAwardsAdvancement(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void extractPreCastUsesFirstFilledFlaskAcrossHands(GameTestHelper helper) {
        FlaskScenarios.extractPreCastUsesFirstFilledFlaskAcrossHands(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void extractPreCastFailsWithoutFilledAlchemistsFlask(GameTestHelper helper) {
        FlaskScenarios.extractPreCastFailsWithoutFilledAlchemistsFlask(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void extractCastConsumesDoseAndSpawnsExpectedPotionProjectile(GameTestHelper helper) {
        FlaskScenarios.extractCastConsumesDoseAndSpawnsExpectedPotionProjectile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void extractThrownPotionRespectsGlowRedEnergyAndAmplify(GameTestHelper helper) {
        FlaskScenarios.extractThrownPotionRespectsGlowRedEnergyAndAmplify(helper);
    }
}
