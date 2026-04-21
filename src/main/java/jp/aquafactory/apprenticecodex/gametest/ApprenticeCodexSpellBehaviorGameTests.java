package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexSpellBehaviorGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String SENSE_EVIL_ISOLATED_BATCH = "apprenticecodex.sense_evil_isolated";
    private static final String HEALING_BLOOM_ISOLATED_BATCH = "apprenticecodex.healing_bloom_isolated";
    private static final String ARCHER_MULTIPLE_ISOLATED_BATCH = "apprenticecodex.archer_multiple_isolated";
    private static final String PERSONAL_SHELF_ISOLATED_BATCH = "apprenticecodex.personal_shelf_isolated";
    private static final String COMPANION_TRUNK_ISOLATED_BATCH = "apprenticecodex.companion_trunk_isolated";
    private static final String HARVEST_MOON_ISOLATED_BATCH = "apprenticecodex.harvest_moon_isolated";

    private ApprenticeCodexSpellBehaviorGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = SENSE_EVIL_ISOLATED_BATCH)
    public static void senseEvilExpandsHorizontalReachToCube(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.senseEvilExpandsHorizontalReachToCube(helper);
    }

    @GameTest(template = TEMPLATE, batch = SENSE_EVIL_ISOLATED_BATCH)
    public static void senseEvilUsesSameCubeForSpawnersAndEntities(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.senseEvilUsesSameCubeForSpawnersAndEntities(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEALING_BLOOM_ISOLATED_BATCH)
    public static void healingBloomLightHasReducedLevelAndNoOutline(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.healingBloomLightHasReducedLevelAndNoOutline(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEALING_BLOOM_ISOLATED_BATCH)
    public static void healingBloomLightSelfCleansWithoutBloom(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.healingBloomLightSelfCleansWithoutBloom(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEALING_BLOOM_ISOLATED_BATCH)
    public static void healingBloomAcceptsOwnerDamageAndStaysSavable(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.healingBloomAcceptsOwnerDamageAndStaysSavable(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEALING_BLOOM_ISOLATED_BATCH)
    public static void healingBloomRootLossUsesDeathState(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.healingBloomRootLossUsesDeathState(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEALING_BLOOM_ISOLATED_BATCH)
    public static void healingBloomSkipsSelfRegenerationAndUsesSlowNaturalHealing(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.healingBloomSkipsSelfRegenerationAndUsesSlowNaturalHealing(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEALING_BLOOM_ISOLATED_BATCH)
    public static void healingBloomCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.healingBloomCanBePlacedOnSupportedSlab(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEALING_BLOOM_ISOLATED_BATCH)
    public static void healingBloomNormalRecastFailsForSameOwner(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.healingBloomNormalRecastFailsForSameOwner(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEALING_BLOOM_ISOLATED_BATCH)
    public static void healingBloomAllowsDifferentOwnersToEachHaveOne(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.healingBloomAllowsDifferentOwnersToEachHaveOne(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEALING_BLOOM_ISOLATED_BATCH)
    public static void healingBloomMissingManagedBloomDoesNotBlockRecast(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.healingBloomMissingManagedBloomDoesNotBlockRecast(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEALING_BLOOM_ISOLATED_BATCH)
    public static void healingBloomSneakCastReplacesOnlyOwnersPreviousBloom(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.healingBloomSneakCastReplacesOnlyOwnersPreviousBloom(helper);
    }

    @GameTest(template = TEMPLATE, batch = ARCHER_MULTIPLE_ISOLATED_BATCH)
    public static void archerMultipleTimeoutWithGreaterConjurersTalismanSkipsCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.archerMultipleTimeoutWithGreaterConjurersTalismanSkipsCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = ARCHER_MULTIPLE_ISOLATED_BATCH, timeoutTicks = 60)
    public static void archerMultipleAllBowRemovalEndsRecastAndStartsCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.archerMultipleAllBowRemovalEndsRecastAndStartsCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = PERSONAL_SHELF_ISOLATED_BATCH)
    public static void personalShelfOpensVanillaChestMenuAndHandlesFullQuickMove(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.personalShelfOpensVanillaChestMenuAndHandlesFullQuickMove(helper);
    }

    @GameTest(template = TEMPLATE, batch = PERSONAL_SHELF_ISOLATED_BATCH, timeoutTicks = 60)
    public static void personalShelfExpireClosesOpenedChestMenu(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.personalShelfExpireClosesOpenedChestMenu(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMPANION_TRUNK_ISOLATED_BATCH)
    public static void companionTrunkRecastRecallsLoadedTrunkWhenFar(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.companionTrunkRecastRecallsLoadedTrunkWhenFar(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMPANION_TRUNK_ISOLATED_BATCH)
    public static void companionTrunkRecastKeepsLoadedTrunkInPlaceWhenNear(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.companionTrunkRecastKeepsLoadedTrunkInPlaceWhenNear(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMPANION_TRUNK_ISOLATED_BATCH)
    public static void companionTrunkDeathStoresItemsInChestWhenSpaceExists(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.companionTrunkDeathStoresItemsInChestWhenSpaceExists(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMPANION_TRUNK_ISOLATED_BATCH)
    public static void companionTrunkDeathDropsItemsWhenNoChestSpaceExists(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.companionTrunkDeathDropsItemsWhenNoChestSpaceExists(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMPANION_TRUNK_ISOLATED_BATCH, timeoutTicks = 80)
    public static void companionTrunkIgnoresFireAndRescuesFromVoid(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.companionTrunkIgnoresFireAndRescuesFromVoid(helper);
    }

    @GameTest(template = TEMPLATE, batch = HARVEST_MOON_ISOLATED_BATCH)
    public static void harvestMoonResetsMatureNetherWartAndPullsDrops(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.harvestMoonResetsMatureNetherWartAndPullsDrops(helper);
    }

    @GameTest(template = TEMPLATE, batch = HARVEST_MOON_ISOLATED_BATCH)
    public static void harvestMoonHarvestsFarmersDelightTomatoViaRightClick(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.harvestMoonHarvestsFarmersDelightTomatoViaRightClick(helper);
    }

    @GameTest(template = TEMPLATE, batch = HARVEST_MOON_ISOLATED_BATCH)
    public static void harvestMoonKeepsFarmersDelightTomatoRopeState(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.harvestMoonKeepsFarmersDelightTomatoRopeState(helper);
    }

    @GameTest(template = TEMPLATE, batch = HARVEST_MOON_ISOLATED_BATCH)
    public static void harvestMoonHarvestsStemFruitWithoutBreakingStem(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.harvestMoonHarvestsStemFruitWithoutBreakingStem(helper);
    }

    @GameTest(template = TEMPLATE, batch = HARVEST_MOON_ISOLATED_BATCH, timeoutTicks = 120)
    public static void harvestMoonProcessesTargetsAcrossMultipleTicksAndKeepsBambooRoot(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.harvestMoonProcessesTargetsAcrossMultipleTicksAndKeepsBambooRoot(helper);
    }

    @GameTest(template = TEMPLATE, batch = HARVEST_MOON_ISOLATED_BATCH)
    public static void harvestMoonHarvestsKelpColumnBeyondInitialYSlice(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.harvestMoonHarvestsKelpColumnBeyondInitialYSlice(helper);
    }
}
