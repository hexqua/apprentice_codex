package jp.aquafactory.apprenticecodex.gametest.spell.grindrunner;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.grind_runner_isolated")
    public static void grindRunnerProcessesCreateCrushingWithoutCraftsmansDelight(GameTestHelper helper) {
        GrindRunnerScenarios.grindRunnerProcessesCreateCrushingWithoutCraftsmansDelight(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.grind_runner_isolated")
    public static void grindRunnerProcessesCreateMillingRecipes(GameTestHelper helper) {
        GrindRunnerScenarios.grindRunnerProcessesCreateMillingRecipes(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.grind_runner_isolated")
    public static void grindRunnerPrefersCreateCrushingBeforeMilling(GameTestHelper helper) {
        GrindRunnerScenarios.grindRunnerPrefersCreateCrushingBeforeMilling(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.grind_runner_isolated")
    public static void grindRunnerProcessesCreateDepotItems(GameTestHelper helper) {
        GrindRunnerScenarios.grindRunnerProcessesCreateDepotItems(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.grind_runner_isolated")
    public static void grindRunnerLeavesCreateChuteItemsUnprocessed(GameTestHelper helper) {
        GrindRunnerScenarios.grindRunnerLeavesCreateChuteItemsUnprocessed(helper);
    }
}
