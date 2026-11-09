package jp.aquafactory.apprenticecodex.gametest.crafting.extraction;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.runtime.CoreGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellExtractionGameTests {
    private SpellExtractionGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellcasterWorkbenchExtractsEligibleSingleSlotSpells(GameTestHelper helper) {
        SpellExtractionGameTestScenarios.spellcasterWorkbenchExtractsEligibleSingleSlotSpells(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcasterWorkbenchReportsExtractionBlockReasons(GameTestHelper helper) {
        SpellExtractionGameTestScenarios.spellcasterWorkbenchReportsExtractionBlockReasons(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellcasterWorkbenchDoesNotConsumeExtractionInputsWhenOutputCannotMove(GameTestHelper helper) {
        SpellExtractionGameTestScenarios.spellcasterWorkbenchDoesNotConsumeExtractionInputsWhenOutputCannotMove(helper);
    }
}
