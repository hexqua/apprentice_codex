package jp.aquafactory.apprenticecodex.gametest.equipment.freecaststaff;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.menu.CalibrationImbueGameTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FreecastStaffLegacyScenarios {
    private FreecastStaffLegacyScenarios() {}

    @GameTest(template = TEMPLATE)
    public static void mithrilFreecastStaffCooldownUsesSelectedSource(GameTestHelper helper) {
        CalibrationImbueGameTests.mithrilFreecastStaffCooldownUsesSelectedSource(helper);
    }
}
