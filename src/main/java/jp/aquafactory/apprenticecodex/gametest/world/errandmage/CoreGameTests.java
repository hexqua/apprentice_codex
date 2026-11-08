package jp.aquafactory.apprenticecodex.gametest.world.errandmage;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = "gametest/basic_floor", timeoutTicks = 400)
    public static void villagerCanClaimApprenticeDeskAsErrandMageJobSite(GameTestHelper helper) {
        ErrandMageScenarios.villagerCanClaimApprenticeDeskAsErrandMageJobSite(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void errandMageVillageHouseIsAddedToVanillaVillagePools(GameTestHelper helper) {
        ErrandMageScenarios.errandMageVillageHouseIsAddedToVanillaVillagePools(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void errandMageVillageHouseTemplatesAreLoadableAndKeepRequiredJigsaws(GameTestHelper helper) {
        ErrandMageScenarios.errandMageVillageHouseTemplatesAreLoadableAndKeepRequiredJigsaws(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void randomSpellImbueCreatesLockedLevelOneWand(GameTestHelper helper) {
        ErrandMageScenarios.randomSpellImbueCreatesLockedLevelOneWand(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void randomSpellImbueUsesOnlyConfiguredEnabledSpells(GameTestHelper helper) {
        ErrandMageScenarios.randomSpellImbueUsesOnlyConfiguredEnabledSpells(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void randomSpellImbueFallsBackWhenNoSpellIsEligible(GameTestHelper helper) {
        ErrandMageScenarios.randomSpellImbueFallsBackWhenNoSpellIsEligible(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void randomSpellImbueFallsBackForUnknownSpell(GameTestHelper helper) {
        ErrandMageScenarios.randomSpellImbueFallsBackForUnknownSpell(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void errandMageOffersAcceptTaggedErrandMagePayments(GameTestHelper helper) {
        ErrandMageScenarios.errandMageOffersAcceptTaggedErrandMagePayments(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void errandMageComfortBerriesPaymentsSurviveSerialization(GameTestHelper helper) {
        ErrandMageScenarios.errandMageComfortBerriesPaymentsSurviveSerialization(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void errandMageTradesMatchExpectedOffers(GameTestHelper helper) {
        ErrandMageScenarios.errandMageTradesMatchExpectedOffers(helper);
    }
}
