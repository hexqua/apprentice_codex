package jp.aquafactory.apprenticecodex.gametest.spell.craftsmansdelight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void gracedRainCastingMobilityFollowsEquipmentAndCastLifecycle(GameTestHelper helper) {
        CraftsmansDelightCastingMobilityGameTestScenarios.gracedRainCastingMobilityFollowsEquipmentAndCastLifecycle(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaMendingCastingMobilityFollowsEquipmentAndCastLifecycle(GameTestHelper helper) {
        CraftsmansDelightCastingMobilityGameTestScenarios.manaMendingCastingMobilityFollowsEquipmentAndCastLifecycle(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void harvestMoonCastingMobilityFollowsEquipmentAndCastLifecycle(GameTestHelper helper) {
        CraftsmansDelightCastingMobilityGameTestScenarios.harvestMoonCastingMobilityFollowsEquipmentAndCastLifecycle(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void craftsmansDelightGracedRainAcceleratesBabyGrowth(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.craftsmansDelightGracedRainAcceleratesBabyGrowth(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void craftsmansDelightGracedRainReducesBreedingCooldown(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.craftsmansDelightGracedRainReducesBreedingCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void gracedRainWithoutCraftsmansDelightLeavesMobAgeUnchanged(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.gracedRainWithoutCraftsmansDelightLeavesMobAgeUnchanged(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void gracedRainUndeadTargetsKeepDamageBehavior(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.gracedRainUndeadTargetsKeepDamageBehavior(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.craftsmans_delight_graced_rain_denylist_config")
    public static void craftsmansDelightGracedRainGrowthDenylistBlocksOnlyGrowth(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.craftsmansDelightGracedRainGrowthDenylistBlocksOnlyGrowth(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.craftsmans_delight_graced_rain_denylist_config")
    public static void craftsmansDelightGracedRainBreedingCooldownDenylistBlocksOnlyCooldown(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.craftsmansDelightGracedRainBreedingCooldownDenylistBlocksOnlyCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void craftsmansDelightGracedRainDoesNotReduceAllayDuplicationCooldown(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.craftsmansDelightGracedRainDoesNotReduceAllayDuplicationCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void gracedRainHealingPostsSpellHealEvent(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.gracedRainHealingPostsSpellHealEvent(helper);
    }
}
