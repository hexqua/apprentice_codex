package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexFlaskAndGuidebookGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private ApprenticeCodexFlaskAndGuidebookGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void isekaiTravelGuidebookStartsWithTwoFixedSpellsAndNoAttributes(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.isekaiTravelGuidebookStartsWithTwoFixedSpellsAndNoAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void explorersCodexGuidebookTransferRecipeMovesFixedSpellsAndKeepsExplorersData(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.explorersCodexGuidebookTransferRecipeMovesFixedSpellsAndKeepsExplorersData(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void explorersCodexGuidebookTransferRecipeIgnoresDuplicateGuidebookSpell(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.explorersCodexGuidebookTransferRecipeIgnoresDuplicateGuidebookSpell(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void explorersCodexGuidebookTransferRecipeRejectsSpellSlotOverflow(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.explorersCodexGuidebookTransferRecipeRejectsSpellSlotOverflow(helper);
    }
}
