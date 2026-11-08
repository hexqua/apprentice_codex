package jp.aquafactory.apprenticecodex.gametest.equipment.chargedtwinbladestaff;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffUpgradeMergesMainhandMeleeDamage(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffUpgradeMergesMainhandMeleeDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffResolveThrownDamageIncludesApplicableEnchantments(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffResolveThrownDamageIncludesApplicableEnchantments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedStaffConfiguredThrowCostsAndBoundaries(GameTestHelper helper) {
        ChargedStaffThrowGameTestScenarios.configuredCostsAndBoundaries(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedStaffThrowConfigSyncAndTooltip(GameTestHelper helper) {
        ChargedStaffThrowGameTestScenarios.configSyncAndTooltip(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedStaffChannelingDoesNotSummonLightning(GameTestHelper helper) {
        ChargedStaffThrowGameTestScenarios.channelingDoesNotSummonLightning(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedStaffEntityImpactKeepsHitPositionWithoutFollowing(GameTestHelper helper) {
        ChargedStaffThrowGameTestScenarios.entityImpactKeepsHitPositionWithoutFollowing(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffThrowConsumesMana(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffThrowConsumesMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffLoyaltyReducesThrowManaCost(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffLoyaltyReducesThrowManaCost(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffRiptideWorksOnDryGroundWithoutProjectile(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffRiptideWorksOnDryGroundWithoutProjectile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedStaffRiptideUpkeepAndExhaustion(GameTestHelper helper) {
        ChargedStaffRiptideGameTestScenarios.upkeepAndExhaustion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedStaffRiptideIndependentCostsAndFreeUpkeep(GameTestHelper helper) {
        ChargedStaffRiptideGameTestScenarios.independentCostsAndFreeUpkeep(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedStaffRiptideCollisionAndFreshCharge(GameTestHelper helper) {
        ChargedStaffRiptideGameTestScenarios.collisionAndFreshCharge(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedStaffRiptideEquipmentAndCreativeBoundaries(GameTestHelper helper) {
        ChargedStaffRiptideGameTestScenarios.equipmentAndCreativeBoundaries(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedStaffRiptideTooltipStylesAndCosts(GameTestHelper helper) {
        ChargedStaffRiptideGameTestScenarios.tooltipStylesAndCosts(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void chargedStaffRiptideCruiseSpeedAndSteering(GameTestHelper helper) {
        ChargedStaffRiptideGameTestScenarios.cruiseSpeedAndSteering(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactForwardUsesHistoryAndFallback(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactForwardUsesHistoryAndFallback(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerCastsInstantAndLongSpells(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerCastsInstantAndLongSpells(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerCastsInstantWhileOwnerBusy(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerCastsInstantWhileOwnerBusy(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffBusyFallbackDoesNotBypassCooldown(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffBusyFallbackDoesNotBypassCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellThrowableCardImpactCastManagerCastsInstantWhileOwnerBusy(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.spellThrowableCardImpactCastManagerCastsInstantWhileOwnerBusy(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffRemoteOwnerDenylistBlocksRuntimeWithoutFallback(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffRemoteOwnerDenylistBlocksRuntimeWithoutFallback(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerCastsPlayerSelfProfile(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerCastsPlayerSelfProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffCreativeImpactCastUsesRemoteOwnerProfileWithZeroMana(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffCreativeImpactCastUsesRemoteOwnerProfileWithZeroMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffCreativeImpactCastUsesStaffProfileWithZeroMana(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffCreativeImpactCastUsesStaffProfileWithZeroMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerCastsInitialRaiseDeadProfile(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerCastsInitialRaiseDeadProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerBlocksRaiseDeadWhenRecastExists(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerBlocksRaiseDeadWhenRecastExists(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffRaiseDeadPreservesWheelSelectionAfterRecast(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffRaiseDeadPreservesWheelSelectionAfterRecast(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerRejectsUnprofiledSpell(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerRejectsUnprofiledSpell(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerStartsContinuousSpells(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerStartsContinuousSpells(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffContinuousRemoteOwnerIgnoresMissingDispenserProfile(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffContinuousRemoteOwnerIgnoresMissingDispenserProfile(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void chargedTwinBladeStaffContinuousThrowableCardUsesWeaponImbueCooldown(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffContinuousThrowableCardUsesWeaponImbueCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargedTwinBladeStaffImpactCastManagerSkipsWhenOwnerCannotCast(GameTestHelper helper) {
        ChargedTwinBladeStaffGameTestScenarios.chargedTwinBladeStaffImpactCastManagerSkipsWhenOwnerCannotCast(helper);
    }
}
