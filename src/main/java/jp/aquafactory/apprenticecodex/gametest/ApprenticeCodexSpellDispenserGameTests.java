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

    private ApprenticeCodexSpellDispenserGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsSingleMagicMissileScroll(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserValidatorAcceptsSingleMagicMissileScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsNonIronsNamespaceScroll(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserValidatorAcceptsNonIronsNamespaceScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsLongScroll(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserValidatorAcceptsLongScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsDenylistedSpell(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserValidatorRejectsDenylistedSpell(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsRayOfSiphoning(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserValidatorRejectsRayOfSiphoning(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsContinuousScroll(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserValidatorRejectsContinuousScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorAcceptsProfiledContinuousScroll(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserValidatorAcceptsProfiledContinuousScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsRecastScroll(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserValidatorRejectsRecastScroll(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserValidatorRejectsNonScrollSpellContainer(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserValidatorRejectsNonScrollSpellContainer(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperCleansUpProxy(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserCastHelperCleansUpProxy(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperAllowsOwnerOptionalSpellWithoutOwnerProfile(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserCastHelperAllowsOwnerOptionalSpellWithoutOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperCompletesLongCastImmediately(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserCastHelperCompletesLongCastImmediately(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperTicksLongCastBeforeCompletion(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserCastHelperTicksLongCastBeforeCompletion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserPrecisionJackLowManaCleansUpKnife(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserPrecisionJackLowManaCleansUpKnife(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperStopsContinuousCastAtDurationCap(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserCastHelperStopsContinuousCastAtDurationCap(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserContinuousBreathBindsProjectileToTrackedAnchor(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserContinuousBreathBindsProjectileToTrackedAnchor(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityStopsContinuousCastWhenSignalTurnsOff(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserBlockEntityStopsContinuousCastWhenSignalTurnsOff(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityDoesNotRestartContinuousCastWhileSignalStaysOn(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserBlockEntityDoesNotRestartContinuousCastWhileSignalStaysOn(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityMarksContinuousResetRequirementAfterHeldCompletion(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserBlockEntityMarksContinuousResetRequirementAfterHeldCompletion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityStartsAndClearsInstantCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserBlockEntityStartsAndClearsInstantCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserAutomationOnlyAcceptsManaContainers(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserAutomationOnlyAcceptsManaContainers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserFluidAcceptsOnlyRegularManaPotions(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserFluidAcceptsOnlyRegularManaPotions(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserFluidConsumesPotionDoseImmediately(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserFluidConsumesPotionDoseImmediately(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserFluidDrainsUnconsumedPotionOnly(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserFluidDrainsUnconsumedPotionOnly(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserFluidPersistsThroughNbt(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserFluidPersistsThroughNbt(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserFluidAcceptsCreateManaPotion(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserFluidAcceptsCreateManaPotion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserPlacementStartsAtZeroManaAndStoresOwnerProfile(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserPlacementStartsAtZeroManaAndStoresOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserComparatorOutputMatchesStoredMana(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserComparatorOutputMatchesStoredMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserSneakPlacementStartsAtZeroMana(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserSneakPlacementStartsAtZeroMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserRefillsFromHighestManaPotionThatFits(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserRefillsFromHighestManaPotionThatFits(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserRefillsFromGlowEnergyFlask(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserRefillsFromGlowEnergyFlask(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserActivationFailsWhenManaIsInsufficient(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserActivationFailsWhenManaIsInsufficient(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserContinuousCastStopsWhenManaRunsOut(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserContinuousCastStopsWhenManaRunsOut(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateContinuousCastRequiresDisableBeforeRestart(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserCreateContinuousCastRequiresDisableBeforeRestart(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateContinuousCastStopsWhenDisabled(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserCreateContinuousCastStopsWhenDisabled(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateInstantCastUsesCooldown(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserCreateInstantCastUsesCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateAllowsOwnerOptionalSpellWithoutOwnerProfile(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserCreateAllowsOwnerOptionalSpellWithoutOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateRefillsFromFuelStorageAndReturnsBottle(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserCreateRefillsFromFuelStorageAndReturnsBottle(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateIgnoresProtectedFuelStorage(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserCreateIgnoresProtectedFuelStorage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityRejectsActivationWithoutOwnerProfile(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserBlockEntityRejectsActivationWithoutOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityAllowsOwnerOptionalActivationWithoutOwnerProfile(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserBlockEntityAllowsOwnerOptionalActivationWithoutOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserSidedAutomationProtectsScrollSlot(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserSidedAutomationProtectsScrollSlot(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserSidedAutomationExtractsOnlyEmptyContainers(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserSidedAutomationExtractsOnlyEmptyContainers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserMountedMenuKeepsContraptionInventoryAccessible(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserMountedMenuKeepsContraptionInventoryAccessible(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserMountedMenuTreatsOwnerOptionalSpellAsReadyWithoutOwnerProfile(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserMountedMenuTreatsOwnerOptionalSpellAsReadyWithoutOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserIsTaggedForContraptionControls(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserIsTaggedForContraptionControls(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserOwnerProfileCanBeReadFromSavedTag(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserOwnerProfileCanBeReadFromSavedTag(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserOwnerProfilePersistsThroughNbt(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserOwnerProfilePersistsThroughNbt(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperSupportsDiagonalVectorFacing(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserCastHelperSupportsDiagonalVectorFacing(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperSupportsSpectralHammer(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.spellDispenserCastHelperSupportsSpectralHammer(helper);
    }
}
