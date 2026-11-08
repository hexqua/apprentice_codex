package jp.aquafactory.apprenticecodex.gametest.spell.automagnet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.AUTO_MAGNET_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void autoMagnetCollectsItemsWithoutSolegnoliaBlock(GameTestHelper helper) {
        AutoMagnetScenarios.autoMagnetCollectsItemsWithoutSolegnoliaBlock(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void autoMagnetFamiliarUsesTransientAlwaysTickingLifecycle(GameTestHelper helper) {
        AutoMagnetScenarios.autoMagnetFamiliarUsesTransientAlwaysTickingLifecycle(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void autoMagnetNormalModeCollectsWhileStanding(GameTestHelper helper) {
        AutoMagnetScenarios.autoMagnetNormalModeCollectsWhileStanding(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void autoMagnetNormalModeStopsWhileCrouching(GameTestHelper helper) {
        AutoMagnetScenarios.autoMagnetNormalModeStopsWhileCrouching(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void autoMagnetReverseModeStopsWhileStanding(GameTestHelper helper) {
        AutoMagnetScenarios.autoMagnetReverseModeStopsWhileStanding(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void autoMagnetReverseModeCollectsWhileCrouching(GameTestHelper helper) {
        AutoMagnetScenarios.autoMagnetReverseModeCollectsWhileCrouching(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void autoMagnetRecastSwitchesModeAndStopsSameMode(GameTestHelper helper) {
        AutoMagnetScenarios.autoMagnetRecastSwitchesModeAndStopsSameMode(helper);
    }
}
