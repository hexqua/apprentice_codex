package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.profile;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.MULTICAST_ECHO_STAFF_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EchoStaffAttackGameTests {
    private EchoStaffAttackGameTests() {}

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 40)
    public static void multicastEchoStaffAttackProfileScalesDirectCombatToolsDamage(GameTestHelper helper) {
        EchoStaffAttackScenarios.multicastEchoStaffAttackProfileScalesDirectCombatToolsDamage(helper);
    }

    @GameTest(template = TEMPLATE, batch = MULTICAST_ECHO_STAFF_ISOLATED_BATCH, timeoutTicks = 40)
    public static void multicastEchoStaffAttackProfileTracksDelayedProjectileDamage(GameTestHelper helper) {
        EchoStaffAttackScenarios.multicastEchoStaffAttackProfileTracksDelayedProjectileDamage(helper);
    }
}
