package jp.aquafactory.apprenticecodex.gametest.equipment.swingcaststaff;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void copperSwingcastStaffStartsWithBallLightningLevelOne(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.copperSwingcastStaffStartsWithBallLightningLevelOne(helper);
    }

    @GameTest(template = TEMPLATE, batch = IRON_SWINGCAST_STAFF_CRYSTALLIZE_CONFIG_BATCH)
    public static void ironSwingcastStaffCrystallizesOnlyForEnabledMainhandMobKillCredit(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.ironSwingcastStaffCrystallizesOnlyForEnabledMainhandMobKillCredit(helper);
    }

    @GameTest(template = TEMPLATE, batch = IRON_SWINGCAST_STAFF_CRYSTALLIZE_CONFIG_BATCH)
    public static void ironSwingcastStaffCrystallizeHintFollowsCommonDescription(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.ironSwingcastStaffCrystallizeHintFollowsCommonDescription(helper);
    }

    @GameTest(template = TEMPLATE, batch = HIGH_TIER_SWINGCAST_STAFF_CONFIG_BATCH)
    public static void highTierSwingcastStaffReducesImbuedCooldownAfterFullyChargedMeleeHit(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.highTierSwingcastStaffReducesImbuedCooldownAfterFullyChargedMeleeHit(helper);
    }

    @GameTest(template = TEMPLATE, batch = HIGH_TIER_SWINGCAST_STAFF_CONFIG_BATCH)
    public static void highTierSwingcastStaffCooldownRespectsDisabledAndInvalidHits(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.highTierSwingcastStaffCooldownRespectsDisabledAndInvalidHits(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void netheriteSwingcastStaffPreventsLongCastInterruptionInMainhand(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.netheriteSwingcastStaffPreventsLongCastInterruptionInMainhand(helper);
    }

    @GameTest(template = TEMPLATE, batch = HIGH_TIER_SWINGCAST_STAFF_CONFIG_BATCH)
    public static void swingcastStaffAbilityTooltipsUseSyncedConfigAndStableOrder(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.swingcastStaffAbilityTooltipsUseSyncedConfigAndStableOrder(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystallineArcaneShardUsesBlastingOnlyRecipeContract(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystallineArcaneShardUsesBlastingOnlyRecipeContract(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void copperSwingcastStaffReplacementSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.copperSwingcastStaffReplacementSpellStaysRemovableAfterNormalization(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void ironSwingcastStaffImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.ironSwingcastStaffImbuedSpellStaysRemovableAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void copperSwingcastStaffPresetEquivalentSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.copperSwingcastStaffPresetEquivalentSpellStaysRemovableAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void ironSwingcastStaffExtractedSpellStaysClearedAfterSaveLoad(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.ironSwingcastStaffExtractedSpellStaysClearedAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void ironSwingcastStaffLegacyLockedReplacementIsRecoveredAfterSaveLoad(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.ironSwingcastStaffLegacyLockedReplacementIsRecoveredAfterSaveLoad(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void swingcastStaffTiersExposeRequestedImbueRules(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.swingcastStaffTiersExposeRequestedImbueRules(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffStartsWithHiddenManaSlash(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffStartsWithHiddenManaSlash(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffMissSwingCastsManaSlash(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffMissSwingCastsManaSlash(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffHitSwingDoesNotCastManaSlash(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffHitSwingDoesNotCastManaSlash(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffVanillaAttackEntityHitDoesNotCastManaSlash(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffVanillaAttackEntityHitDoesNotCastManaSlash(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffDelayedHitDoesNotCastManaSlash(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffDelayedHitDoesNotCastManaSlash(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffMissTriggerDoesNotUseSwappedStack(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffMissTriggerDoesNotUseSwappedStack(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffPendingMissTriggerKeepsEarlierHand(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffPendingMissTriggerKeepsEarlierHand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffMainHandHitDoesNotSuppressOffhandMiss(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffMainHandHitDoesNotSuppressOffhandMiss(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffLegacyWheelPresetIsHiddenWhenHeld(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffLegacyWheelPresetIsHiddenWhenHeld(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffLegacyWheelPresetIsHiddenWhenHeldInOffhand(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffLegacyWheelPresetIsHiddenWhenHeldInOffhand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void crystalBladedStaffLegacyWheelReplacementStaysRemovableWhenHeld(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.crystalBladedStaffLegacyWheelReplacementStaysRemovableWhenHeld(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaSlashOffhandSwingUsesOffhandCatalystAttackDamage(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.manaSlashOffhandSwingUsesOffhandCatalystAttackDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaSlashCatalystDamageUsesStackAttributeModifiers(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.manaSlashCatalystDamageUsesStackAttributeModifiers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaSlashCatalystDamageAppliesAttributeEventOnce(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.manaSlashCatalystDamageAppliesAttributeEventOnce(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaSlashDamageMultiplierAppliesAfterMinimumDamage(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.manaSlashDamageMultiplierAppliesAfterMinimumDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaSlashAllowsNonSwingcastPrecondition(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.manaSlashAllowsNonSwingcastPrecondition(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaSlashRequiresSwingcastCatalystWhenContextIsActive(GameTestHelper helper) {
        SwingcastStaffGameTestScenarios.manaSlashRequiresSwingcastCatalystWhenContextIsActive(helper);
    }
}
