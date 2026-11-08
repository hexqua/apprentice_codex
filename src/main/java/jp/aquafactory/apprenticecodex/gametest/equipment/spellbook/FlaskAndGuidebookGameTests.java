package jp.aquafactory.apprenticecodex.gametest.equipment.spellbook;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FlaskAndGuidebookGameTests {
    private FlaskAndGuidebookGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void isekaiTravelGuidebookStartsWithTwoFixedSpellsAndNoAttributes(GameTestHelper helper) {
        GuidebookScenarios.isekaiTravelGuidebookStartsWithTwoFixedSpellsAndNoAttributes(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellStainedRunicTabletUsesDefaultServerConfigValues(GameTestHelper helper) {
        RunicTabletScenarios.spellStainedRunicTabletUsesDefaultServerConfigValues(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellStainedRunicTabletAcceptsNegativeServerConfigValues(GameTestHelper helper) {
        RunicTabletScenarios.spellStainedRunicTabletAcceptsNegativeServerConfigValues(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellStainedRunicTabletFiltersSchoolPowerByConfiguredThresholds(GameTestHelper helper) {
        RunicTabletScenarios.spellStainedRunicTabletFiltersSchoolPowerByConfiguredThresholds(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void explorersCodexGuidebookTransferRecipeMovesFixedSpellsAndKeepsExplorersData(GameTestHelper helper) {
        GuidebookScenarios.explorersCodexGuidebookTransferRecipeMovesFixedSpellsAndKeepsExplorersData(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void explorersCodexGuidebookTransferRecipeIgnoresDuplicateGuidebookSpell(GameTestHelper helper) {
        GuidebookScenarios.explorersCodexGuidebookTransferRecipeIgnoresDuplicateGuidebookSpell(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void explorersCodexGuidebookTransferRecipeRejectsSpellSlotOverflow(GameTestHelper helper) {
        GuidebookScenarios.explorersCodexGuidebookTransferRecipeRejectsSpellSlotOverflow(helper);
    }
}
