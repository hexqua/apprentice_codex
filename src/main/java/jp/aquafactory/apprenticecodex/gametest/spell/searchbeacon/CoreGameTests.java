package jp.aquafactory.apprenticecodex.gametest.spell.searchbeacon;

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

    @GameTest(template = TEMPLATE)
    public static void searchBeaconRefundLogicOnlyRefundsWhenUnknownStructuresAreAbsent(GameTestHelper helper) {
        SearchBeaconScenarios.searchBeaconRefundLogicOnlyRefundsWhenUnknownStructuresAreAbsent(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void instantSearchBrazierUseCreatesConfiguredSingleOfferBeaconAndRefundsBeforeSearch(
            GameTestHelper helper
    ) {
        InstantSearchBrazierGameTestScenarios.useCreatesConfiguredSingleOfferBeaconAndRefundsBeforeSearch(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void instantSearchBrazierSearchStartStopsBrazierRefundAndRejectsAdditionalOffer(
            GameTestHelper helper
    ) {
        InstantSearchBrazierGameTestScenarios.searchStartStopsBrazierRefundAndRejectsAdditionalOffer(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void instantSearchBrazierPendingRefundSurvivesSpellDataRoundTrip(GameTestHelper helper) {
        InstantSearchBrazierGameTestScenarios.pendingBrazierSurvivesSpellDataRoundTripAndRecoversOnce(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void instantSearchBrazierUnloadReturnsBrazierToOwnerInventory(GameTestHelper helper) {
        InstantSearchBrazierGameTestScenarios.unloadingBeaconReturnsBrazierToOwnerInventory(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void instantSearchBrazierCancelDoesNotResetSpellCooldown(GameTestHelper helper) {
        InstantSearchBrazierGameTestScenarios.cancelOnlyResetsCooldownForSpellSummonedBeacon(helper);
    }
}
