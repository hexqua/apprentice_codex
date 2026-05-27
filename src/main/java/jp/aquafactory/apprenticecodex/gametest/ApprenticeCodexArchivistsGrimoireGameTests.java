package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexArchivistsGrimoireGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private ApprenticeCodexArchivistsGrimoireGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireInventoryKeepsOnlyScrollsAndPersists(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.archivistsGrimoireInventoryKeepsOnlyScrollsAndPersists(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireSelectedRowNavigationUsesPopulatedRowsOnly(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.archivistsGrimoireSelectedRowNavigationUsesPopulatedRowsOnly(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireVisibleSpellsExposeOnlySelectedRow(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.archivistsGrimoireVisibleSpellsExposeOnlySelectedRow(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireLockedRowsHideLegacyScrolls(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.archivistsGrimoireLockedRowsHideLegacyScrolls(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireTooltipShowsInscribeHintOnlyWhenEmpty(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.archivistsGrimoireTooltipShowsInscribeHintOnlyWhenEmpty(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireTooltipWarnsAboutLockedScrolls(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.archivistsGrimoireTooltipWarnsAboutLockedScrolls(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireWorkbenchUpgradePreservesStoredScrolls(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.archivistsGrimoireWorkbenchUpgradePreservesStoredScrolls(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireConfigMaxRowsCannotFallBelowInitialRows(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.archivistsGrimoireConfigMaxRowsCannotFallBelowInitialRows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireCurioAndUpgradeContractsStayRegistered(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.archivistsGrimoireCurioAndUpgradeContractsStayRegistered(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireSpellSelectionManagerReadsVisibleRow(GameTestHelper helper) {
        ApprenticeCodexGameTestScenarios.archivistsGrimoireSpellSelectionManagerReadsVisibleRow(helper);
    }
}
