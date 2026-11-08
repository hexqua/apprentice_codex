package jp.aquafactory.apprenticecodex.gametest.equipment.transcendence;

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
    public static void transcendenceScrollBonusIsFixedAndPreservesLegacyData(GameTestHelper helper) {
        TranscendenceGameTestScenarios.scrollBonusIsFixedAndPreservesLegacyData(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void transcendenceLegacyEquipmentDoesNotModifySpellLevels(GameTestHelper helper) {
        TranscendenceGameTestScenarios.legacyEquipmentDoesNotModifySpellLevels(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void transcendenceInternalBonusStacksWithAffinityOnly(GameTestHelper helper) {
        TranscendenceGameTestScenarios.internalBonusStacksWithAffinityOnly(helper);
    }
}
