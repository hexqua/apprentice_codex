package jp.aquafactory.apprenticecodex.gametest.spell.thermalslice;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.KATANA_AREA_HIT_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void thermalSliceAppliesThermalSunderedAfterDamage(GameTestHelper helper) {
        ThermalSunderedGameTestScenarios.thermalSliceAppliesThermalSunderedAfterDamage(helper);
    }

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void thermalSliceSkipsProtectedAndRejectedTargets(GameTestHelper helper) {
        ThermalSunderedGameTestScenarios.thermalSliceSkipsProtectedAndRejectedTargets(helper);
    }

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void thermalSunderedRefreshesWithoutDowngrading(GameTestHelper helper) {
        ThermalSunderedGameTestScenarios.thermalSunderedRefreshesWithoutDowngrading(helper);
    }

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void thermalSliceDashesFixedDistanceAndCanOvershootCloseTarget(GameTestHelper helper) {
        ThermalSliceMovementGameTestScenarios.thermalSliceDashesFixedDistanceAndCanOvershootCloseTarget(helper);
    }

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void thermalSliceStopsAtWallAndSlashesEarly(GameTestHelper helper) {
        ThermalSliceMovementGameTestScenarios.thermalSliceStopsAtWallAndSlashesEarly(helper);
    }

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void thermalSliceKeepsDashDirectionButUsesFinalLookForSlash(GameTestHelper helper) {
        ThermalSliceMovementGameTestScenarios.thermalSliceKeepsDashDirectionButUsesFinalLookForSlash(helper);
    }

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void thermalSlicePreservesVerticalMovementAndFallDistance(GameTestHelper helper) {
        ThermalSliceMovementGameTestScenarios.thermalSlicePreservesVerticalMovementAndFallDistance(helper);
    }
}
