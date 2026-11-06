package jp.aquafactory.apprenticecodex.gametest.equipment.manashield;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmUsesCharmSlotAndAppearsInCreativeTab(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmUsesCharmSlotAndAppearsInCreativeTab(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmExclusiveEnchantmentsStayMutuallyExclusive(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmExclusiveEnchantmentsStayMutuallyExclusive(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmFullyNegatesDamageAndPreservesArmorDurability(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmFullyNegatesDamageAndPreservesArmorDurability(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmBurnedOutFullNegateCancelsHitAndStartsCooldown(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmBurnedOutFullNegateCancelsHitAndStartsCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmLowManaBurnedOutFullNegateStillCancelsHit(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmLowManaBurnedOutFullNegateStillCancelsHit(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmDoesNotRespendManaDuringVanillaStyleIFrame(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmDoesNotRespendManaDuringVanillaStyleIFrame(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmPartialReductionEntersCooldownAndKeepsArmorMitigation(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmPartialReductionEntersCooldownAndKeepsArmorMitigation(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmCooldownRecoversAtOneHundredMana(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmCooldownRecoversAtOneHundredMana(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmShellUsesAllArmorEffectsAndBypassSkipsThem(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmShellUsesAllArmorEffectsAndBypassSkipsThem(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmShellActivationCostBurnoutPassesOriginalHit(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmShellActivationCostBurnoutPassesOriginalHit(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmShellExactActivationCostAppliesEffectAndBurnsOut(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmShellExactActivationCostAppliesEffectAndBurnsOut(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmShellChargesActivationCostForFractionalDamage(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmShellChargesActivationCostForFractionalDamage(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmSynchronizationUsesHigherCostOnOrdinaryDamage(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmSynchronizationUsesHigherCostOnOrdinaryDamage(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmSynchronizationDiscountsArmorBypassAndVoidDamage(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmSynchronizationDiscountsArmorBypassAndVoidDamage(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmSynchronizationDoesNotDiscountInvulnerabilityBypass(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmSynchronizationDoesNotDiscountInvulnerabilityBypass(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmSynchronizationReductionDoesNotRecoverMana(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmSynchronizationReductionDoesNotRecoverMana(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaShieldCharmNeutralizationAbsorbsBypassArmorDamageDuringCooldown(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmNeutralizationAbsorbsBypassArmorDamageDuringCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void antiManaArrowNeutralizationConsumesFixedManaAndCancelsDamage(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.antiManaArrowNeutralizationConsumesFixedManaAndCancelsDamage(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void antiManaArrowDisablesUnprotectedManaShieldAfterCurrentHit(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.antiManaArrowDisablesUnprotectedManaShieldAfterCurrentHit(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void neutralizationCounterspellResistanceRequiresFullManaCost(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.neutralizationCounterspellResistanceRequiresFullManaCost(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_shield_charm_free_cost_config")
    public static void manaShieldCharmFreeManaCostConfigAbsorbsWithoutDepletionCooldown(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmFreeManaCostConfigAbsorbsWithoutDepletionCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_shield_charm_recovery_config")
    public static void manaShieldCharmZeroRecoveryThresholdDisablesDepletionCooldown(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmZeroRecoveryThresholdDisablesDepletionCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_shield_charm_synchronization_config")
    public static void manaShieldCharmSynchronizationManaCostUsesServerConfig(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmSynchronizationManaCostUsesServerConfig(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_shield_charm_neutralization_config")
    public static void manaShieldCharmNeutralizationZeroRecoveryStillNullifies(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmNeutralizationZeroRecoveryStillNullifies(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_shield_charm_shell_config")
    public static void manaShieldCharmShellArmorDurabilityDamageUsesServerConfig(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmShellArmorDurabilityDamageUsesServerConfig(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mana_shield_charm_iframe_config")
    public static void manaShieldCharmInvulnerableTimeUsesServerConfig(GameTestHelper helper) {
        ManaShieldCharmGameTestScenarios.manaShieldCharmInvulnerableTimeUsesServerConfig(helper);
    }
}
