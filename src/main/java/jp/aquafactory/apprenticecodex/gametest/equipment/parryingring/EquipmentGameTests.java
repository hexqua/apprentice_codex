package jp.aquafactory.apprenticecodex.gametest.equipment.parryingring;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.SPELL_CAST_PARRYING_RING_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingBlocksFrontLongCastWithinWindow(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingBlocksFrontLongCastWithinWindow(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingWithoutRingDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingWithoutRingDoesNotBlock(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELL_CAST_PARRYING_RING_CONFIG_BATCH)
    public static void spellCastParryingRingAfterWindowDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingAfterWindowDoesNotBlock(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingBackAttackDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingBackAttackDoesNotBlock(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingNormalInstantAndContinuousDoNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingNormalInstantAndContinuousDoNotBlock(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingStoppedLongCastDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingStoppedLongCastDoesNotBlock(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingFocusStaffbowInstantPendingBlocks(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingFocusStaffbowInstantPendingBlocks(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingFocusStaffbowLongPendingBlocks(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingFocusStaffbowLongPendingBlocks(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELL_CAST_PARRYING_RING_CONFIG_BATCH)
    public static void spellCastParryingRingChargecastRespectsWindowAndCancellation(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingChargecastRespectsWindowAndCancellation(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingFocusStaffbowContinuousPendingDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingFocusStaffbowContinuousPendingDoesNotBlock(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingBypassShieldDoesNotBlock(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingBypassShieldDoesNotBlock(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellCastParryingRingDiscardsBlockedProjectile(GameTestHelper helper) {
        SpellCastParryingRingGameTestScenarios.spellCastParryingRingDiscardsBlockedProjectile(helper);
    }
}
