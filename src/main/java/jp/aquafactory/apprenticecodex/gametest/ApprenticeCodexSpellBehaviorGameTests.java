package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexSpellBehaviorGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String SENSE_EVIL_ISOLATED_BATCH = "apprenticecodex.sense_evil_isolated";
    private static final String HEALING_BLOOM_ISOLATED_BATCH = "apprenticecodex.healing_bloom_isolated";
    private static final String ARCHER_MULTIPLE_ISOLATED_BATCH = "apprenticecodex.archer_multiple_isolated";
    private static final String PERSONAL_SHELF_ISOLATED_BATCH = "apprenticecodex.personal_shelf_isolated";
    private static final String COMPANION_TRUNK_ISOLATED_BATCH = "apprenticecodex.companion_trunk_isolated";
    private static final String RIFT_HOLE_ISOLATED_BATCH = "apprenticecodex.rift_hole_isolated";
    private static final String DEMICREATOR_WINGS_ISOLATED_BATCH = "apprenticecodex.demicreator_wings_isolated";
    private static final String REMOTE_EYE_ISOLATED_BATCH = "apprenticecodex.remote_eye_isolated";
    private static final String MIRAGE_AVOIDANCE_ISOLATED_BATCH = "apprenticecodex.mirage_avoidance_isolated";
    private static final String HARVEST_MOON_ISOLATED_BATCH = "apprenticecodex.harvest_moon_isolated";
    private static final String AUTO_MAGNET_ISOLATED_BATCH = "apprenticecodex.auto_magnet_isolated";
    private static final String EARTH_FORGE_ISOLATED_BATCH = "apprenticecodex.earth_forge_isolated";
    private static final String COMPOUND_PHIAL_ISOLATED_BATCH = "apprenticecodex.compound_phial_isolated";
    private static final String ASSIST_WINGS_ISOLATED_BATCH = "apprenticecodex.assist_wings_isolated";
    private static final String MULTICAST_ECHO_STAFF_ISOLATED_BATCH = "apprenticecodex.multicast_echo_staff_isolated";
    private static final String MULTICAST_ECHO_STAFF_COOLDOWN_CAP_BATCH = "apprenticecodex.multicast_echo_staff_cooldown_cap";
    private static final String MULTICAST_ECHO_STAFF_BASE_COOLDOWN_CAP_BATCH = "apprenticecodex.multicast_echo_staff_base_cooldown_cap";
    private static final String ECHO_CAST_MULTICAST_LIMIT_BATCH = "apprenticecodex.echo_cast_multicast_limit";
    private static final String MYSTIC_SHIELD_ISOLATED_BATCH = "apprenticecodex.mystic_shield_isolated";
    private static final String MIST_FORM_ISOLATED_BATCH = "apprenticecodex.mist_form_isolated";
    private static final String COUNTERSPELL_COMPAT_ISOLATED_BATCH = "apprenticecodex.counterspell_compat_isolated";
    private static final String INSCRIBE_ICE_ISOLATED_BATCH = "apprenticecodex.inscribe_ice_isolated";
    private static final String HEAVENLY_FIST_ISOLATED_BATCH = "apprenticecodex.heavenly_fist_isolated";
    private static final String HEAVENLY_FIST_CREATE_PRESSING_DENYLIST_BATCH =
            "apprenticecodex.heavenly_fist_create_pressing_denylist";
    private static final String HEAVENLY_FIST_CREATE_COMPACTING_DENYLIST_BATCH =
            "apprenticecodex.heavenly_fist_create_compacting_denylist";
    private static final String GRIND_RUNNER_ISOLATED_BATCH = "apprenticecodex.grind_runner_isolated";

    private ApprenticeCodexSpellBehaviorGameTests() {
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
    public static void healingBloomDeathDropsOnlyStoredFruitWithoutPlantingBush(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.healingBloomDeathDropsOnlyStoredFruitWithoutPlantingBush(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEALING_BLOOM_ISOLATED_BATCH)
    public static void healingBloomImmediateDeathDropsNothingAndPlantsNoBush(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.healingBloomImmediateDeathDropsNothingAndPlantsNoBush(helper);
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
    public static void healingBloomOfflineDeathDoesNotBlockRecast(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.healingBloomOfflineDeathDoesNotBlockRecast(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEALING_BLOOM_ISOLATED_BATCH)
    public static void healingBloomSneakCastRecoversOfflineDeathState(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.healingBloomSneakCastRecoversOfflineDeathState(helper);
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

    @GameTest(template = TEMPLATE, batch = COMPANION_TRUNK_ISOLATED_BATCH, timeoutTicks = 120)
    public static void companionTrunkClimbsOneBlockStepWhenFollowingOwner(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.companionTrunkClimbsOneBlockStepWhenFollowingOwner(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMPANION_TRUNK_ISOLATED_BATCH, timeoutTicks = 80)
    public static void companionTrunkLandingDoesNotTrampleFarmland(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.companionTrunkLandingDoesNotTrampleFarmland(helper);
    }

    @GameTest(template = TEMPLATE, batch = RIFT_HOLE_ISOLATED_BATCH)
    public static void riftHoleDimensionDenylistRejectsCurrentDimension(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.riftHoleDimensionDenylistRejectsCurrentDimension(helper);
    }

    @GameTest(template = TEMPLATE, batch = RIFT_HOLE_ISOLATED_BATCH)
    public static void riftHoleDimensionAllowlistRequiresCurrentDimension(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.riftHoleDimensionAllowlistRequiresCurrentDimension(helper);
    }

    @GameTest(template = TEMPLATE, batch = RIFT_HOLE_ISOLATED_BATCH)
    public static void riftHoleDimensionDenylistOverridesAllowlist(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.riftHoleDimensionDenylistOverridesAllowlist(helper);
    }

    @GameTest(template = TEMPLATE, batch = DEMICREATOR_WINGS_ISOLATED_BATCH)
    public static void demicreatorWingsDimensionDenylistRejectsCurrentDimension(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.demicreatorWingsDimensionDenylistRejectsCurrentDimension(helper);
    }

    @GameTest(template = TEMPLATE, batch = DEMICREATOR_WINGS_ISOLATED_BATCH)
    public static void demicreatorWingsDimensionAllowlistRequiresCurrentDimension(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.demicreatorWingsDimensionAllowlistRequiresCurrentDimension(helper);
    }

    @GameTest(template = TEMPLATE, batch = DEMICREATOR_WINGS_ISOLATED_BATCH)
    public static void demicreatorWingsDimensionDenylistOverridesAllowlist(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.demicreatorWingsDimensionDenylistOverridesAllowlist(helper);
    }

    @GameTest(template = TEMPLATE, batch = DEMICREATOR_WINGS_ISOLATED_BATCH)
    public static void demicreatorWingsDimensionRestrictionAllowsCloseCast(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.demicreatorWingsDimensionRestrictionAllowsCloseCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = REMOTE_EYE_ISOLATED_BATCH)
    public static void remoteEyeDimensionDenylistRejectsCurrentDimension(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.remoteEyeDimensionDenylistRejectsCurrentDimension(helper);
    }

    @GameTest(template = TEMPLATE, batch = REMOTE_EYE_ISOLATED_BATCH)
    public static void remoteEyeDimensionAllowlistRequiresCurrentDimension(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.remoteEyeDimensionAllowlistRequiresCurrentDimension(helper);
    }

    @GameTest(template = TEMPLATE, batch = REMOTE_EYE_ISOLATED_BATCH)
    public static void remoteEyeDimensionDenylistOverridesAllowlist(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.remoteEyeDimensionDenylistOverridesAllowlist(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIRAGE_AVOIDANCE_ISOLATED_BATCH)
    public static void mirageAvoidanceUsesFifteenTickInvulnerabilityAndActiveRecastLock(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mirageAvoidanceUsesFifteenTickInvulnerabilityAndActiveRecastLock(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIRAGE_AVOIDANCE_ISOLATED_BATCH, timeoutTicks = 80)
    public static void mirageAvoidanceFreezesThenSlidesAndSuppressesFallDamage(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mirageAvoidanceFreezesThenSlidesAndSuppressesFallDamage(helper);
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

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void autoMagnetCollectsItemsWithoutSolegnoliaBlock(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autoMagnetCollectsItemsWithoutSolegnoliaBlock(helper);
    }

    @GameTest(template = TEMPLATE, batch = GRIND_RUNNER_ISOLATED_BATCH)
    public static void grindRunnerProcessesCreateCrushingWithoutCraftsmansDelight(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.grindRunnerProcessesCreateCrushingWithoutCraftsmansDelight(helper);
    }

    @GameTest(template = TEMPLATE, batch = GRIND_RUNNER_ISOLATED_BATCH)
    public static void grindRunnerProcessesCreateMillingRecipes(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.grindRunnerProcessesCreateMillingRecipes(helper);
    }

    @GameTest(template = TEMPLATE, batch = GRIND_RUNNER_ISOLATED_BATCH)
    public static void grindRunnerPrefersCreateCrushingBeforeMilling(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.grindRunnerPrefersCreateCrushingBeforeMilling(helper);
    }

    @GameTest(template = TEMPLATE, batch = GRIND_RUNNER_ISOLATED_BATCH)
    public static void grindRunnerProcessesCreateDepotItems(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.grindRunnerProcessesCreateDepotItems(helper);
    }

    @GameTest(template = TEMPLATE, batch = GRIND_RUNNER_ISOLATED_BATCH)
    public static void grindRunnerLeavesCreateChuteItemsUnprocessed(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.grindRunnerLeavesCreateChuteItemsUnprocessed(helper);
    }

    @GameTest(template = TEMPLATE, batch = EARTH_FORGE_ISOLATED_BATCH, timeoutTicks = 60)
    public static void earthForgeReplacesWaterButKeepsUnsafeFluidBlocks(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.earthForgeReplacesWaterButKeepsUnsafeFluidBlocks(helper);
    }

    @GameTest(template = TEMPLATE, batch = EARTH_FORGE_ISOLATED_BATCH)
    public static void blockToolsTemporaryUseKeepsOneCountStackAndHands(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.blockToolsTemporaryUseKeepsOneCountStackAndHands(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMPOUND_PHIAL_ISOLATED_BATCH, timeoutTicks = 40)
    public static void compoundPhialSplashDamageUsesWeakFalloffAndKeepsSelfHit(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.compoundPhialSplashDamageUsesWeakFalloffAndKeepsSelfHit(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsSmashcastGroundCastJumpsWithoutKeepingWing(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.assistWingsSmashcastGroundCastJumpsWithoutKeepingWing(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsSmashcastAirCastConsumesJumpAndDropsWing(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.assistWingsSmashcastAirCastConsumesJumpAndDropsWing(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsSmashcastExhaustedAirCastOnlyDropsWing(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.assistWingsSmashcastExhaustedAirCastOnlyDropsWing(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffInstantCastRunsAfterDelayAndAppliesPenaltyCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffInstantCastRunsAfterDelayAndAppliesPenaltyCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffInsufficientManaEndsWithPenaltyCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffInsufficientManaEndsWithPenaltyCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffItemChangeEndsWithPenaltyCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffItemChangeEndsWithPenaltyCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffLogoutEndsWithPenaltyCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffLogoutEndsWithPenaltyCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffLongCastAddsSkippedCastTimeCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffLongCastAddsSkippedCastTimeCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffInvalidInstantCastKeepsEchoSpell(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffInvalidInstantCastKeepsEchoSpell(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffInvalidLongCastIgnoresStaleEchoContext(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffInvalidLongCastIgnoresStaleEchoContext(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffLongCastUsesStartEchoContextAfterEffectRemoved(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffLongCastUsesStartEchoContextAfterEffectRemoved(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffRepeatedFortifyClearsTargetAreaIndicator(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffRepeatedFortifyClearsTargetAreaIndicator(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 40)
    public static void multicastEchoStaffMobEffectProfileExtendsDuplicateDuration(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffMobEffectProfileExtendsDuplicateDuration(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 40)
    public static void multicastEchoStaffMobEffectProfileStacksAmplifierByLevel(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffMobEffectProfileStacksAmplifierByLevel(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 40)
    public static void multicastEchoStaffMobEffectProfileIgnoresMissingProfile(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffMobEffectProfileIgnoresMissingProfile(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 40)
    public static void multicastEchoStaffAttackProfileScalesDirectCombatToolsDamage(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffAttackProfileScalesDirectCombatToolsDamage(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 40)
    public static void multicastEchoStaffAttackProfileTracksDelayedProjectileDamage(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffAttackProfileTracksDelayedProjectileDamage(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_COOLDOWN_CAP_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffCooldownCapLimitsAdjustedCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffCooldownCapLimitsAdjustedCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_BASE_COOLDOWN_CAP_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffBaseCooldownAboveCapUsesOriginalCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffBaseCooldownAboveCapUsesOriginalCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = ECHO_CAST_MULTICAST_LIMIT_BATCH, timeoutTicks = 40)
    public static void echoCastStopsAtConfiguredMulticastLimit(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.echoCastStopsAtConfiguredMulticastLimit(helper);
    }

    @GameTest(template = TEMPLATE, batch = MYSTIC_SHIELD_ISOLATED_BATCH, timeoutTicks = 40)
    public static void mysticShieldBlocksFrontDamageAndLimitsSameSourceAccumulation(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mysticShieldBlocksFrontDamageAndLimitsSameSourceAccumulation(helper);
    }

    @GameTest(template = TEMPLATE, batch = MYSTIC_SHIELD_ISOLATED_BATCH, timeoutTicks = 40)
    public static void mysticShieldReflectsStoredDamageAfterNonFrontCancel(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mysticShieldReflectsStoredDamageAfterNonFrontCancel(helper);
    }

    @GameTest(template = TEMPLATE, batch = MYSTIC_SHIELD_ISOLATED_BATCH)
    public static void mysticShieldUsesYawWhenLookPitchIsVertical(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mysticShieldUsesYawWhenLookPitchIsVertical(helper);
    }

    @GameTest(template = TEMPLATE, batch = MYSTIC_SHIELD_ISOLATED_BATCH)
    public static void mysticShieldReceivesProtectionSpellSupporterBenefits(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mysticShieldReceivesProtectionSpellSupporterBenefits(helper);
    }

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH)
    public static void counterspellCompatMagicMobEffectsAreMagicMobEffects(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.counterspellCompatMagicMobEffectsAreMagicMobEffects(helper);
    }

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH)
    public static void spectralWingEffectRemovalClearsFlightState(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spectralWingEffectRemovalClearsFlightState(helper);
    }

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH)
    public static void counterspellCompatMagicConstructsDeactivateSafely(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.counterspellCompatMagicConstructsDeactivateSafely(helper);
    }

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH, timeoutTicks = 40)
    public static void healingBloomAntiMagicUsesDeathCleanup(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.healingBloomAntiMagicUsesDeathCleanup(helper);
    }

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH, timeoutTicks = 40)
    public static void counterspellCompatProjectilesFizzleHarmlessly(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.counterspellCompatProjectilesFizzleHarmlessly(helper);
    }

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH, timeoutTicks = 40)
    public static void magicSpearAntiMagicBurstDoesNotRestart(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.magicSpearAntiMagicBurstDoesNotRestart(helper);
    }

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH, timeoutTicks = 40)
    public static void uniteLunaAntiMagicAmplifiesBurst(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.uniteLunaAntiMagicAmplifiesBurst(helper);
    }

    @GameTest(template = TEMPLATE, batch = COUNTERSPELL_COMPAT_ISOLATED_BATCH)
    public static void counterspellCompatSpecialPlayerTargetBehaviors(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.counterspellCompatSpecialPlayerTargetBehaviors(helper);
    }

    @GameTest(template = TEMPLATE, batch = INSCRIBE_ICE_ISOLATED_BATCH)
    public static void inscribeIceCastUsesShortThrowJob(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.inscribeIceCastUsesShortThrowJob(helper);
    }

    @GameTest(template = TEMPLATE, batch = INSCRIBE_ICE_ISOLATED_BATCH)
    public static void notchedFrozenStacksAndBurstsOnThirdStack(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.notchedFrozenStacksAndBurstsOnThirdStack(helper);
    }

    @GameTest(template = TEMPLATE, batch = INSCRIBE_ICE_ISOLATED_BATCH)
    public static void notchedFrozenMaintainsExistingFreezeWithoutIceWeakness(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.notchedFrozenMaintainsExistingFreezeWithoutIceWeakness(helper);
    }

    @GameTest(template = TEMPLATE, batch = INSCRIBE_ICE_ISOLATED_BATCH)
    public static void inscribeIceBurstUsesHalfDamageForChainedBurstsAndSkipsPlayers(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.inscribeIceBurstUsesHalfDamageForChainedBurstsAndSkipsPlayers(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistImpactAabbAppliesDamageAndGravityBound(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.heavenlyFistImpactAabbAppliesDamageAndGravityBound(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistImpactDamagesNonLivingCombatTarget(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.heavenlyFistImpactDamagesNonLivingCombatTarget(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistImpactDoesNotTrackMovedTarget(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.heavenlyFistImpactDoesNotTrackMovedTarget(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistProcessesCreateDepotItems(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.heavenlyFistProcessesCreateDepotItems(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistLeavesDroppedCreateItemsOutsideProcessArea(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.heavenlyFistLeavesDroppedCreateItemsOutsideProcessArea(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_CREATE_PRESSING_DENYLIST_BATCH, timeoutTicks = 60)
    public static void heavenlyFistCreatePressingDenylistLeavesDepotItems(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.heavenlyFistCreatePressingDenylistLeavesDepotItems(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistProcessesCreateBasinCompacting(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.heavenlyFistProcessesCreateBasinCompacting(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistCreateBasinCompactingConsumesOneBudgetPerRecipe(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.heavenlyFistCreateBasinCompactingConsumesOneBudgetPerRecipe(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_CREATE_COMPACTING_DENYLIST_BATCH, timeoutTicks = 60)
    public static void heavenlyFistCreateCompactingDenylistLeavesBasinItems(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.heavenlyFistCreateCompactingDenylistLeavesBasinItems(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 60)
    public static void heavenlyFistSkipsCreateBasinCompressionCrafting(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.heavenlyFistSkipsCreateBasinCompressionCrafting(helper);
    }

    @GameTest(template = TEMPLATE, batch = HEAVENLY_FIST_ISOLATED_BATCH, timeoutTicks = 40)
    public static void gravityBoundPullsAirborneTargetsDown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.gravityBoundPullsAirborneTargetsDown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormAppliesEffectAndFixedAttributes(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mistFormAppliesEffectAndFixedAttributes(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormSuppressesAwarenessWithinThirtyTwoBlocks(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mistFormSuppressesAwarenessWithinThirtyTwoBlocks(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormDamageToLivingTargetRemovesEffect(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mistFormDamageToLivingTargetRemovesEffect(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormSlowsFallingWithoutAmplifierScaling(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mistFormSlowsFallingWithoutAmplifierScaling(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormStandsOnLiquidAndSneakSinks(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mistFormStandsOnLiquidAndSneakSinks(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormPassesTaggedBlocksAndRejectsGlass(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mistFormPassesTaggedBlocksAndRejectsGlass(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormPassableBlockDenylistBlocksIdsAndTags(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mistFormPassableBlockDenylistBlocksIdsAndTags(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormWaterloggedPassableBlockDoesNotSnapUp(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mistFormWaterloggedPassableBlockDoesNotSnapUp(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormIgnoresTaggedMovementRestrictions(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mistFormIgnoresTaggedMovementRestrictions(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormMovementRestrictionIgnoreKeepsBlockEffects(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mistFormMovementRestrictionIgnoreKeepsBlockEffects(helper);
    }
}
