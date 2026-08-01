package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexCoreGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String PROCESSING_RECIPE_DENYLIST_CONFIG_BATCH = "processing_recipe_denylist_config";

    private ApprenticeCodexCoreGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void requiredOptionalModsAreLoaded(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.requiredOptionalModsAreLoaded(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void registriesAndDynamicContentAreRegistered(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.registriesAndDynamicContentAreRegistered(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void tinyLumberjackRecognizesMalumRunewoodAndSoulwoodLogs(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.tinyLumberjackRecognizesMalumRunewoodAndSoulwoodLogs(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void terraResonanceTargetTagIncludesSourcesClustersAndImmatureAmethyst(GameTestHelper helper) {
        TerraResonanceGameTestScenarios.targetTagIncludesSourcesClustersAndImmatureAmethyst(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void terraResonanceSearchExtendsInwardFromEverySelectedFaceAndKeepsSnapshot(GameTestHelper helper) {
        TerraResonanceGameTestScenarios.searchExtendsInwardFromEverySelectedFaceAndKeepsSnapshot(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void comfortBerriesCanBePottedAsDecoration(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.comfortBerriesCanBePottedAsDecoration(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void assistWingsOnlyJumpItemsTagIncludesSmashcastScepter(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.assistWingsOnlyJumpItemsTagIncludesSmashcastScepter(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaMendingRequiresDamagedHeldItem(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaMendingRequiresDamagedHeldItem(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaMendingRepairsMainHandDamagedItem(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaMendingRepairsMainHandDamagedItem(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaMendingPrefersOffhandDamagedItem(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaMendingPrefersOffhandDamagedItem(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaMendingCraftsmansDelightBoostsRepairAndClearsRepairCost(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.manaMendingCraftsmansDelightBoostsRepairAndClearsRepairCost(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void searchBeaconRefundLogicOnlyRefundsWhenUnknownStructuresAreAbsent(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.searchBeaconRefundLogicOnlyRefundsWhenUnknownStructuresAreAbsent(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void instantSearchBrazierUseCreatesConfiguredSingleOfferBeaconAndRefundsBeforeSearch(
            GameTestHelper helper
    ) {
        InstantSearchBrazierGameTestScenarios
                .useCreatesConfiguredSingleOfferBeaconAndRefundsBeforeSearch(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void instantSearchBrazierSearchStartStopsBrazierRefundAndRejectsAdditionalOffer(
            GameTestHelper helper
    ) {
        InstantSearchBrazierGameTestScenarios
                .searchStartStopsBrazierRefundAndRejectsAdditionalOffer(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void villagerCanClaimApprenticeDeskAsErrandMageJobSite(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.villagerCanClaimApprenticeDeskAsErrandMageJobSite(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void errandMageVillageHouseIsAddedToVanillaVillagePools(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.errandMageVillageHouseIsAddedToVanillaVillagePools(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void errandMageVillageHouseTemplatesAreLoadableAndKeepRequiredJigsaws(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.errandMageVillageHouseTemplatesAreLoadableAndKeepRequiredJigsaws(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void errandMageOffersAcceptTaggedErrandMagePayments(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.errandMageOffersAcceptTaggedErrandMagePayments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void errandMageTradesMatchExpectedOffers(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.errandMageTradesMatchExpectedOffers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void customRecipeDataIsLoaded(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.customRecipeDataIsLoaded(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcasterWorkbenchExtractsEligibleSingleSlotSpells(GameTestHelper helper) {
        SpellExtractionGameTestScenarios.spellcasterWorkbenchExtractsEligibleSingleSlotSpells(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcasterWorkbenchReportsExtractionBlockReasons(GameTestHelper helper) {
        SpellExtractionGameTestScenarios.spellcasterWorkbenchReportsExtractionBlockReasons(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcasterWorkbenchDoesNotConsumeExtractionInputsWhenOutputCannotMove(GameTestHelper helper) {
        SpellExtractionGameTestScenarios.spellcasterWorkbenchDoesNotConsumeExtractionInputsWhenOutputCannotMove(helper);
    }

    @GameTest(template = TEMPLATE, batch = PROCESSING_RECIPE_DENYLIST_CONFIG_BATCH)
    public static void processingRecipeDenylistsRejectConfiguredRecipeIds(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.processingRecipeDenylistsRejectConfiguredRecipeIds(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void serverBlocksAndEntitiesCanBeInstantiated(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.serverBlocksAndEntitiesCanBeInstantiated(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void wizardlampUsesClientCellAndClampsItWithoutLineOfSight(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.wizardlampUsesClientCellAndClampsItWithoutLineOfSight(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void blockPlacementSpellsRespectForgePlaceEvent(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.blockPlacementSpellsRespectForgePlaceEvent(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void wizardlampLanternHasLanternCollisionAndNoDrops(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.wizardlampLanternHasLanternCollisionAndNoDrops(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCalibrationBenchStoresScrollsOnGauntlet(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellCalibrationBenchStoresScrollsOnGauntlet(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void scrollcasterGauntletEmptySelectionViewsTrackEnabledSlots(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.scrollcasterGauntletEmptySelectionViewsTrackEnabledSlots(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void scrollcasterGauntletSelectedScrollDrivesImbuedSpell(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.scrollcasterGauntletSelectedScrollDrivesImbuedSpell(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void scrollcasterGauntletFreecastStaffAdjustmentEnablesSwingcast(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.scrollcasterGauntletFreecastStaffAdjustmentEnablesSwingcast(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void scrollcasterGauntletEpicFightMirroredOffhandSwingcastUsesMainhand(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.scrollcasterGauntletEpicFightMirroredOffhandSwingcastUsesMainhand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void scrollcasterGauntletEpicFightFallbackIgnoresUnadjustedGauntlet(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.scrollcasterGauntletEpicFightFallbackIgnoresUnadjustedGauntlet(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCalibrationBenchAcceptsGauntletFreecastStaffAdjustment(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellCalibrationBenchAcceptsGauntletFreecastStaffAdjustment(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCalibrationBenchStoresScrollsOnRevolvercastStaff(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellCalibrationBenchStoresScrollsOnRevolvercastStaff(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void revolvercastStaffSelectedScrollNormalizesAndDrivesSpellWheel(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.revolvercastStaffSelectedScrollNormalizesAndDrivesSpellWheel(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void revolvercastStaffCooldownFailureAdvancesOnlyInSkipMode(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.revolvercastStaffCooldownFailureAdvancesOnlyInSkipMode(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void revolvercastStaffSuccessfulCastAdvancesAfterCompletionTick(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.revolvercastStaffSuccessfulCastAdvancesAfterCompletionTick(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void revolvercastStaffPendingAdvanceSurvivesUnrelatedCastCompletion(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.revolvercastStaffPendingAdvanceSurvivesUnrelatedCastCompletion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void revolvercastStaffCancelledCastDoesNotAdvancePendingSelection(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.revolvercastStaffCancelledCastDoesNotAdvancePendingSelection(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void revolvercastStaffBlocksArcaneAnvilAndUsesDiamondSwingcastRestrictions(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.revolvercastStaffBlocksArcaneAnvilAndUsesDiamondSwingcastRestrictions(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void scrollcasterGauntletStopsCreativeBlockAttackLikeVanillaSword(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.scrollcasterGauntletStopsCreativeBlockAttackLikeVanillaSword(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCalibrationBenchAdjustmentSlotsValidateInputs(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellCalibrationBenchAdjustmentSlotsValidateInputs(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCalibrationBenchSchoolRuneRetunesGauntletSpellPower(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellCalibrationBenchSchoolRuneRetunesGauntletSpellPower(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCalibrationBenchSyncsGauntletEnchantments(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellCalibrationBenchSyncsGauntletEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void scrollcasterGauntletGrindstoneDoesNotExposeOutput(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.scrollcasterGauntletGrindstoneDoesNotExposeOutput(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void arcanumInAJarComparatorOutputMatchesStoredEssence(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.arcanumInAJarComparatorOutputMatchesStoredEssence(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void atelierStationComparatorOutputMatchesStoredPotionFluidAmount(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.atelierStationComparatorOutputMatchesStoredPotionFluidAmount(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void creativeTabSpellsStayGroupedBySchool(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.creativeTabSpellsStayGroupedBySchool(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void bonusChestLootIncludesIsekaiTravelGuidebook(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.bonusChestLootIncludesIsekaiTravelGuidebook(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void zenithStaffUsesStrongestSchoolPowerAndManaPenalty(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.zenithStaffUsesStrongestSchoolPowerAndManaPenalty(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void apprenticeCurioBonusLootTableContainsAllThreeItems(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.apprenticeCurioBonusLootTableContainsAllThreeItems(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void genericLootIncludesApprenticeCurioBonusDrops(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.genericLootIncludesApprenticeCurioBonusDrops(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void ironsStructureLootIncludesApprenticeCurioBonusDrops(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.ironsStructureLootIncludesApprenticeCurioBonusDrops(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void ominousVaultLootIncludesApprenticeCurioBonusDrops(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.ominousVaultLootIncludesApprenticeCurioBonusDrops(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void nonLootableApprenticeSpellsAreExcludedFromDefaultSpellFilter(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.nonLootableApprenticeSpellsAreExcludedFromDefaultSpellFilter(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void genericMagicTreasureLootDoesNotGenerateBlockedApprenticeScrolls(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.genericMagicTreasureLootDoesNotGenerateBlockedApprenticeScrolls(helper);
    }
}
