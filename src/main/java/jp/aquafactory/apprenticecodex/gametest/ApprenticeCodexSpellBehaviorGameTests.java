package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
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
    private static final String RIFT_HOLE_ISOLATED_BATCH = "apprenticecodex.rift_hole_isolated";
    private static final String DEMICREATOR_WINGS_ISOLATED_BATCH = "apprenticecodex.demicreator_wings_isolated";
    private static final String REMOTE_EYE_ISOLATED_BATCH = "apprenticecodex.remote_eye_isolated";
    private static final String MIRAGE_AVOIDANCE_ISOLATED_BATCH = "apprenticecodex.mirage_avoidance_isolated";
    private static final String HARVEST_MOON_ISOLATED_BATCH = "apprenticecodex.harvest_moon_isolated";
    private static final String AUTO_TURRET_ISOLATED_BATCH = "apprenticecodex.auto_turret_isolated";
    private static final String FIELD_OVERSEER_ISOLATED_BATCH = "apprenticecodex.field_overseer_isolated";
    private static final String FIELD_OVERSEER_LIFECYCLE_BATCH = "apprenticecodex.field_overseer_lifecycle";
    private static final String TOTEM_OF_PERMAFROST_ISOLATED_BATCH = "apprenticecodex.totem_of_permafrost_isolated";
    private static final String TOTEM_OF_PERMAFROST_PULSE_BATCH = "apprenticecodex.totem_of_permafrost_pulse";
    private static final String AUTO_MAGNET_ISOLATED_BATCH = "apprenticecodex.auto_magnet_isolated";
    private static final String EARTH_FORGE_ISOLATED_BATCH = "apprenticecodex.earth_forge_isolated";
    private static final String LINEAR_BUILD_ISOLATED_BATCH = "apprenticecodex.linear_build_isolated";
    private static final String COMPOUND_PHIAL_ISOLATED_BATCH = "apprenticecodex.compound_phial_isolated";
    private static final String ASSIST_WINGS_ISOLATED_BATCH = "apprenticecodex.assist_wings_isolated";
    private static final String MYSTIC_SHIELD_ISOLATED_BATCH = "apprenticecodex.mystic_shield_isolated";
    private static final String MULTICAST_ECHO_STAFF_ISOLATED_BATCH = "apprenticecodex.multicast_echo_staff_isolated";
    private static final String MULTICAST_ECHO_STAFF_COOLDOWN_CAP_BATCH = "apprenticecodex.multicast_echo_staff_cooldown_cap";
    private static final String MULTICAST_ECHO_STAFF_BASE_COOLDOWN_CAP_BATCH = "apprenticecodex.multicast_echo_staff_base_cooldown_cap";
    private static final String ECHO_CAST_MULTICAST_LIMIT_BATCH = "apprenticecodex.echo_cast_multicast_limit";
    private static final String MIST_FORM_ISOLATED_BATCH = "apprenticecodex.mist_form_isolated";
    private static final String COUNTERSPELL_COMPAT_ISOLATED_BATCH = "apprenticecodex.counterspell_compat_isolated";
    private static final String MOON_LIGHT_COUNTERSPELL_ISOLATED_BATCH =
            "apprenticecodex.moon_light_counterspell_isolated";
    private static final String INSCRIBE_ICE_ISOLATED_BATCH = "apprenticecodex.inscribe_ice_isolated";
    private static final String STRAIGHT_PROJECTILE_COLLISION_ISOLATED_BATCH =
            "apprenticecodex.straight_projectile_collision_isolated";
    private static final String DUAL_ACROBAT_ISOLATED_BATCH = "apprenticecodex.dual_acrobat_isolated";
    private static final String BULLET_STREAM_ISOLATED_BATCH = "apprenticecodex.bullet_stream_isolated";
    private static final String HEAVENLY_FIST_ISOLATED_BATCH = "apprenticecodex.heavenly_fist_isolated";
    private static final String HEAVENLY_FIST_CREATE_PRESSING_DENYLIST_BATCH =
            "apprenticecodex.heavenly_fist_create_pressing_denylist";
    private static final String HEAVENLY_FIST_CREATE_COMPACTING_DENYLIST_BATCH =
            "apprenticecodex.heavenly_fist_create_compacting_denylist";
    private static final String GRIND_RUNNER_ISOLATED_BATCH = "apprenticecodex.grind_runner_isolated";
    private static final String BEAM_OCCLUSION_ISOLATED_BATCH = "apprenticecodex.beam_occlusion_isolated";
    private static final String SUMMON_WEAPON_ANIMATION_BATCH = "apprenticecodex.summon_weapon_animation";
    private static final String COMBAT_TARGET_POLICY_BATCH = "apprenticecodex.combat_target_policy";
    private static final String HIGANBANA_ISOLATED_BATCH = "apprenticecodex.higanbana_isolated";
    private static final String KATANA_AREA_HIT_ISOLATED_BATCH = "apprenticecodex.katana_area_hit_isolated";
    private static final String FUJIN_ISOLATED_BATCH = "apprenticecodex.fujin_isolated";
    private static final String OTHERWORLD_LENS_ISOLATED_BATCH = "apprenticecodex.otherworld_lens_isolated";

    private ApprenticeCodexSpellBehaviorGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = OTHERWORLD_LENS_ISOLATED_BATCH)
    public static void otherworldLensKeepsOcclusionWithoutEntityCollision(GameTestHelper helper) {
        var pos = new BlockPos(0, 2, 0);
        var state = BlockRegistry.OTHERWORLD_LENS_LENS.get().defaultBlockState();
        helper.assertTrue(state.canOcclude(), "OtherworldLens lens must remain an occluding block");
        helper.assertTrue(
                Block.isShapeFullBlock(state.getOcclusionShape(helper.getLevel(), helper.absolutePos(pos))),
                "OtherworldLens lens must keep a full-cube occlusion shape"
        );
        helper.assertTrue(
                state.getCollisionShape(helper.getLevel(), helper.absolutePos(pos), CollisionContext.empty()).isEmpty(),
                "OtherworldLens lens must not collide with entities"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = OTHERWORLD_LENS_ISOLATED_BATCH, timeoutTicks = 40)
    public static void otherworldLensOrphanSelfCleans(GameTestHelper helper) {
        var relativePos = new BlockPos(0, 2, 0);
        var absolutePos = helper.absolutePos(relativePos);
        helper.getLevel().setBlockAndUpdate(absolutePos, BlockRegistry.OTHERWORLD_LENS_LENS.get().defaultBlockState());
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(absolutePos).isAir(),
                    "OtherworldLens orphan lens should remove itself");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = OTHERWORLD_LENS_ISOLATED_BATCH, timeoutTicks = 40)
    public static void otherworldLensOrphanTickKeepsReplacement(GameTestHelper helper) {
        var relativePos = new BlockPos(0, 2, 0);
        var absolutePos = helper.absolutePos(relativePos);
        helper.getLevel().setBlockAndUpdate(absolutePos, BlockRegistry.OTHERWORLD_LENS_LENS.get().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(absolutePos, Blocks.STONE.defaultBlockState());
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(absolutePos).is(Blocks.STONE),
                    "OtherworldLens cleanup must not remove a replacement block");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = HIGANBANA_ISOLATED_BATCH)
    public static void higanbanaHasNoRecastAndKeepsSlashCounts(GameTestHelper helper) {
        HiganbanaGameTestScenarios.higanbanaHasNoRecastAndKeepsSlashCounts(helper);
    }

    @GameTest(template = TEMPLATE, batch = HIGANBANA_ISOLATED_BATCH)
    public static void higanbanaAutomaticallySlashesWithoutFollowingOwner(GameTestHelper helper) {
        HiganbanaGameTestScenarios.higanbanaAutomaticallySlashesWithoutFollowingOwner(helper);
    }

    @GameTest(template = TEMPLATE, batch = HIGANBANA_ISOLATED_BATCH)
    public static void higanbanaDamageHasNoKnockbackAndHealsHalf(GameTestHelper helper) {
        HiganbanaGameTestScenarios.higanbanaDamageHasNoKnockbackAndHealsHalf(helper);
    }

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void horizontalOrientedBoxRejectsBroadPhaseCorners(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.horizontalOrientedBoxRejectsBroadPhaseCorners(helper);
    }

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void orientedBoxOcclusionUsesPartialVisibilityAndEmbeddedSource(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.orientedBoxOcclusionUsesPartialVisibilityAndEmbeddedSource(helper);
    }

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void orientedBoxDeduplicatesMultipartTargets(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.orientedBoxDeduplicatesMultipartTargets(helper);
    }

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void slashBladeRefreshesPoseAndDamagesThroughWall(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.slashBladeRefreshesPoseAndDamagesThroughWall(helper);
    }

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void slashBladeKeepsFullDamageAndKnockbackWithoutWall(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.slashBladeKeepsFullDamageAndKnockbackWithoutWall(helper);
    }

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void higanbanaStaysHorizontalAndRejectsWallHits(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.higanbanaStaysHorizontalAndRejectsWallHits(helper);
    }

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void katanasKeepAttackOriginBeforeThinCover(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.katanasKeepAttackOriginBeforeThinCover(helper);
    }

    @GameTest(template = TEMPLATE, batch = FUJIN_ISOLATED_BATCH)
    public static void fujinKatanaFollowsOwnerAndReleases(GameTestHelper helper) {
        FujinGameTestScenarios.fujinKatanaFollowsOwnerAndReleases(helper);
    }

    @GameTest(template = TEMPLATE, batch = FUJIN_ISOLATED_BATCH)
    public static void fujinSlashPiercesAndDamagesEachTargetOnceWithoutKnockback(GameTestHelper helper) {
        FujinGameTestScenarios.fujinSlashPiercesAndDamagesEachTargetOnceWithoutKnockback(helper);
    }

    @GameTest(template = TEMPLATE, batch = FUJIN_ISOLATED_BATCH)
    public static void fujinSlashUsesSmallBlockCollision(GameTestHelper helper) {
        FujinGameTestScenarios.fujinSlashUsesSmallBlockCollision(helper);
    }

    @GameTest(template = TEMPLATE, batch = FUJIN_ISOLATED_BATCH)
    public static void fujinSlashDamagesTargetBeforeWall(GameTestHelper helper) {
        FujinGameTestScenarios.fujinSlashDamagesTargetBeforeWall(helper);
    }

    @GameTest(template = TEMPLATE, batch = FUJIN_ISOLATED_BATCH)
    public static void fujinSlashRespectsParallelWallOcclusion(GameTestHelper helper) {
        FujinGameTestScenarios.fujinSlashRespectsParallelWallOcclusion(helper);
    }

    @GameTest(template = TEMPLATE, batch = FUJIN_ISOLATED_BATCH)
    public static void fujinSlashExpiresBeyondRangeAndSupportsAntiMagic(GameTestHelper helper) {
        FujinGameTestScenarios.fujinSlashExpiresBeyondRangeAndSupportsAntiMagic(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMBAT_TARGET_POLICY_BATCH)
    public static void combatTargetPolicySeparatesSelfDamageFromAllyProtection(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.combatTargetPolicySeparatesSelfDamageFromAllyProtection(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMBAT_TARGET_POLICY_BATCH)
    public static void combatTargetPolicyRespectsTeamFriendlyFireAndPvp(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.combatTargetPolicyRespectsTeamFriendlyFireAndPvp(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMBAT_TARGET_POLICY_BATCH)
    public static void combatTargetPolicyProtectsWholeVehicleAndOwnedEntities(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.combatTargetPolicyProtectsWholeVehicleAndOwnedEntities(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void ownSpellsUniqueInfoAcceptsNullCaster(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.ownSpellsUniqueInfoAcceptsNullCaster(helper);
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

    @GameTest(template = TEMPLATE, batch = PERSONAL_SHELF_ISOLATED_BATCH)
    public static void personalShelfSynchronizesExportModeBlockState(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.personalShelfSynchronizesExportModeBlockState(helper);
    }

    @GameTest(template = TEMPLATE, batch = PERSONAL_SHELF_ISOLATED_BATCH, timeoutTicks = 60)
    public static void personalShelfExpireClosesOpenedChestMenu(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.personalShelfExpireClosesOpenedChestMenu(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void autoMagnetCollectsItemsWithoutSolegnoliaBlock(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autoMagnetCollectsItemsWithoutSolegnoliaBlock(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void autoMagnetFamiliarUsesTransientAlwaysTickingLifecycle(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autoMagnetFamiliarUsesTransientAlwaysTickingLifecycle(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void magneticStabilityAnchorProtectsItemsByPositionWithoutBlockingOwner(GameTestHelper helper) {
        MagneticStabilityAnchorGameTestScenarios.protectsItemsByPositionWithoutBlockingOwner(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void magneticStabilityAnchorRemovalRestoresCollectionAndExperienceRemainsUnaffected(GameTestHelper helper) {
        MagneticStabilityAnchorGameTestScenarios.removalRestoresItemCollectionAndExperienceRemainsUnaffected(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void magneticStabilityAnchorSupportsWaterloggingAndAlwaysDropsPlainItem(GameTestHelper helper) {
        MagneticStabilityAnchorGameTestScenarios.supportsWaterloggingAndAlwaysDropsPlainItem(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void autoMagnetNormalModeCollectsWhileStanding(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autoMagnetNormalModeCollectsWhileStanding(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void autoMagnetNormalModeStopsWhileCrouching(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autoMagnetNormalModeStopsWhileCrouching(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void autoMagnetReverseModeStopsWhileStanding(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autoMagnetReverseModeStopsWhileStanding(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void autoMagnetReverseModeCollectsWhileCrouching(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autoMagnetReverseModeCollectsWhileCrouching(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void autoMagnetRecastSwitchesModeAndStopsSameMode(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autoMagnetRecastSwitchesModeAndStopsSameMode(helper);
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

    @GameTest(template = TEMPLATE, batch = REMOTE_EYE_ISOLATED_BATCH)
    public static void remoteEyeSanitizerKeepsStoredLongDuration(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.remoteEyeSanitizerKeepsStoredLongDuration(helper);
    }

    @GameTest(template = TEMPLATE, batch = REMOTE_EYE_ISOLATED_BATCH)
    public static void remoteEyeSanitizerUsesLegacyFallbackWhenDurationMissing(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.remoteEyeSanitizerUsesLegacyFallbackWhenDurationMissing(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIRAGE_AVOIDANCE_ISOLATED_BATCH)
    public static void mirageAvoidanceUsesFifteenTickInvulnerabilityAndActiveRecastLock(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mirageAvoidanceUsesFifteenTickInvulnerabilityAndActiveRecastLock(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIRAGE_AVOIDANCE_ISOLATED_BATCH, timeoutTicks = 80)
    public static void mirageAvoidanceFreezesThenSlidesAndResetsFallDistance(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.mirageAvoidanceFreezesThenSlidesAndResetsFallDistance(helper);
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

    @GameTest(template = TEMPLATE, batch = AUTO_TURRET_ISOLATED_BATCH)
    public static void autoTurretRestockConsumesManaAndRestoresAmmo(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autoTurretRestockConsumesManaAndRestoresAmmo(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_TURRET_ISOLATED_BATCH)
    public static void autoTurretCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autoTurretCanBePlacedOnSupportedSlab(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_TURRET_ISOLATED_BATCH)
    public static void autoTurretCanBePlacedOnSupportedStairs(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autoTurretCanBePlacedOnSupportedStairs(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_TURRET_ISOLATED_BATCH, timeoutTicks = 80)
    public static void autoTurretFallsWhenSupportRemoved(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autoTurretFallsWhenSupportRemoved(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_TURRET_ISOLATED_BATCH)
    public static void autoTurretRestockFullAmmoDoesNotSpendMana(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autoTurretRestockFullAmmoDoesNotSpendMana(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_TURRET_ISOLATED_BATCH)
    public static void autoTurretRestockInsufficientManaDoesNotRestoreAmmo(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autoTurretRestockInsufficientManaDoesNotRestoreAmmo(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_TURRET_ISOLATED_BATCH, timeoutTicks = 180)
    public static void autoTurretAmmoDepletionKeepsAliveAndRestockClearsDiscardDelay(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.autoTurretAmmoDepletionKeepsAliveAndRestockClearsDiscardDelay(helper);
    }

    @GameTest(template = TEMPLATE, batch = FIELD_OVERSEER_ISOLATED_BATCH, timeoutTicks = 80)
    public static void fieldOverseerFallsWhenSupportRemoved(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.fieldOverseerFallsWhenSupportRemoved(helper);
    }

    @GameTest(template = TEMPLATE, batch = FIELD_OVERSEER_ISOLATED_BATCH)
    public static void fieldOverseerCastDataRoundTripsPlacementAndIdentity(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.fieldOverseerCastDataRoundTripsPlacementAndIdentity(helper);
    }

    @GameTest(template = TEMPLATE, batch = FIELD_OVERSEER_ISOLATED_BATCH)
    public static void fieldOverseerIgnoresOwnerDamage(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.fieldOverseerIgnoresOwnerDamage(helper);
    }

    @GameTest(template = TEMPLATE, batch = FIELD_OVERSEER_LIFECYCLE_BATCH)
    public static void fieldOverseerUsesDurationBoundPersistencePolicy(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.fieldOverseerUsesDurationBoundPersistencePolicy(helper);
    }

    @GameTest(template = TEMPLATE, batch = FIELD_OVERSEER_LIFECYCLE_BATCH)
    public static void fieldOverseerRecastRemovesPlacedStaff(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.fieldOverseerRecastRemovesPlacedStaff(helper);
    }

    @GameTest(template = TEMPLATE, batch = FIELD_OVERSEER_LIFECYCLE_BATCH)
    public static void fieldOverseerTimeoutRemovesPlacedStaff(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.fieldOverseerTimeoutRemovesPlacedStaff(helper);
    }

    @GameTest(template = TEMPLATE, batch = FIELD_OVERSEER_LIFECYCLE_BATCH)
    public static void fieldOverseerCancelledWhileUnloadedDoesNotReturn(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.fieldOverseerCancelledWhileUnloadedDoesNotReturn(helper);
    }

    @GameTest(template = TEMPLATE, batch = FIELD_OVERSEER_LIFECYCLE_BATCH)
    public static void fieldOverseerDestructionEndsMatchingRecast(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.fieldOverseerDestructionEndsMatchingRecast(helper);
    }

    @GameTest(template = TEMPLATE, batch = FIELD_OVERSEER_ISOLATED_BATCH, timeoutTicks = 100)
    public static void fieldOverseerPrioritizesHealthAndTransfersMana(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.fieldOverseerPrioritizesHealthAndTransfersMana(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.totemOfPermafrostCanBePlacedOnSupportedSlab(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH, timeoutTicks = 80)
    public static void totemOfPermafrostFallsWhenSupportRemoved(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.totemOfPermafrostFallsWhenSupportRemoved(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostInvalidPlacementCreatesNoRecast(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.totemOfPermafrostInvalidPlacementCreatesNoRecast(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostRecastRemovesPlacedTotem(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.totemOfPermafrostRecastRemovesPlacedTotem(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostMissingTotemRecastIsNoop(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.totemOfPermafrostMissingTotemRecastIsNoop(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostTimeoutRemovesPlacedTotem(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.totemOfPermafrostTimeoutRemovesPlacedTotem(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostGreaterConjurersTalismanSkipsTimeoutCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.totemOfPermafrostGreaterConjurersTalismanSkipsTimeoutCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostGreaterConjurersTalismanSkipsManualRecastCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.totemOfPermafrostGreaterConjurersTalismanSkipsManualRecastCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostPulseUsesSummonDamageAttribute(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.totemOfPermafrostPulseUsesSummonDamageAttribute(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_PULSE_BATCH, timeoutTicks = 40)
    public static void totemOfPermafrostPulseHitsVisibleCombatTargetsOnly(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.totemOfPermafrostPulseHitsVisibleCombatTargetsOnly(helper);
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

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildUsesBaseAndCraftsmansDelightRanges(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildUsesBaseAndCraftsmansDelightRanges(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildConsumesConfiguredManaPerPlacedBlock(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildConsumesConfiguredManaPerPlacedBlock(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCraftsmansDelightDiscountsManaPerBlock(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildCraftsmansDelightDiscountsManaPerBlock(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildStopsPartwayWhenManaIsDepleted(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildStopsPartwayWhenManaIsDepleted(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildZeroManaCostConfigAllowsPlacement(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildZeroManaCostConfigAllowsPlacement(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildScrollCastDoesNotConsumeAdditionalMana(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildScrollCastDoesNotConsumeAdditionalMana(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildPlacesUntilPlayerAxis(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildPlacesUntilPlayerAxis(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildTriesOneBlockWhenFirstPlacementTouchesPlayerAxis(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildTriesOneBlockWhenFirstPlacementTouchesPlayerAxis(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildUpwardFromPlayerYBlockPlacesOnlyOne(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildUpwardFromPlayerYBlockPlacesOnlyOne(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildPrefersOffhandBlockTemplate(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildPrefersOffhandBlockTemplate(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildUsesOffhandLuminousDeviceBeforeShulkerSource(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildUsesOffhandLuminousDeviceBeforeShulkerSource(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildConsumesSoulwovenPouchContents(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildConsumesSoulwovenPouchContents(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildConsumesLastRavenousPouchItem(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildConsumesLastRavenousPouchItem(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildUsesMalumPouchBeforeLuminousDevice(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildUsesMalumPouchBeforeLuminousDevice(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildMalumPouchRequiresMatchingComponents(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildMalumPouchRequiresMatchingComponents(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildResolvesZeroCountLuminousDeviceSelectionAndEmptyFallback(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildResolvesZeroCountLuminousDeviceSelectionAndEmptyFallback(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildConsumesReplaceablePlacedBlockTemplate(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildConsumesReplaceablePlacedBlockTemplate(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildRejectsLargeAndDenylistedTemplates(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildRejectsLargeAndDenylistedTemplates(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildRejectsOffhandTemplateWithoutMainHandFallback(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildRejectsOffhandTemplateWithoutMainHandFallback(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildSkipsBlockedPositionsByDefault(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildSkipsBlockedPositionsByDefault(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildAbortOnFailedPlacementConfigStopsAtBlockedPosition(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildAbortOnFailedPlacementConfigStopsAtBlockedPosition(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCopiesTopSlabAndConsumesCompanionTrunkFirst(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildCopiesTopSlabAndConsumesCompanionTrunkFirst(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCopiesBottomSlabType(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildCopiesBottomSlabType(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildDoesNotCopyDoubleSlabType(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildDoesNotCopyDoubleSlabType(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCopiesFurnaceFacing(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildCopiesFurnaceFacing(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCopiesLogAxis(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildCopiesLogAxis(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCopiesPistonFacing(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildCopiesPistonFacing(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCopiesSpellDispenserFacing(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildCopiesSpellDispenserFacing(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildKeepsShulkerBlockEntityTagContents(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildKeepsShulkerBlockEntityTagContents(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCopiesStairFacingAndHalfOnly(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildCopiesStairFacingAndHalfOnly(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCreativeCopiesHeldBlockWithoutConsumingStorage(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildCreativeCopiesHeldBlockWithoutConsumingStorage(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildConsumesPersonalShelfWithoutNearbyShelf(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildConsumesPersonalShelfWithoutNearbyShelf(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildPrefersEnderChestBeforeCreateToolbox(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildPrefersEnderChestBeforeCreateToolbox(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildConsumesPlacedCreateToolboxBeforeCompanionTrunk(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildConsumesPlacedCreateToolboxBeforeCompanionTrunk(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildIgnoresInventoryCreateToolboxAfterPlacedToolboxMisses(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildIgnoresInventoryCreateToolboxAfterPlacedToolboxMisses(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildShulkerSourceFollowsServerConfig(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildShulkerSourceFollowsServerConfig(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildShulkerSourceKeepsSlotAfterPartialConsume(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildShulkerSourceKeepsSlotAfterPartialConsume(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildBundleSourceFollowsServerConfig(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.linearBuildBundleSourceFollowsServerConfig(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMPOUND_PHIAL_ISOLATED_BATCH, timeoutTicks = 40)
    public static void compoundPhialSplashDamageUsesWeakFalloffAndKeepsSelfHit(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.compoundPhialSplashDamageUsesWeakFalloffAndKeepsSelfHit(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsRegularAirCastPreservesHorizontalMovementWithoutSelfMotionSync(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.assistWingsRegularAirCastPreservesHorizontalMovementWithoutSelfMotionSync(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsTaggedGroundCastKeepsWingAndBlocksFallProtection(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.assistWingsTaggedGroundCastKeepsWingAndBlocksFallProtection(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsTaggedOffhandBlocksFallProtectionWithoutDiscardingWing(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.assistWingsTaggedOffhandBlocksFallProtectionWithoutDiscardingWing(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsRemovingTaggedItemRestoresFallProtectionWithoutSlowingFall(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.assistWingsRemovingTaggedItemRestoresFallProtectionWithoutSlowingFall(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsTaggedLandingResetsAirJumpsAndAllowsNextAirCast(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.assistWingsTaggedLandingResetsAirJumpsAndAllowsNextAirCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsWaterRemovalUsesGraceAndResetsAirJumps(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.assistWingsWaterRemovalUsesGraceAndResetsAirJumps(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsLavaContactDoesNotRemoveWing(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.assistWingsLavaContactDoesNotRemoveWing(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsSuccessfulCastRestartsRemovalGrace(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.assistWingsSuccessfulCastRestartsRemovalGrace(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsRejectsOtherBroomsWithoutChangingState(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.assistWingsRejectsOtherBroomsWithoutChangingState(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsHoverrideUsesSurfaceAndAirJumpCounts(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.assistWingsHoverrideUsesSurfaceAndAirJumpCounts(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void smashcastScepterWindBurstUsesVanillaPostAttackEffect(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.smashcastScepterWindBurstUsesVanillaPostAttackEffect(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void smashcastScepterSmashSetsVanillaImpulseFallProtection(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.smashcastScepterSmashSetsVanillaImpulseFallProtection(helper);
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

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffInstantCastRunsAfterDelayAndAppliesPenaltyCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffInstantCastRunsAfterDelayAndAppliesPenaltyCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffCreativeCastSkipsFinalCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.multicastEchoStaffCreativeCastSkipsFinalCooldown(helper);
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

    @GameTest(template = TEMPLATE, batch = STRAIGHT_PROJECTILE_COLLISION_ISOLATED_BATCH, timeoutTicks = 40)
    public static void straightProjectilesTreatBoundingBoxGrazesAsBlockImpacts(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.straightProjectilesTreatBoundingBoxGrazesAsBlockImpacts(helper);
    }

    @GameTest(template = TEMPLATE, batch = STRAIGHT_PROJECTILE_COLLISION_ISOLATED_BATCH, timeoutTicks = 40)
    public static void straightProjectilesRespectCancelledBlockImpacts(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.straightProjectilesRespectCancelledBlockImpacts(helper);
    }

    @GameTest(template = TEMPLATE, batch = STRAIGHT_PROJECTILE_COLLISION_ISOLATED_BATCH, timeoutTicks = 40)
    public static void inscribeIceGraceDoesNotDuplicateBlockImpactEvent(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.inscribeIceGraceDoesNotDuplicateBlockImpactEvent(helper);
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

    @GameTest(template = TEMPLATE, batch = MOON_LIGHT_COUNTERSPELL_ISOLATED_BATCH)
    public static void moonLightSuccessfulDamageAppliesCounterspellEffects(GameTestHelper helper) {
        MoonLightCounterspellGameTestScenarios.successfulDamageAppliesCounterspellEffects(helper);
    }

    @GameTest(template = TEMPLATE, batch = MOON_LIGHT_COUNTERSPELL_ISOLATED_BATCH)
    public static void moonLightFailedDamageDoesNotApplyCounterspellEffects(GameTestHelper helper) {
        MoonLightCounterspellGameTestScenarios.failedDamageDoesNotApplyCounterspellEffects(helper);
    }

    @GameTest(template = TEMPLATE, batch = MOON_LIGHT_COUNTERSPELL_ISOLATED_BATCH)
    public static void moonLightCanceledCounterspellEventKeepsAdditionalEffects(GameTestHelper helper) {
        MoonLightCounterspellGameTestScenarios.canceledCounterspellEventKeepsAdditionalEffects(helper);
    }

    @GameTest(template = TEMPLATE, batch = MOON_LIGHT_COUNTERSPELL_ISOLATED_BATCH)
    public static void moonLightSuccessfulDamageCancelsMagicEntityCast(GameTestHelper helper) {
        MoonLightCounterspellGameTestScenarios.successfulDamageCancelsMagicEntityCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = MOON_LIGHT_COUNTERSPELL_ISOLATED_BATCH, timeoutTicks = 40)
    public static void moonLightChargeCutAppliesCounterspellEffects(GameTestHelper helper) {
        MoonLightCounterspellGameTestScenarios.chargeCutAppliesCounterspellEffects(helper);
    }

    @GameTest(template = TEMPLATE, batch = DUAL_ACROBAT_ISOLATED_BATCH, timeoutTicks = 80)
    public static void dualAcrobatStartsFiringAfterStartupAndContinuesWhileCasting(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.dualAcrobatStartsFiringAfterStartupAndContinuesWhileCasting(helper);
    }

    @GameTest(template = TEMPLATE, batch = DUAL_ACROBAT_ISOLATED_BATCH, timeoutTicks = 80)
    public static void dualAcrobatCompletionDiscardsImmediately(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.dualAcrobatCompletionDiscardsImmediately(helper);
    }

    @GameTest(template = TEMPLATE, batch = DUAL_ACROBAT_ISOLATED_BATCH, timeoutTicks = 40)
    public static void dualAcrobatCancelledInterruptionDiscardsImmediately(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.dualAcrobatCancelledInterruptionDiscardsImmediately(helper);
    }

    @GameTest(template = TEMPLATE, batch = DUAL_ACROBAT_ISOLATED_BATCH, timeoutTicks = 80)
    public static void dualAcrobatCounterspellInterruptDiscardsImmediately(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.dualAcrobatCounterspellInterruptDiscardsImmediately(helper);
    }

    @GameTest(template = TEMPLATE, batch = DUAL_ACROBAT_ISOLATED_BATCH, timeoutTicks = 40)
    public static void dualAcrobatCounterspellDoesNotInterruptNearbyOtherOwnerWeapon(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.dualAcrobatCounterspellDoesNotInterruptNearbyOtherOwnerWeapon(helper);
    }

    @GameTest(template = TEMPLATE, batch = BULLET_STREAM_ISOLATED_BATCH)
    public static void bulletStreamWaitsForSpinUpThenFiresEveryTick(GameTestHelper helper) {
        BulletStreamGameTestScenarios.waitsForSpinUpThenFiresEveryTick(helper);
    }

    @GameTest(template = TEMPLATE, batch = BULLET_STREAM_ISOLATED_BATCH)
    public static void bulletStreamReleaseKeepsWeaponAndOnlyFinishesAfterFiring(GameTestHelper helper) {
        BulletStreamGameTestScenarios.releaseKeepsWeaponForTenTicksAndOnlyFinishesAfterFiring(helper);
    }

    @GameTest(template = TEMPLATE, batch = BULLET_STREAM_ISOLATED_BATCH)
    public static void bulletStreamCastDurationIgnoresCastTimeReduction(GameTestHelper helper) {
        BulletStreamGameTestScenarios.castDurationIgnoresCastTimeReduction(helper);
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

    @GameTest(template = TEMPLATE, batch = BEAM_OCCLUSION_ISOLATED_BATCH)
    public static void beamLengthIgnoresNoCollisionGrass(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.beamLengthIgnoresNoCollisionGrass(helper);
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

    @GameTest(template = TEMPLATE, batch = SUMMON_WEAPON_ANIMATION_BATCH)
    public static void slashBladeStandbyAnimationSpeedTracksReducedCastTime(GameTestHelper helper) {
        SummonWeaponAnimationGameTestScenarios.slashBladeStandbyAnimationSpeedTracksReducedCastTime(helper);
    }

    @GameTest(template = TEMPLATE, batch = SUMMON_WEAPON_ANIMATION_BATCH)
    public static void moonLightStandbyAnimationSpeedAndDelayTrackReducedCastTime(GameTestHelper helper) {
        SummonWeaponAnimationGameTestScenarios.moonLightStandbyAnimationSpeedAndDelayTrackReducedCastTime(helper);
    }
}
