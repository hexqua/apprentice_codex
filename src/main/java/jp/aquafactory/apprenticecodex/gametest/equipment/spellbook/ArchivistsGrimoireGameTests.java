package jp.aquafactory.apprenticecodex.gametest.equipment.spellbook;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ArchivistsGrimoireGameTests {
    private ArchivistsGrimoireGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void archivistsGrimoireInventoryKeepsOnlyScrollsAndPersists(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireInventoryKeepsOnlyScrollsAndPersists(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void archivistsGrimoireSelectedRowNavigationUsesPopulatedRowsOnly(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireSelectedRowNavigationUsesPopulatedRowsOnly(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void archivistsGrimoireVisibleSpellsExposeOnlySelectedRow(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireVisibleSpellsExposeOnlySelectedRow(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void archivistsGrimoireLockedRowsHideLegacyScrolls(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireLockedRowsHideLegacyScrolls(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void archivistsGrimoireTooltipShowsInscribeHintOnlyWhenEmpty(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireTooltipShowsInscribeHintOnlyWhenEmpty(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void archivistsGrimoireTooltipWarnsAboutLockedScrolls(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireTooltipWarnsAboutLockedScrolls(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void archivistsGrimoireWorkbenchUpgradePreservesStoredScrolls(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireWorkbenchUpgradePreservesStoredScrolls(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void archivistsGrimoireConfigMaxRowsCannotFallBelowInitialRows(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireConfigMaxRowsCannotFallBelowInitialRows(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void archivistsGrimoireCurioAndUpgradeContractsStayRegistered(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireCurioAndUpgradeContractsStayRegistered(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void archivistsGrimoireSpellSelectionManagerReadsVisibleRow(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireSpellSelectionManagerReadsVisibleRow(helper);
    }
}
