package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ScytheEpicFightGameTests {
    private static boolean enabled(GameTestHelper h) {
        if (ModList.get().isLoaded("epicfight")) return true;
        h.succeed();
        return false;
    }
    @GameTest(template = "gametest/basic_floor")
    public static void capabilityAndRightClickIsolation(GameTestHelper h) {
        if (enabled(h)) ScytheEpicFightTestHelper.capabilityAndRightClickIsolation(h);
    }
    @GameTest(template = "gametest/basic_floor")
    public static void acceptedAttackRecallsButRejectedAirAttackDoesNot(GameTestHelper h) {
        if (enabled(h)) ScytheEpicFightTestHelper.acceptedAttackRecallsButRejectedAirAttackDoesNot(h);
    }
    @GameTest(template = "gametest/basic_floor", timeoutTicks = 100)
    public static void equalEntityIdsKeepIndependentCharges(GameTestHelper h) {
        if (enabled(h)) ScytheEpicFightTestHelper.equalEntityIdsKeepIndependentCharges(h);
    }
    @GameTest(template = "gametest/basic_floor", timeoutTicks = 100)
    public static void chargeLaunchRecallAndMana(GameTestHelper h) {
        if (enabled(h)) ScytheEpicFightTestHelper.chargeLaunchRecallAndMana(h);
    }
    @GameTest(template = "gametest/basic_floor", timeoutTicks = 100)
    public static void interruptedChargesNeverLaunch(GameTestHelper h) {
        if (enabled(h)) ScytheEpicFightTestHelper.interruptedChargesNeverLaunch(h);
    }
    @GameTest(template = "gametest/basic_floor", timeoutTicks = 100)
    public static void chargeBoundariesAndInsufficientMana(GameTestHelper h) {
        if (enabled(h)) ScytheEpicFightTestHelper.chargeBoundariesAndInsufficientMana(h);
    }
    @GameTest(template = "gametest/basic_floor")
    public static void malumInstantSkillsAndRecallPriority(GameTestHelper h) {
        if (enabled(h)) ScytheEpicFightTestHelper.malumInstantSkillsAndRecallPriority(h);
    }
}
