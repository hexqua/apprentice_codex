package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 80)
    public static void multicastEchoStaffRepeatedFortifyClearsTargetAreaIndicator(GameTestHelper helper) {
        MulticastEchoStaffScenarios.multicastEchoStaffRepeatedFortifyClearsTargetAreaIndicator(helper);
    }

}
