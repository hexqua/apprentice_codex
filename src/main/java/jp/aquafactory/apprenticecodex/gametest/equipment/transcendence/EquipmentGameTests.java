package jp.aquafactory.apprenticecodex.gametest.equipment.transcendence;

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
    public static void transcendenceScrollBonusIsFixedAndPreservesLegacyData(GameTestHelper helper) {
        TranscendenceGameTestScenarios.scrollBonusIsFixedAndPreservesLegacyData(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void transcendenceLegacyEquipmentDoesNotModifySpellLevels(GameTestHelper helper) {
        TranscendenceGameTestScenarios.legacyEquipmentDoesNotModifySpellLevels(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void transcendenceInternalBonusStacksWithAffinityOnly(GameTestHelper helper) {
        TranscendenceGameTestScenarios.internalBonusStacksWithAffinityOnly(helper);
    }
}
