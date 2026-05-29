package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexFlaskAndGuidebookGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private ApprenticeCodexFlaskAndGuidebookGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskAcceptsAllVanillaPotionTypes(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellcastersFlaskAcceptsAllVanillaPotionTypes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskDrinkingLastDoseClearsStoredItem(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellcastersFlaskDrinkingLastDoseClearsStoredItem(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskDrinkingGlowEnergyTradesDurationForAmplifier(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellcastersFlaskDrinkingGlowEnergyTradesDurationForAmplifier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskMismatchedVanillaPotionDrinkConsumesExtraDose(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellcastersFlaskMismatchedVanillaPotionDrinkConsumesExtraDose(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskBatchExtractionClearsStoredItemAtZero(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellcastersFlaskBatchExtractionClearsStoredItemAtZero(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskExtractRecipeClearsStoredItemWhenEmpty(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellcastersFlaskExtractRecipeClearsStoredItemWhenEmpty(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskStartsWithExtractAndNoSpellWheel(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.alchemistsFlaskStartsWithExtractAndNoSpellWheel(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskAllowsInstantLongAndContinuousImbues(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.alchemistsFlaskAllowsInstantLongAndContinuousImbues(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskAcceptsAllVanillaPotionTypesAndSimpleElixir(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.alchemistsFlaskAcceptsAllVanillaPotionTypesAndSimpleElixir(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void flaskMismatchTooltipOnlyWarnsForVanillaPotionTypeMismatch(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.flaskMismatchTooltipOnlyWarnsForVanillaPotionTypeMismatch(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void flaskAutomaticFillTypeGateRejectsMismatchedEmptyVanillaPotion(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.flaskAutomaticFillTypeGateRejectsMismatchedEmptyVanillaPotion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskUsesDoubleCapacityAndExtractRecipeSupportsSplashPotion(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.alchemistsFlaskUsesDoubleCapacityAndExtractRecipeSupportsSplashPotion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskSmithingConvertsSupportedStoredItemsAndRemovesGuzzle(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.alchemistsFlaskSmithingConvertsSupportedStoredItemsAndRemovesGuzzle(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskTippedArrowRecipeConsumesOneDoseAndRejectsSimpleElixir(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.alchemistsFlaskTippedArrowRecipeConsumesOneDoseAndRejectsSimpleElixir(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskTippedArrowCraftAwardsAdvancement(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.alchemistsFlaskTippedArrowCraftAwardsAdvancement(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void extractPreCastUsesFirstFilledFlaskAcrossHands(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.extractPreCastUsesFirstFilledFlaskAcrossHands(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void extractPreCastFailsWithoutFilledAlchemistsFlask(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.extractPreCastFailsWithoutFilledAlchemistsFlask(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void extractCastConsumesDoseAndSpawnsExpectedPotionProjectile(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.extractCastConsumesDoseAndSpawnsExpectedPotionProjectile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void extractThrownPotionRespectsGlowRedEnergyAndAmplify(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.extractThrownPotionRespectsGlowRedEnergyAndAmplify(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void isekaiTravelGuidebookStartsWithTwoFixedSpellsAndNoAttributes(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.isekaiTravelGuidebookStartsWithTwoFixedSpellsAndNoAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellStainedRunicTabletUsesDefaultServerConfigValues(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellStainedRunicTabletUsesDefaultServerConfigValues(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellStainedRunicTabletAcceptsNegativeServerConfigValues(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellStainedRunicTabletAcceptsNegativeServerConfigValues(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellStainedRunicTabletFiltersSchoolPowerByConfiguredThresholds(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellStainedRunicTabletFiltersSchoolPowerByConfiguredThresholds(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void explorersCodexGuidebookTransferRecipeMovesFixedSpellsAndKeepsExplorersData(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.explorersCodexGuidebookTransferRecipeMovesFixedSpellsAndKeepsExplorersData(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void explorersCodexGuidebookTransferRecipeIgnoresDuplicateGuidebookSpell(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.explorersCodexGuidebookTransferRecipeIgnoresDuplicateGuidebookSpell(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void explorersCodexGuidebookTransferRecipeRejectsSpellSlotOverflow(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.explorersCodexGuidebookTransferRecipeRejectsSpellSlotOverflow(helper);
    }
}
