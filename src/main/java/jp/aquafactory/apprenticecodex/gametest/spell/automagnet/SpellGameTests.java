package jp.aquafactory.apprenticecodex.gametest.spell.automagnet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_magnet_isolated")
    public static void autoMagnetCollectsItemsWithoutSolegnoliaBlock(GameTestHelper helper) {
        AutoMagnetScenarios.autoMagnetCollectsItemsWithoutSolegnoliaBlock(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_magnet_isolated")
    public static void autoMagnetFamiliarUsesTransientAlwaysTickingLifecycle(GameTestHelper helper) {
        AutoMagnetScenarios.autoMagnetFamiliarUsesTransientAlwaysTickingLifecycle(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_magnet_isolated")
    public static void autoMagnetNormalModeCollectsWhileStanding(GameTestHelper helper) {
        AutoMagnetScenarios.autoMagnetNormalModeCollectsWhileStanding(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_magnet_isolated")
    public static void autoMagnetNormalModeStopsWhileCrouching(GameTestHelper helper) {
        AutoMagnetScenarios.autoMagnetNormalModeStopsWhileCrouching(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_magnet_isolated")
    public static void autoMagnetReverseModeStopsWhileStanding(GameTestHelper helper) {
        AutoMagnetScenarios.autoMagnetReverseModeStopsWhileStanding(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_magnet_isolated")
    public static void autoMagnetReverseModeCollectsWhileCrouching(GameTestHelper helper) {
        AutoMagnetScenarios.autoMagnetReverseModeCollectsWhileCrouching(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_magnet_isolated")
    public static void autoMagnetRecastSwitchesModeAndStopsSameMode(GameTestHelper helper) {
        AutoMagnetScenarios.autoMagnetRecastSwitchesModeAndStopsSameMode(helper);
    }
}
