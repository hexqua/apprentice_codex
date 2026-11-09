package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.session;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.MULTICAST_ECHO_STAFF_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EchoStaffSessionGameTests {
    private EchoStaffSessionGameTests() {}

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffInstantCastRunsAfterDelayAndAppliesPenaltyCooldown(GameTestHelper helper) {
        EchoStaffSessionScenarios.multicastEchoStaffInstantCastRunsAfterDelayAndAppliesPenaltyCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffCreativeCastSkipsFinalCooldown(GameTestHelper helper) {
        EchoStaffSessionScenarios.multicastEchoStaffCreativeCastSkipsFinalCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffInsufficientManaEndsWithPenaltyCooldown(GameTestHelper helper) {
        EchoStaffSessionScenarios.multicastEchoStaffInsufficientManaEndsWithPenaltyCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffItemChangeEndsWithPenaltyCooldown(GameTestHelper helper) {
        EchoStaffSessionScenarios.multicastEchoStaffItemChangeEndsWithPenaltyCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffLogoutEndsWithPenaltyCooldown(GameTestHelper helper) {
        EchoStaffSessionScenarios.multicastEchoStaffLogoutEndsWithPenaltyCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 80)
    public static void multicastEchoStaffLongCastAddsSkippedCastTimeCooldown(GameTestHelper helper) {
        EchoStaffSessionScenarios.multicastEchoStaffLongCastAddsSkippedCastTimeCooldown(helper);
    }
}
