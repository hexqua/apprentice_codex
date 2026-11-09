package jp.aquafactory.apprenticecodex.gametest.world.errandmage;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.runtime.CoreGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void villagerCanClaimApprenticeDeskAsErrandMageJobSite(GameTestHelper helper) {
        ErrandMageScenarios.villagerCanClaimApprenticeDeskAsErrandMageJobSite(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void errandMageVillageHouseIsAddedToVanillaVillagePools(GameTestHelper helper) {
        ErrandMageScenarios.errandMageVillageHouseIsAddedToVanillaVillagePools(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void errandMageVillageHouseTemplatesAreLoadableAndKeepRequiredJigsaws(GameTestHelper helper) {
        ErrandMageScenarios.errandMageVillageHouseTemplatesAreLoadableAndKeepRequiredJigsaws(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void randomSpellImbueCreatesLockedLevelOneWand(GameTestHelper helper) {
        ErrandMageScenarios.randomSpellImbueCreatesLockedLevelOneWand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void randomSpellImbueUsesOnlyConfiguredEnabledSpells(GameTestHelper helper) {
        ErrandMageScenarios.randomSpellImbueUsesOnlyConfiguredEnabledSpells(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void randomSpellImbueFallsBackWhenNoSpellIsEligible(GameTestHelper helper) {
        ErrandMageScenarios.randomSpellImbueFallsBackWhenNoSpellIsEligible(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void randomSpellImbueFallsBackForUnknownSpell(GameTestHelper helper) {
        ErrandMageScenarios.randomSpellImbueFallsBackForUnknownSpell(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void errandMageOffersAcceptTaggedErrandMagePayments(GameTestHelper helper) {
        ErrandMageScenarios.errandMageOffersAcceptTaggedErrandMagePayments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void errandMageTradesMatchExpectedOffers(GameTestHelper helper) {
        ErrandMageScenarios.errandMageTradesMatchExpectedOffers(helper);
    }
}
