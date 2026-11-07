package jp.aquafactory.apprenticecodex.gametest.equipment.armor;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementMaidenRobeGameTests {
    private ElementMaidenRobeGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void elementMaidenRobeKeepsExpectedStatsAndImbueSurface(GameTestHelper helper) {
        ElementMaidenRobeGameTestScenarios.elementMaidenRobeKeepsExpectedStatsAndImbueSurface(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementMaidenRobeSchoolSpellPowerDistributesSpellbookSchools(GameTestHelper helper) {
        ElementMaidenRobeGameTestScenarios.elementMaidenRobeSchoolSpellPowerDistributesSpellbookSchools(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementMaidenRobeSchoolSpellPowerSplitsEmptySlotsBetweenTiedSchools(GameTestHelper helper) {
        ElementMaidenRobeGameTestScenarios.elementMaidenRobeSchoolSpellPowerSplitsEmptySlotsBetweenTiedSchools(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementMaidenRobeSchoolSpellPowerIgnoresHandsAndZeroConfig(GameTestHelper helper) {
        ElementMaidenRobeGameTestScenarios.elementMaidenRobeSchoolSpellPowerIgnoresHandsAndZeroConfig(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementMaidenRobeSchoolSpellPowerRefreshesArchivistsAndEnderGrimoireSources(GameTestHelper helper) {
        ElementMaidenRobeGameTestScenarios.elementMaidenRobeSchoolSpellPowerRefreshesArchivistsAndEnderGrimoireSources(helper);
    }
}
