package jp.aquafactory.apprenticecodex.gametest.spell.harvestmoon;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = HARVEST_MOON_ISOLATED_BATCH)
    public static void harvestMoonResetsMatureNetherWartAndPullsDrops(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonResetsMatureNetherWartAndPullsDrops(helper);
    }

    @GameTest(template = TEMPLATE, batch = HARVEST_MOON_ISOLATED_BATCH)
    public static void harvestMoonWithCraftsmansDelightShearsSheepWithoutShears(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonWithCraftsmansDelightShearsSheepWithoutShears(helper);
    }

    @GameTest(template = TEMPLATE, batch = HARVEST_MOON_ISOLATED_BATCH)
    public static void harvestMoonWithoutCraftsmansDelightDoesNotShearSheep(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonWithoutCraftsmansDelightDoesNotShearSheep(helper);
    }

    @GameTest(template = TEMPLATE, batch = HARVEST_MOON_SHEEP_SNAPSHOT_BATCH)
    public static void harvestMoonSheepTargetsAreFixedAtCastAndRecheckedWhenProcessed(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonSheepTargetsAreFixedAtCastAndRecheckedWhenProcessed(helper);
    }

    @GameTest(template = TEMPLATE, batch = HARVEST_MOON_ISOLATED_BATCH)
    public static void harvestMoonHarvestsFarmersDelightTomatoViaRightClick(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonHarvestsFarmersDelightTomatoViaRightClick(helper);
    }

    @GameTest(template = TEMPLATE, batch = HARVEST_MOON_ISOLATED_BATCH)
    public static void harvestMoonKeepsFarmersDelightTomatoRopeState(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonKeepsFarmersDelightTomatoRopeState(helper);
    }

    @GameTest(template = TEMPLATE, batch = HARVEST_MOON_ISOLATED_BATCH)
    public static void harvestMoonHarvestsStemFruitWithoutBreakingStem(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonHarvestsStemFruitWithoutBreakingStem(helper);
    }

    @GameTest(template = TEMPLATE, batch = HARVEST_MOON_ISOLATED_BATCH, timeoutTicks = 120)
    public static void harvestMoonProcessesTargetsAcrossMultipleTicksAndKeepsBambooRoot(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonProcessesTargetsAcrossMultipleTicksAndKeepsBambooRoot(helper);
    }

    @GameTest(template = TEMPLATE, batch = HARVEST_MOON_ISOLATED_BATCH)
    public static void harvestMoonHarvestsKelpColumnBeyondInitialYSlice(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonHarvestsKelpColumnBeyondInitialYSlice(helper);
    }
}
