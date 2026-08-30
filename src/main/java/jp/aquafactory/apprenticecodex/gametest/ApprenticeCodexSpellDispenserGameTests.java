package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexSpellDispenserGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String SERVER_DISABLED_CONFIG_BATCH = "spell_dispenser_server_disabled_config";
    private static final String SERVER_ALLOWLIST_CONFIG_BATCH = "spell_dispenser_server_allowlist_config";
    private static final String BLOCK_SERVER_DISABLED_CONFIG_BATCH = "spell_dispenser_block_server_disabled_config";
    private static final String BLOCK_COOLDOWN_MULTIPLIER_CONFIG_BATCH = "spell_dispenser_block_cooldown_multiplier_config";
    private static final String CREATE_COOLDOWN_MULTIPLIER_CONFIG_BATCH = "spell_dispenser_create_cooldown_multiplier_config";
    private static final String CREATIVE_CONFIG_BATCH = "creative_spell_dispenser_config";

    private ApprenticeCodexSpellDispenserGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsSingleMagicMissileScroll(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserValidatorAcceptsSingleMagicMissileScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsNonIronsNamespaceScroll(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserValidatorAcceptsNonIronsNamespaceScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsLongScroll(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserValidatorAcceptsLongScroll(helper);
    }

    @GameTest(template = TEMPLATE, batch = SERVER_DISABLED_CONFIG_BATCH)
    public static void spellDispenserValidatorRejectsWhenServerDisabled(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserValidatorRejectsWhenServerDisabled(helper);
    }

    @GameTest(template = TEMPLATE, batch = SERVER_ALLOWLIST_CONFIG_BATCH)
    public static void spellDispenserValidatorRequiresServerAllowlist(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserValidatorRequiresServerAllowlist(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsUnprofiledRayOfSiphoning(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserValidatorRejectsUnprofiledRayOfSiphoning(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsContinuousScroll(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserValidatorRejectsContinuousScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsProfiledContinuousScroll(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserValidatorAcceptsProfiledContinuousScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsRecastScroll(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserValidatorRejectsRecastScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsNonScrollSpellContainer(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserValidatorRejectsNonScrollSpellContainer(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperCleansUpProxy(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCastHelperCleansUpProxy(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperAllowsOwnerOptionalSpellWithoutOwnerProfile(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCastHelperAllowsOwnerOptionalSpellWithoutOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserWizardlampPlacesLanternInAirWithoutBlockTarget(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserWizardlampPlacesLanternInAirWithoutBlockTarget(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperUsesNeutralLivingCasterProfileForMagicMissile(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCastHelperUsesNeutralLivingCasterProfileForMagicMissile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperCompletesLongCastImmediately(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCastHelperCompletesLongCastImmediately(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperTicksLongCastBeforeCompletion(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCastHelperTicksLongCastBeforeCompletion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserPrecisionJackLowManaCleansUpKnife(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserPrecisionJackLowManaCleansUpKnife(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperStopsContinuousCastAtDurationCap(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCastHelperStopsContinuousCastAtDurationCap(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserContinuousBreathBindsProjectileToTrackedAnchor(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserContinuousBreathBindsProjectileToTrackedAnchor(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityStopsContinuousCastWhenSignalTurnsOff(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserBlockEntityStopsContinuousCastWhenSignalTurnsOff(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityDoesNotRestartContinuousCastWhileSignalStaysOn(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserBlockEntityDoesNotRestartContinuousCastWhileSignalStaysOn(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityMarksContinuousResetRequirementAfterHeldCompletion(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserBlockEntityMarksContinuousResetRequirementAfterHeldCompletion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityStartsAndClearsInstantCooldown(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserBlockEntityStartsAndClearsInstantCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = BLOCK_SERVER_DISABLED_CONFIG_BATCH)
    public static void spellDispenserBlockEntityReportsServerDisabledActivation(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserBlockEntityReportsServerDisabledActivation(helper);
    }

    @GameTest(template = TEMPLATE, batch = BLOCK_COOLDOWN_MULTIPLIER_CONFIG_BATCH)
    public static void spellDispenserBlockEntityAppliesCooldownMultiplier(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserBlockEntityAppliesCooldownMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserAutomationOnlyAcceptsManaContainers(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserAutomationOnlyAcceptsManaContainers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserFluidAcceptsOnlyRegularManaPotions(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserFluidAcceptsOnlyRegularManaPotions(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserFluidConsumesPotionDoseImmediately(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserFluidConsumesPotionDoseImmediately(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserFluidDrainsUnconsumedPotionOnly(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserFluidDrainsUnconsumedPotionOnly(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserFluidPersistsThroughNbt(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserFluidPersistsThroughNbt(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserFluidAcceptsCreateManaPotion(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserFluidAcceptsCreateManaPotion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserPlacementStartsAtZeroManaAndStoresOwnerProfile(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserPlacementStartsAtZeroManaAndStoresOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void creativeSpellDispenserPlacementStartsAtZeroManaAndSkipsOwnerProfile(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.creativeSpellDispenserPlacementStartsAtZeroManaAndSkipsOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void creativeSpellDispenserIgnoresOwnerProfileNbt(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.creativeSpellDispenserIgnoresOwnerProfileNbt(helper);
    }

    @GameTest(template = TEMPLATE, batch = CREATIVE_CONFIG_BATCH)
    public static void creativeSpellDispenserUsesCreativeManaConfig(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.creativeSpellDispenserUsesCreativeManaConfig(helper);
    }

    @GameTest(template = TEMPLATE, batch = CREATIVE_CONFIG_BATCH)
    public static void creativeSpellDispenserAllowsZeroCooldownMultiplier(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.creativeSpellDispenserAllowsZeroCooldownMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void creativeSpellDispenserDropsNoStoredItems(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.creativeSpellDispenserDropsNoStoredItems(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void creativeSpellDispenserRestrictsMenuAccess(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.creativeSpellDispenserRestrictsMenuAccess(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserComparatorOutputMatchesStoredMana(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserComparatorOutputMatchesStoredMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserSneakPlacementStartsAtZeroMana(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserSneakPlacementStartsAtZeroMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserRefillsFromHighestManaPotionThatFits(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserRefillsFromHighestManaPotionThatFits(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserRefillsFromGlowEnergyFlask(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserRefillsFromGlowEnergyFlask(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserActivationFailsWhenManaIsInsufficient(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserActivationFailsWhenManaIsInsufficient(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserContinuousCastStopsWhenManaRunsOut(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserContinuousCastStopsWhenManaRunsOut(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateContinuousCastRequiresDisableBeforeRestart(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCreateContinuousCastRequiresDisableBeforeRestart(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateContinuousCastStopsWhenDisabled(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCreateContinuousCastStopsWhenDisabled(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateInstantCastUsesCooldown(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCreateInstantCastUsesCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = CREATE_COOLDOWN_MULTIPLIER_CONFIG_BATCH)
    public static void spellDispenserCreateAppliesCooldownMultiplier(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCreateAppliesCooldownMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateAllowsOwnerOptionalSpellWithoutOwnerProfile(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCreateAllowsOwnerOptionalSpellWithoutOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateRefillsFromFuelStorageAndReturnsBottle(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCreateRefillsFromFuelStorageAndReturnsBottle(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateIgnoresProtectedFuelStorage(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCreateIgnoresProtectedFuelStorage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityRejectsActivationWithoutOwnerProfile(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserBlockEntityRejectsActivationWithoutOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityAllowsOwnerOptionalActivationWithoutOwnerProfile(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserBlockEntityAllowsOwnerOptionalActivationWithoutOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserSidedAutomationProtectsScrollSlot(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserSidedAutomationProtectsScrollSlot(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserSidedAutomationExtractsOnlyEmptyContainers(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserSidedAutomationExtractsOnlyEmptyContainers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserMountedMenuKeepsContraptionInventoryAccessible(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserMountedMenuKeepsContraptionInventoryAccessible(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserMountedMenuTreatsOwnerOptionalSpellAsReadyWithoutOwnerProfile(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserMountedMenuTreatsOwnerOptionalSpellAsReadyWithoutOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserIsTaggedForContraptionControls(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserIsTaggedForContraptionControls(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void creativeSpellDispenserIsTaggedForContraptionControls(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.creativeSpellDispenserIsTaggedForContraptionControls(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserOwnerProfileCanBeReadFromSavedTag(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserOwnerProfileCanBeReadFromSavedTag(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserOwnerProfilePersistsThroughNbt(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserOwnerProfilePersistsThroughNbt(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperSupportsDiagonalVectorFacing(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCastHelperSupportsDiagonalVectorFacing(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperSupportsSpectralHammer(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCastHelperSupportsSpectralHammer(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastFrameProjectionMovesAndRotatesMagicMissile(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCastFrameProjectionMovesAndRotatesMagicMissile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastFrameProjectionKeepsShockEndpointsInWorldFrame(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCastFrameProjectionKeepsShockEndpointsInWorldFrame(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastFrameProjectionAppliesProfileOffsetsBeforeProjection(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserCastFrameProjectionAppliesProfileOffsetsBeforeProjection(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserContinuousProjectionUpdatesFireBreathAnchor(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserContinuousProjectionUpdatesFireBreathAnchor(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserContinuousProjectionUpdatesBlazeStormShots(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserContinuousProjectionUpdatesBlazeStormShots(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserContinuousProjectionUpdatesArcaneBeam(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserContinuousProjectionUpdatesArcaneBeam(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserContinuousProjectionUpdatesFujinWeapon(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserContinuousProjectionUpdatesFujinWeapon(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityReprojectsContinuousCastEachTick(GameTestHelper helper) {
        SpellDispenserGameTestScenarios.spellDispenserBlockEntityReprojectsContinuousCastEachTick(helper);
    }
}
