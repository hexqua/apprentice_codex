package jp.aquafactory.apprenticecodex.gametest.spell.wisdomplunder;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void newlyUnifiedHeldTargetsApplyWisdomAndPlunder(GameTestHelper helper) {
        WisdomPlunderEffectGameTestScenarios.newlyUnifiedHeldTargetsApplyWisdomAndPlunder(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void equipmentOnlyAndUnsupportedWisdomPlunderItemsStayInactiveWhileHeld(GameTestHelper helper) {
        WisdomPlunderEffectGameTestScenarios.equipmentOnlyAndUnsupportedItemsDoNotApplyWhileHeld(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void circuitHeatStaffKeepsVanillaLooting(GameTestHelper helper) {
        WisdomPlunderEffectGameTestScenarios.circuitHeatStaffKeepsVanillaLooting(helper);
    }
}
