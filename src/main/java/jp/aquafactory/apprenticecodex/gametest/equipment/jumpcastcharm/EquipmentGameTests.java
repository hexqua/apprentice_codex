package jp.aquafactory.apprenticecodex.gametest.equipment.jumpcastcharm;

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
    public static void jumpcastCharmUsesFeetSlotAndSupportsCalibrationImbue(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmUsesFeetSlotAndSupportsCalibrationImbue(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.jumpcast_charm_config")
    public static void jumpcastCharmInstantSpellCastsInAirAndConsumesMana(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmInstantSpellCastsInAirAndConsumesMana(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.jumpcast_charm_config")
    public static void jumpcastCharmLongSpellFailsWithoutSpendingOrCooldown(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmLongSpellFailsWithoutSpendingOrCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.jumpcast_charm_config")
    public static void jumpcastCharmInsufficientManaFailsWithoutSpending(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmInsufficientManaFailsWithoutSpending(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.jumpcast_charm_config")
    public static void jumpcastCharmPreservesExistingCast(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmPreservesExistingCast(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.jumpcast_charm_config")
    public static void jumpcastCharmBlockedMovementContextsDoNotCast(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmBlockedMovementContextsDoNotCast(helper);
    }
}
