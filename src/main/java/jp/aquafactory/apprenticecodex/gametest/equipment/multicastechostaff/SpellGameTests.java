package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 80)
    public static void multicastEchoStaffInstantCastRunsAfterDelayAndAppliesPenaltyCooldown(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffInstantCastRunsAfterDelayAndAppliesPenaltyCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 80)
    public static void multicastEchoStaffCreativeCastSkipsFinalCooldown(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffCreativeCastSkipsFinalCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 80)
    public static void multicastEchoStaffInsufficientManaEndsWithPenaltyCooldown(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffInsufficientManaEndsWithPenaltyCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 80)
    public static void multicastEchoStaffItemChangeEndsWithPenaltyCooldown(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffItemChangeEndsWithPenaltyCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 80)
    public static void multicastEchoStaffLogoutEndsWithPenaltyCooldown(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffLogoutEndsWithPenaltyCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 80)
    public static void multicastEchoStaffLongCastAddsSkippedCastTimeCooldown(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffLongCastAddsSkippedCastTimeCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 80)
    public static void multicastEchoStaffInvalidInstantCastKeepsEchoSpell(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffInvalidInstantCastKeepsEchoSpell(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 80)
    public static void multicastEchoStaffInvalidLongCastIgnoresStaleEchoContext(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffInvalidLongCastIgnoresStaleEchoContext(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 80)
    public static void multicastEchoStaffLongCastUsesStartEchoContextAfterEffectRemoved(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffLongCastUsesStartEchoContextAfterEffectRemoved(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 80)
    public static void multicastEchoStaffRepeatedFortifyClearsTargetAreaIndicator(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffRepeatedFortifyClearsTargetAreaIndicator(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 40)
    public static void multicastEchoStaffMobEffectProfileExtendsDuplicateDuration(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffMobEffectProfileExtendsDuplicateDuration(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 40)
    public static void multicastEchoStaffMobEffectProfileStacksAmplifierByLevel(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffMobEffectProfileStacksAmplifierByLevel(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 40)
    public static void multicastEchoStaffMobEffectProfileIgnoresMissingProfile(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffMobEffectProfileIgnoresMissingProfile(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 40)
    public static void multicastEchoStaffAttackProfileScalesDirectCombatToolsDamage(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffAttackProfileScalesDirectCombatToolsDamage(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 40)
    public static void multicastEchoStaffAttackProfileTracksDelayedProjectileDamage(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffAttackProfileTracksDelayedProjectileDamage(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_cooldown_cap", timeoutTicks = 80)
    public static void multicastEchoStaffCooldownCapLimitsAdjustedCooldown(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffCooldownCapLimitsAdjustedCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_base_cooldown_cap", timeoutTicks = 80)
    public static void multicastEchoStaffBaseCooldownAboveCapUsesOriginalCooldown(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffBaseCooldownAboveCapUsesOriginalCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.echo_cast_multicast_limit", timeoutTicks = 40)
    public static void echoCastStopsAtConfiguredMulticastLimit(GameTestHelper helper) {
        MulticastEchoStaffScenarios.echoCastStopsAtConfiguredMulticastLimit(helper);
    }
}
