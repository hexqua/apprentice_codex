package jp.aquafactory.apprenticecodex.gametest.equipment.armor;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementMaidenRobeGameTests {
    private ElementMaidenRobeGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void elementMaidenRobeKeepsExpectedStatsAndImbueSurface(GameTestHelper helper) {
        ElementMaidenRobeGameTestScenarios.elementMaidenRobeKeepsExpectedStatsAndImbueSurface(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementMaidenRobeSchoolSpellPowerDistributesSpellbookSchools(GameTestHelper helper) {
        ElementMaidenRobeGameTestScenarios.elementMaidenRobeSchoolSpellPowerDistributesSpellbookSchools(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementMaidenRobeSchoolSpellPowerSplitsEmptySlotsBetweenTiedSchools(GameTestHelper helper) {
        ElementMaidenRobeGameTestScenarios.elementMaidenRobeSchoolSpellPowerSplitsEmptySlotsBetweenTiedSchools(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementMaidenRobeSchoolSpellPowerIgnoresHandsAndZeroConfig(GameTestHelper helper) {
        ElementMaidenRobeGameTestScenarios.elementMaidenRobeSchoolSpellPowerIgnoresHandsAndZeroConfig(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementMaidenRobeSchoolSpellPowerRefreshesArchivistsAndEnderGrimoireSources(GameTestHelper helper) {
        ElementMaidenRobeGameTestScenarios.elementMaidenRobeSchoolSpellPowerRefreshesArchivistsAndEnderGrimoireSources(helper);
    }
}
