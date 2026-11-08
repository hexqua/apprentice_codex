package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.session;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EchoStaffLimitGameTests {
    private EchoStaffLimitGameTests() {}

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_COOLDOWN_CAP_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffCooldownCapLimitsAdjustedCooldown(GameTestHelper helper) {
        EchoStaffLimitScenarios.multicastEchoStaffCooldownCapLimitsAdjustedCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_BASE_COOLDOWN_CAP_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffBaseCooldownAboveCapUsesOriginalCooldown(GameTestHelper helper) {
        EchoStaffLimitScenarios.multicastEchoStaffBaseCooldownAboveCapUsesOriginalCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = ECHO_CAST_MULTICAST_LIMIT_BATCH, timeoutTicks = 40)
    public static void echoCastStopsAtConfiguredMulticastLimit(GameTestHelper helper) {
        EchoStaffLimitScenarios.echoCastStopsAtConfiguredMulticastLimit(helper);
    }
}
