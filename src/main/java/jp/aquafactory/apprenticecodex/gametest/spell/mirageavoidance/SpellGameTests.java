package jp.aquafactory.apprenticecodex.gametest.spell.mirageavoidance;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.MIRAGE_AVOIDANCE_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = MIRAGE_AVOIDANCE_ISOLATED_BATCH)
    public static void mirageAvoidanceUsesFifteenTickInvulnerabilityAndActiveRecastLock(GameTestHelper helper) {
        MirageAvoidanceScenarios.mirageAvoidanceUsesFifteenTickInvulnerabilityAndActiveRecastLock(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIRAGE_AVOIDANCE_ISOLATED_BATCH)
    public static void mirageAvoidanceSupporterUsesCastTimeEquipmentAndTimingBoundaries(GameTestHelper helper) {
        MirageAvoidanceScenarios.mirageAvoidanceSupporterUsesCastTimeEquipmentAndTimingBoundaries(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIRAGE_AVOIDANCE_ISOLATED_BATCH, timeoutTicks = 80)
    public static void mirageAvoidanceFreezesThenSlidesAndResetsFallDistance(GameTestHelper helper) {
        MirageAvoidanceScenarios.mirageAvoidanceFreezesThenSlidesAndResetsFallDistance(helper);
    }
}
