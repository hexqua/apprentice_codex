package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.automation;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.SpellDispenserGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserAutomationGameTests {
    private SpellDispenserAutomationGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellDispenserAutomationOnlyAcceptsManaContainers(GameTestHelper helper) {
        SpellDispenserAutomationScenarios.spellDispenserAutomationOnlyAcceptsManaContainers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserSidedAutomationProtectsScrollSlot(GameTestHelper helper) {
        SpellDispenserAutomationScenarios.spellDispenserSidedAutomationProtectsScrollSlot(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserSidedAutomationExtractsOnlyEmptyContainers(GameTestHelper helper) {
        SpellDispenserAutomationScenarios.spellDispenserSidedAutomationExtractsOnlyEmptyContainers(helper);
    }
}
