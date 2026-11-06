package jp.aquafactory.apprenticecodex.gametest.spell.searchbeacon;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void searchBeaconRefundLogicOnlyRefundsWhenUnknownStructuresAreAbsent(GameTestHelper helper) {
        SearchBeaconScenarios.searchBeaconRefundLogicOnlyRefundsWhenUnknownStructuresAreAbsent(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void instantSearchBrazierUseCreatesConfiguredSingleOfferBeaconAndRefundsBeforeSearch(
            GameTestHelper helper
    ) {
        InstantSearchBrazierGameTestScenarios
                .useCreatesConfiguredSingleOfferBeaconAndRefundsBeforeSearch(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void instantSearchBrazierSearchStartStopsBrazierRefundAndRejectsAdditionalOffer(
            GameTestHelper helper
    ) {
        InstantSearchBrazierGameTestScenarios
                .searchStartStopsBrazierRefundAndRejectsAdditionalOffer(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void instantSearchBrazierPendingRefundSurvivesSpellDataRoundTrip(GameTestHelper helper) {
        InstantSearchBrazierGameTestScenarios
                .pendingBrazierSurvivesSpellDataRoundTripAndRecoversOnce(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void instantSearchBrazierUnloadReturnsBrazierToOwnerInventory(GameTestHelper helper) {
        InstantSearchBrazierGameTestScenarios.unloadingBeaconReturnsBrazierToOwnerInventory(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void instantSearchBrazierCancelDoesNotResetSpellCooldown(GameTestHelper helper) {
        InstantSearchBrazierGameTestScenarios.cancelOnlyResetsCooldownForSpellSummonedBeacon(helper);
    }
}
