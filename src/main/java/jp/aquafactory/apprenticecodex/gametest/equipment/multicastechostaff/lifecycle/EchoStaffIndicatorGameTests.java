package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.lifecycle;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.MULTICAST_ECHO_STAFF_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EchoStaffIndicatorGameTests {
    private EchoStaffIndicatorGameTests() {}

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffRepeatedFortifyClearsTargetAreaIndicator(GameTestHelper helper) {
        EchoStaffIndicatorScenarios.multicastEchoStaffRepeatedFortifyClearsTargetAreaIndicator(helper);
    }
}
