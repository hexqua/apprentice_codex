package jp.aquafactory.apprenticecodex.gametest.spell.mirageavoidance;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mirage_avoidance_isolated")
    public static void mirageAvoidanceUsesFifteenTickInvulnerabilityAndActiveRecastLock(GameTestHelper helper) {
        MirageAvoidanceScenarios.mirageAvoidanceUsesFifteenTickInvulnerabilityAndActiveRecastLock(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mirage_avoidance_isolated")
    public static void mirageAvoidanceSupporterUsesCastTimeEquipmentAndTimingBoundaries(GameTestHelper helper) {
        MirageAvoidanceScenarios.mirageAvoidanceSupporterUsesCastTimeEquipmentAndTimingBoundaries(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mirage_avoidance_isolated", timeoutTicks = 80)
    public static void mirageAvoidanceFreezesThenSlidesAndResetsFallDistance(GameTestHelper helper) {
        MirageAvoidanceScenarios.mirageAvoidanceFreezesThenSlidesAndResetsFallDistance(helper);
    }
}
