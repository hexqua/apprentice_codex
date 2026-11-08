package jp.aquafactory.apprenticecodex.gametest.storage.companiontrunk;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.COMPANION_TRUNK_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = COMPANION_TRUNK_ISOLATED_BATCH)
    public static void companionTrunkRecastRecallsLoadedTrunkWhenFar(GameTestHelper helper) {
        CompanionTrunkScenarios.companionTrunkRecastRecallsLoadedTrunkWhenFar(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMPANION_TRUNK_ISOLATED_BATCH)
    public static void companionTrunkRecastKeepsLoadedTrunkInPlaceWhenNear(GameTestHelper helper) {
        CompanionTrunkScenarios.companionTrunkRecastKeepsLoadedTrunkInPlaceWhenNear(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMPANION_TRUNK_ISOLATED_BATCH)
    public static void companionTrunkDeathStoresItemsInChestWhenSpaceExists(GameTestHelper helper) {
        CompanionTrunkScenarios.companionTrunkDeathStoresItemsInChestWhenSpaceExists(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMPANION_TRUNK_ISOLATED_BATCH)
    public static void companionTrunkDeathDropsItemsWhenNoChestSpaceExists(GameTestHelper helper) {
        CompanionTrunkScenarios.companionTrunkDeathDropsItemsWhenNoChestSpaceExists(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMPANION_TRUNK_ISOLATED_BATCH, timeoutTicks = 80)
    public static void companionTrunkIgnoresFireAndRescuesFromVoid(GameTestHelper helper) {
        CompanionTrunkScenarios.companionTrunkIgnoresFireAndRescuesFromVoid(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMPANION_TRUNK_ISOLATED_BATCH, timeoutTicks = 120)
    public static void companionTrunkClimbsOneBlockStepWhenFollowingOwner(GameTestHelper helper) {
        CompanionTrunkScenarios.companionTrunkClimbsOneBlockStepWhenFollowingOwner(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMPANION_TRUNK_ISOLATED_BATCH, timeoutTicks = 80)
    public static void companionTrunkLandingDoesNotTrampleFarmland(GameTestHelper helper) {
        CompanionTrunkScenarios.companionTrunkLandingDoesNotTrampleFarmland(helper);
    }
}
