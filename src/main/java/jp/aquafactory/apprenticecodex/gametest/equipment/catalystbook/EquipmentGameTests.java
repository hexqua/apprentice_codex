package jp.aquafactory.apprenticecodex.gametest.equipment.catalystbook;

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
    public static void chargecastCatalystbookStoresOnlyInstantSpellsAndExpandsToFourSlots(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.storesOnlyInstantSpellsAndExpandsToFourSlots(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void chargecastCatalystbookAppliesAdjustmentAndAttributePolicies(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.appliesAdjustmentAndAttributePolicies(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void chargecastCatalystbookRejectsPreCastSpellPowerDependentSpells(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.rejectsPreCastSpellPowerDependentSpells(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.chargecast_denylist_config")
    public static void chargecastCatalystbookSpellDenylistRejectsConfiguredSpell(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.spellDenylistRejectsConfiguredSpell(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void chargecastLethalAssaultWaitsForCompletionBeforeFiring(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.lethalAssaultWaitsForChargecastCompletionBeforeFiring(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void chargecastHiganbanaWaitsForCompletionBeforeSlashing(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.higanbanaWaitsForChargecastCompletionBeforeSlashing(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void chargecastLethalAssaultCancellationRemovesPreCastRifle(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.lethalAssaultCancellationRemovesPreCastRifle(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void chargecastHiganbanaCancellationRemovesPreCastKatana(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.higanbanaCancellationRemovesPreCastKatana(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void chargecastWisdomWheelCastOnlyRequiresHeldBookForExternalSpell(GameTestHelper helper) {
        ChargecastCatalystbookGameTestScenarios.wisdomWheelCastOnlyRequiresHeldBookForExternalSpell(helper);
    }
}
