package jp.aquafactory.apprenticecodex.gametest.spell.thermalslice;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void thermalSliceAppliesThermalSunderedAfterDamage(GameTestHelper helper) {
        ThermalSunderedGameTestScenarios.thermalSliceAppliesThermalSunderedAfterDamage(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void thermalSliceSkipsProtectedAndRejectedTargets(GameTestHelper helper) {
        ThermalSunderedGameTestScenarios.thermalSliceSkipsProtectedAndRejectedTargets(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void thermalSunderedRefreshesWithoutDowngrading(GameTestHelper helper) {
        ThermalSunderedGameTestScenarios.thermalSunderedRefreshesWithoutDowngrading(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void thermalSliceDashesFixedDistanceAndCanOvershootCloseTarget(GameTestHelper helper) {
        ThermalSliceMovementGameTestScenarios.thermalSliceDashesFixedDistanceAndCanOvershootCloseTarget(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void thermalSliceStopsAtWallAndSlashesEarly(GameTestHelper helper) {
        ThermalSliceMovementGameTestScenarios.thermalSliceStopsAtWallAndSlashesEarly(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void thermalSliceKeepsDashDirectionButUsesFinalLookForSlash(GameTestHelper helper) {
        ThermalSliceMovementGameTestScenarios.thermalSliceKeepsDashDirectionButUsesFinalLookForSlash(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void thermalSlicePreservesVerticalMovementAndFallDistance(GameTestHelper helper) {
        ThermalSliceMovementGameTestScenarios.thermalSlicePreservesVerticalMovementAndFallDistance(helper);
    }
}
