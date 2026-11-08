package jp.aquafactory.apprenticecodex.gametest.equipment.spellbook;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.flask.FlaskAndGuidebookGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FlaskAndGuidebookGameTests {
    private FlaskAndGuidebookGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void isekaiTravelGuidebookStartsWithTwoFixedSpellsAndNoAttributes(GameTestHelper helper) {
        GuidebookScenarios.isekaiTravelGuidebookStartsWithTwoFixedSpellsAndNoAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellStainedRunicTabletUsesDefaultServerConfigValues(GameTestHelper helper) {
        RunicTabletScenarios.spellStainedRunicTabletUsesDefaultServerConfigValues(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellStainedRunicTabletAcceptsNegativeServerConfigValues(GameTestHelper helper) {
        RunicTabletScenarios.spellStainedRunicTabletAcceptsNegativeServerConfigValues(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellStainedRunicTabletFiltersSchoolPowerByConfiguredThresholds(GameTestHelper helper) {
        RunicTabletScenarios.spellStainedRunicTabletFiltersSchoolPowerByConfiguredThresholds(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void explorersCodexGuidebookTransferRecipeMovesFixedSpellsAndKeepsExplorersData(GameTestHelper helper) {
        GuidebookScenarios.explorersCodexGuidebookTransferRecipeMovesFixedSpellsAndKeepsExplorersData(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void explorersCodexGuidebookTransferRecipeIgnoresDuplicateGuidebookSpell(GameTestHelper helper) {
        GuidebookScenarios.explorersCodexGuidebookTransferRecipeIgnoresDuplicateGuidebookSpell(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void explorersCodexGuidebookTransferRecipeRejectsSpellSlotOverflow(GameTestHelper helper) {
        GuidebookScenarios.explorersCodexGuidebookTransferRecipeRejectsSpellSlotOverflow(helper);
    }
}
