package jp.aquafactory.apprenticecodex.gametest.spell.autoturret;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.AUTO_TURRET_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class AutoTurretPlacementGameTests {
    private AutoTurretPlacementGameTests() {}

    @GameTest(template = TEMPLATE, batch = AUTO_TURRET_ISOLATED_BATCH)
    public static void autoTurretCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        AutoTurretPlacementScenarios.autoTurretCanBePlacedOnSupportedSlab(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_TURRET_ISOLATED_BATCH)
    public static void autoTurretCanBePlacedOnSupportedStairs(GameTestHelper helper) {
        AutoTurretPlacementScenarios.autoTurretCanBePlacedOnSupportedStairs(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_TURRET_ISOLATED_BATCH, timeoutTicks = 80)
    public static void autoTurretFallsWhenSupportRemoved(GameTestHelper helper) {
        AutoTurretPlacementScenarios.autoTurretFallsWhenSupportRemoved(helper);
    }
}
