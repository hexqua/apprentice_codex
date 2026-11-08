package jp.aquafactory.apprenticecodex.gametest.equipment.attackcastring;

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
    public static void attackcastRingSupportsOnlyRemovableInstantSpells(GameTestHelper helper) {
        AttackcastRingGameTestScenarios.attackcastRingSupportsOnlyRemovableInstantSpells(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attackcastRingConsumesManaAndAddsCooldown(GameTestHelper helper) {
        AttackcastRingGameTestScenarios.attackcastRingConsumesManaAndAddsCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attackcastRingSilentlySkipsCooldownAndFailsWithoutMana(GameTestHelper helper) {
        AttackcastRingGameTestScenarios.attackcastRingSilentlySkipsCooldownAndFailsWithoutMana(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attackcastRingDoesNotInterruptActiveCast(GameTestHelper helper) {
        AttackcastRingGameTestScenarios.attackcastRingDoesNotInterruptActiveCast(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attackcastRingCastsAllEquippedRingsInSlotOrder(GameTestHelper helper) {
        AttackcastRingGameTestScenarios.attackcastRingCastsAllEquippedRingsInSlotOrder(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attackcastRingDefersToSuccessfulHeldSwingcast(GameTestHelper helper) {
        AttackcastRingGameTestScenarios.attackcastRingDefersToSuccessfulHeldSwingcast(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attackcastRingRequiresFullChargeOutsideCompatTiming(GameTestHelper helper) {
        AttackcastRingGameTestScenarios.attackcastRingRequiresFullChargeOutsideCompatTiming(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attackcastRingEpicFightAttackPhaseUsesEquippedRing(GameTestHelper helper) {
        AttackcastRingGameTestScenarios.attackcastRingEpicFightAttackPhaseUsesEquippedRing(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attackcastRingEpicFightStaffrifleDoesNotFallback(GameTestHelper helper) {
        AttackcastRingGameTestScenarios.attackcastRingEpicFightStaffrifleDoesNotFallback(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attackcastRingEpicFightUsesSyncedBlockTarget(GameTestHelper helper) {
        AttackcastRingGameTestScenarios.attackcastRingEpicFightUsesSyncedBlockTarget(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attackcastRingFallsBackAfterCrystalBladedStaffHit(GameTestHelper helper) {
        AttackcastRingGameTestScenarios.attackcastRingFallsBackAfterCrystalBladedStaffHit(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attackcastRingFallsBackAfterCrystalBladedStaffMissFailure(GameTestHelper helper) {
        AttackcastRingGameTestScenarios.attackcastRingFallsBackAfterCrystalBladedStaffMissFailure(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attackcastRingCrystalBladedStaffDoesNotInterruptActiveCast(GameTestHelper helper) {
        AttackcastRingGameTestScenarios.attackcastRingCrystalBladedStaffDoesNotInterruptActiveCast(helper);
    }
}
