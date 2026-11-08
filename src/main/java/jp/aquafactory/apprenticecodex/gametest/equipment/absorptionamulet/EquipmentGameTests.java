package jp.aquafactory.apprenticecodex.gametest.equipment.absorptionamulet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.absorption_amplify_amulet_config")
    public static void absorptionAmplifyAmuletZeroRecoveryDelayRepairsResumeToCurrentTick(GameTestHelper helper) {
        AbsorptionAmuletScenarios.absorptionAmplifyAmuletZeroRecoveryDelayRepairsResumeToCurrentTick(helper);
    }
}
