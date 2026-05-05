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

    private ApprenticeCodexCoreGameTests() {
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
    public static void comfortBerriesCanBePottedAsDecoration(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.comfortBerriesCanBePottedAsDecoration(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void searchBeaconRefundLogicOnlyRefundsWhenUnknownStructuresAreAbsent(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.searchBeaconRefundLogicOnlyRefundsWhenUnknownStructuresAreAbsent(helper);
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
    public static void serverBlocksAndEntitiesCanBeInstantiated(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.serverBlocksAndEntitiesCanBeInstantiated(helper);
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
    public static void catacombsLootIncludesScarletThirstBonusDrop(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.catacombsLootIncludesScarletThirstBonusDrop(helper);
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
