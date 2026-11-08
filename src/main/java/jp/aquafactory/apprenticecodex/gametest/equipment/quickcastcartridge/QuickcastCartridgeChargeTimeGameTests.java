package jp.aquafactory.apprenticecodex.gametest.equipment.quickcastcartridge;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.QuickcastCartridgeChargeState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class QuickcastCartridgeChargeTimeGameTests {
    private QuickcastCartridgeChargeTimeGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.quickcast_cartridge_charge")
    public static void quickcastRecoveryRoundsUpRespectsTheMinimumAndSaturates(GameTestHelper helper) {
        helper.assertTrue(QuickcastCartridgeChargeState.recoveryTicks(101, 1.5, 60) == 152,
                "Recovery must ceil the scaled full cooldown");
        helper.assertTrue(QuickcastCartridgeChargeState.recoveryTicks(0, 1, 60) == 60,
                "Recovery must respect the minimum");
        helper.assertTrue(QuickcastCartridgeChargeState.recoveryTicks(Integer.MAX_VALUE, Double.MAX_VALUE, 60) == Long.MAX_VALUE,
                "Recovery multiplication must saturate");
        helper.assertTrue(QuickcastCartridgeChargeState.addTime(Long.MAX_VALUE - 1, 60) == Long.MAX_VALUE,
                "Recovery timestamp addition must saturate");
        helper.succeed();
    }
}
