package jp.aquafactory.apprenticecodex.gametest.equipment.manashield;

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
    public static void manaShieldCharmUsesCharmSlotAndAppearsInCreativeTab(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmUsesCharmSlotAndAppearsInCreativeTab(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmFullyNegatesDamageAndPreservesArmorDurability(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmFullyNegatesDamageAndPreservesArmorDurability(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmBurnedOutFullNegateCancelsHitAndStartsCooldown(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmBurnedOutFullNegateCancelsHitAndStartsCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmLowManaBurnedOutFullNegateStillCancelsHit(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmLowManaBurnedOutFullNegateStillCancelsHit(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmDoesNotRespendManaDuringVanillaStyleIFrame(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmDoesNotRespendManaDuringVanillaStyleIFrame(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmPartialReductionEntersCooldownAndKeepsArmorMitigation(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmPartialReductionEntersCooldownAndKeepsArmorMitigation(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmCooldownRecoversAtOneHundredMana(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmCooldownRecoversAtOneHundredMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmShellUsesAllArmorEffectsAndBypassSkipsThem(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmShellUsesAllArmorEffectsAndBypassSkipsThem(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmShellActivationCostBurnoutPassesOriginalHit(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmShellActivationCostBurnoutPassesOriginalHit(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmShellExactActivationCostAppliesEffectAndBurnsOut(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmShellExactActivationCostAppliesEffectAndBurnsOut(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmShellChargesActivationCostForFractionalDamage(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmShellChargesActivationCostForFractionalDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmSynchronizationUsesHigherCostOnOrdinaryDamage(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmSynchronizationUsesHigherCostOnOrdinaryDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmSynchronizationDiscountsArmorBypassAndVoidDamage(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmSynchronizationDiscountsArmorBypassAndVoidDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmSynchronizationDoesNotDiscountInvulnerabilityBypass(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmSynchronizationDoesNotDiscountInvulnerabilityBypass(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmSynchronizationReductionDoesNotRecoverMana(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmSynchronizationReductionDoesNotRecoverMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaShieldCharmNeutralizationAbsorbsBypassArmorDamageDuringCooldown(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmNeutralizationAbsorbsBypassArmorDamageDuringCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void antiManaArrowNeutralizationConsumesFixedManaAndCancelsDamage(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.antiManaArrowNeutralizationConsumesFixedManaAndCancelsDamage(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void antiManaArrowDisablesUnprotectedManaShieldAfterCurrentHit(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.antiManaArrowDisablesUnprotectedManaShieldAfterCurrentHit(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void neutralizationCounterspellResistanceRequiresFullManaCost(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.neutralizationCounterspellResistanceRequiresFullManaCost(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_FREE_COST_CONFIG_BATCH)
    public static void manaShieldCharmFreeManaCostConfigAbsorbsWithoutDepletionCooldown(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmFreeManaCostConfigAbsorbsWithoutDepletionCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_RECOVERY_CONFIG_BATCH)
    public static void manaShieldCharmZeroRecoveryThresholdDisablesDepletionCooldown(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmZeroRecoveryThresholdDisablesDepletionCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_SYNCHRONIZATION_CONFIG_BATCH)
    public static void manaShieldCharmSynchronizationManaCostUsesServerConfig(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmSynchronizationManaCostUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_NEUTRALIZATION_CONFIG_BATCH)
    public static void manaShieldCharmNeutralizationZeroRecoveryStillNullifies(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmNeutralizationZeroRecoveryStillNullifies(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_SHELL_CONFIG_BATCH)
    public static void manaShieldCharmShellArmorDurabilityDamageUsesServerConfig(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmShellArmorDurabilityDamageUsesServerConfig(helper);
    }

    @GameTest(template = TEMPLATE, batch = MANA_SHIELD_CHARM_IFRAME_CONFIG_BATCH)
    public static void manaShieldCharmInvulnerableTimeUsesServerConfig(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmInvulnerableTimeUsesServerConfig(helper);
    }
}
