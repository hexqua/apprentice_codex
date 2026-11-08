package jp.aquafactory.apprenticecodex.gametest.equipment.zenithstaff;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.runtime.CoreGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void zenithStaffUsesStrongestSchoolPowerAndManaPenalty(GameTestHelper helper) {
        ZenithStaffScenarios.zenithStaffUsesStrongestSchoolPowerAndManaPenalty(helper);
    }
}
