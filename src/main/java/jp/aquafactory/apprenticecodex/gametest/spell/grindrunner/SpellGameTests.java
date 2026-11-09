package jp.aquafactory.apprenticecodex.gametest.spell.grindrunner;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.GRIND_RUNNER_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = GRIND_RUNNER_ISOLATED_BATCH)
    public static void grindRunnerProcessesCreateCrushingWithoutCraftsmansDelight(GameTestHelper helper) {
        GrindRunnerScenarios.grindRunnerProcessesCreateCrushingWithoutCraftsmansDelight(helper);
    }

    @GameTest(template = TEMPLATE, batch = GRIND_RUNNER_ISOLATED_BATCH)
    public static void grindRunnerProcessesCreateMillingRecipes(GameTestHelper helper) {
        GrindRunnerScenarios.grindRunnerProcessesCreateMillingRecipes(helper);
    }

    @GameTest(template = TEMPLATE, batch = GRIND_RUNNER_ISOLATED_BATCH)
    public static void grindRunnerPrefersCreateCrushingBeforeMilling(GameTestHelper helper) {
        GrindRunnerScenarios.grindRunnerPrefersCreateCrushingBeforeMilling(helper);
    }

    @GameTest(template = TEMPLATE, batch = GRIND_RUNNER_ISOLATED_BATCH)
    public static void grindRunnerProcessesCreateDepotItems(GameTestHelper helper) {
        GrindRunnerScenarios.grindRunnerProcessesCreateDepotItems(helper);
    }

    @GameTest(template = TEMPLATE, batch = GRIND_RUNNER_ISOLATED_BATCH)
    public static void grindRunnerLeavesCreateChuteItemsUnprocessed(GameTestHelper helper) {
        GrindRunnerScenarios.grindRunnerLeavesCreateChuteItemsUnprocessed(helper);
    }
}
