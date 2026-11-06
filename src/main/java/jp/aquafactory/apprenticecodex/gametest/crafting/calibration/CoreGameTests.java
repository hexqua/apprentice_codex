package jp.aquafactory.apprenticecodex.gametest.crafting.calibration;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void spellcasterWorkbenchExtractsEligibleSingleSlotSpells(GameTestHelper helper) {
        SpellExtractionGameTestScenarios.spellcasterWorkbenchExtractsEligibleSingleSlotSpells(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellcasterWorkbenchReportsExtractionBlockReasons(GameTestHelper helper) {
        SpellExtractionGameTestScenarios.spellcasterWorkbenchReportsExtractionBlockReasons(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellcasterWorkbenchDoesNotConsumeExtractionInputsWhenOutputCannotMove(GameTestHelper helper) {
        SpellExtractionGameTestScenarios.spellcasterWorkbenchDoesNotConsumeExtractionInputsWhenOutputCannotMove(helper);
    }
}
