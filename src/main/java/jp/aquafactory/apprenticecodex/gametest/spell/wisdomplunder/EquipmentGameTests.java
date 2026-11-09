package jp.aquafactory.apprenticecodex.gametest.spell.wisdomplunder;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void newlyUnifiedHeldTargetsApplyWisdomAndPlunder(GameTestHelper helper) {
        WisdomPlunderEffectGameTestScenarios.newlyUnifiedHeldTargetsApplyWisdomAndPlunder(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void equipmentOnlyAndUnsupportedWisdomPlunderItemsStayInactiveWhileHeld(GameTestHelper helper) {
        WisdomPlunderEffectGameTestScenarios.equipmentOnlyAndUnsupportedItemsDoNotApplyWhileHeld(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void circuitHeatStaffKeepsVanillaLooting(GameTestHelper helper) {
        WisdomPlunderEffectGameTestScenarios.circuitHeatStaffKeepsVanillaLooting(helper);
    }
}
