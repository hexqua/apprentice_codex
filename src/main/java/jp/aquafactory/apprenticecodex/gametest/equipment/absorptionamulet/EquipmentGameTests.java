package jp.aquafactory.apprenticecodex.gametest.equipment.absorptionamulet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.ABSORPTION_AMPLIFY_AMULET_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE, batch = ABSORPTION_AMPLIFY_AMULET_CONFIG_BATCH)
    public static void absorptionAmplifyAmuletZeroRecoveryDelayRepairsResumeToCurrentTick(GameTestHelper helper) {
        AbsorptionAmuletScenarios.absorptionAmplifyAmuletZeroRecoveryDelayRepairsResumeToCurrentTick(helper);
    }
}
