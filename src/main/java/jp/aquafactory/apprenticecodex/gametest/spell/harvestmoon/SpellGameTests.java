package jp.aquafactory.apprenticecodex.gametest.spell.harvestmoon;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.harvest_moon_isolated")
    public static void harvestMoonResetsMatureNetherWartAndPullsDrops(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonResetsMatureNetherWartAndPullsDrops(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.harvest_moon_isolated")
    public static void harvestMoonWithCraftsmansDelightShearsSheepWithoutShears(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonWithCraftsmansDelightShearsSheepWithoutShears(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.harvest_moon_isolated")
    public static void harvestMoonWithoutCraftsmansDelightDoesNotShearSheep(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonWithoutCraftsmansDelightDoesNotShearSheep(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.harvest_moon_sheep_snapshot")
    public static void harvestMoonSheepTargetsAreFixedAtCastAndRecheckedWhenProcessed(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonSheepTargetsAreFixedAtCastAndRecheckedWhenProcessed(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.harvest_moon_isolated")
    public static void harvestMoonHarvestsFarmersDelightTomatoViaRightClick(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonHarvestsFarmersDelightTomatoViaRightClick(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.harvest_moon_isolated")
    public static void harvestMoonKeepsFarmersDelightTomatoRopeState(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonKeepsFarmersDelightTomatoRopeState(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.harvest_moon_isolated")
    public static void harvestMoonHarvestsStemFruitWithoutBreakingStem(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonHarvestsStemFruitWithoutBreakingStem(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.harvest_moon_isolated", timeoutTicks = 120)
    public static void harvestMoonProcessesTargetsAcrossMultipleTicksAndKeepsBambooRoot(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonProcessesTargetsAcrossMultipleTicksAndKeepsBambooRoot(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.harvest_moon_isolated")
    public static void harvestMoonHarvestsKelpColumnBeyondInitialYSlice(GameTestHelper helper) {
        HarvestMoonScenarios.harvestMoonHarvestsKelpColumnBeyondInitialYSlice(helper);
    }
}
