package jp.aquafactory.apprenticecodex.gametest.equipment.parryingring;

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
    public static void spellCastParryingRingBlocksFrontLongCastWithinWindow(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingBlocksFrontLongCastWithinWindow(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCastParryingRingWithoutRingDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingWithoutRingDoesNotBlock(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spell_cast_parrying_ring_config")
    public static void spellCastParryingRingAfterWindowDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingAfterWindowDoesNotBlock(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCastParryingRingBackAttackDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingBackAttackDoesNotBlock(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCastParryingRingNormalInstantAndContinuousDoNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingNormalInstantAndContinuousDoNotBlock(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCastParryingRingStoppedLongCastDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingStoppedLongCastDoesNotBlock(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCastParryingRingFocusStaffbowInstantPendingBlocks(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingFocusStaffbowInstantPendingBlocks(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCastParryingRingFocusStaffbowLongPendingBlocks(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingFocusStaffbowLongPendingBlocks(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spell_cast_parrying_ring_config")
    public static void spellCastParryingRingChargecastRespectsWindowAndCancellation(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingChargecastRespectsWindowAndCancellation(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCastParryingRingFocusStaffbowContinuousPendingDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingFocusStaffbowContinuousPendingDoesNotBlock(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCastParryingRingBypassShieldDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingBypassShieldDoesNotBlock(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCastParryingRingDiscardsBlockedProjectile(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingDiscardsBlockedProjectile(helper);
    }
}
