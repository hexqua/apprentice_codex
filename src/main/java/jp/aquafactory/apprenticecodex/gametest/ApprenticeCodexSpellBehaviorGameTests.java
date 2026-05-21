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
    private static final String HARVEST_MOON_ISOLATED_BATCH = "apprenticecodex.harvest_moon_isolated";
    private static final String AUTO_MAGNET_ISOLATED_BATCH = "apprenticecodex.auto_magnet_isolated";
    private static final String EARTH_FORGE_ISOLATED_BATCH = "apprenticecodex.earth_forge_isolated";
    private static final String COMPOUND_PHIAL_ISOLATED_BATCH = "apprenticecodex.compound_phial_isolated";
    private static final String ASSIST_WINGS_ISOLATED_BATCH = "apprenticecodex.assist_wings_isolated";
    private static final String MULTICAST_ECHO_STAFF_ISOLATED_BATCH = "apprenticecodex.multicast_echo_staff_isolated";
    private static final String MULTICAST_ECHO_STAFF_COOLDOWN_CAP_BATCH = "apprenticecodex.multicast_echo_staff_cooldown_cap";
    private static final String MULTICAST_ECHO_STAFF_BASE_COOLDOWN_CAP_BATCH = "apprenticecodex.multicast_echo_staff_base_cooldown_cap";
    private static final String ECHO_CAST_MULTICAST_LIMIT_BATCH = "apprenticecodex.echo_cast_multicast_limit";

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

    @GameTest(template = TEMPLATE, batch = EARTH_FORGE_ISOLATED_BATCH, timeoutTicks = 60)
    public static void earthForgeReplacesWaterButKeepsUnsafeFluidBlocks(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.earthForgeReplacesWaterButKeepsUnsafeFluidBlocks(helper);
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
}
