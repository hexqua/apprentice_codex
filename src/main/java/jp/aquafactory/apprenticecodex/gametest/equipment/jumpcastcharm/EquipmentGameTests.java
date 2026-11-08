package jp.aquafactory.apprenticecodex.gametest.equipment.jumpcastcharm;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.JUMPCAST_CHARM_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void jumpcastCharmUsesFeetSlotAndSupportsCalibrationImbue(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmUsesFeetSlotAndSupportsCalibrationImbue(helper);
    }

    @GameTest(template = TEMPLATE, batch = JUMPCAST_CHARM_CONFIG_BATCH)
    public static void jumpcastCharmInstantSpellCastsInAirAndConsumesMana(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmInstantSpellCastsInAirAndConsumesMana(helper);
    }

    @GameTest(template = TEMPLATE, batch = JUMPCAST_CHARM_CONFIG_BATCH)
    public static void jumpcastCharmLongSpellFailsWithoutSpendingOrCooldown(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmLongSpellFailsWithoutSpendingOrCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = JUMPCAST_CHARM_CONFIG_BATCH)
    public static void jumpcastCharmInsufficientManaFailsWithoutSpending(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmInsufficientManaFailsWithoutSpending(helper);
    }

    @GameTest(template = TEMPLATE, batch = JUMPCAST_CHARM_CONFIG_BATCH)
    public static void jumpcastCharmPreservesExistingCast(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmPreservesExistingCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = JUMPCAST_CHARM_CONFIG_BATCH)
    public static void jumpcastCharmBlockedMovementContextsDoNotCast(GameTestHelper helper) {
        JumpcastCharmGameTestScenarios.jumpcastCharmBlockedMovementContextsDoNotCast(helper);
    }
}
