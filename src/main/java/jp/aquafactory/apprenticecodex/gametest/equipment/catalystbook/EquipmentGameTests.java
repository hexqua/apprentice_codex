package jp.aquafactory.apprenticecodex.gametest.equipment.catalystbook;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.CHARGECAST_DENYLIST_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void chargecastCatalystbookStoresOnlyInstantSpellsAndExpandsToFourSlots(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.storesOnlyInstantSpellsAndExpandsToFourSlots(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargecastCatalystbookAppliesAdjustmentAndAttributePolicies(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.appliesAdjustmentAndAttributePolicies(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargecastCatalystbookRejectsPreCastSpellPowerDependentSpells(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.rejectsPreCastSpellPowerDependentSpells(helper);
    }

    @GameTest(template = TEMPLATE, batch = CHARGECAST_DENYLIST_CONFIG_BATCH)
    public static void chargecastCatalystbookSpellDenylistRejectsConfiguredSpell(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.spellDenylistRejectsConfiguredSpell(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargecastLethalAssaultWaitsForCompletionBeforeFiring(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.lethalAssaultWaitsForChargecastCompletionBeforeFiring(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargecastHiganbanaWaitsForCompletionBeforeSlashing(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.higanbanaWaitsForChargecastCompletionBeforeSlashing(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargecastLethalAssaultCancellationRemovesPreCastRifle(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.lethalAssaultCancellationRemovesPreCastRifle(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargecastHiganbanaCancellationRemovesPreCastKatana(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.higanbanaCancellationRemovesPreCastKatana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chargecastWisdomWheelCastOnlyRequiresHeldBookForExternalSpell(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.wisdomWheelCastOnlyRequiresHeldBookForExternalSpell(helper);
    }
}
