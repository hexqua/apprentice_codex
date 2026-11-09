package jp.aquafactory.apprenticecodex.gametest.equipment.spellbook;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ArchivistsGrimoireGameTests {
    private ArchivistsGrimoireGameTests() {}

    static final String TEMPLATE = "gametest/basic_floor";

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireInventoryKeepsOnlyScrollsAndPersists(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireInventoryKeepsOnlyScrollsAndPersists(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireSelectedRowNavigationUsesPopulatedRowsOnly(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireSelectedRowNavigationUsesPopulatedRowsOnly(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireVisibleSpellsExposeOnlySelectedRow(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireVisibleSpellsExposeOnlySelectedRow(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireLockedRowsHideLegacyScrolls(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireLockedRowsHideLegacyScrolls(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireTooltipShowsInscribeHintOnlyWhenEmpty(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireTooltipShowsInscribeHintOnlyWhenEmpty(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireTooltipWarnsAboutLockedScrolls(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireTooltipWarnsAboutLockedScrolls(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireWorkbenchUpgradePreservesStoredScrolls(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireWorkbenchUpgradePreservesStoredScrolls(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireConfigMaxRowsCannotFallBelowInitialRows(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireConfigMaxRowsCannotFallBelowInitialRows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireCurioAndUpgradeContractsStayRegistered(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireCurioAndUpgradeContractsStayRegistered(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void archivistsGrimoireSpellSelectionManagerReadsVisibleRow(GameTestHelper helper) {
        ArchivistsGrimoireScenarios.archivistsGrimoireSpellSelectionManagerReadsVisibleRow(helper);
    }
}
