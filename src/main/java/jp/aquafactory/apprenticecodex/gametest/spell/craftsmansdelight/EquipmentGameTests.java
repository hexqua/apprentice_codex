package jp.aquafactory.apprenticecodex.gametest.spell.craftsmansdelight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.CRAFTSMANS_DELIGHT_GRACED_RAIN_DENYLIST_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void gracedRainCastingMobilityFollowsEquipmentAndCastLifecycle(GameTestHelper helper) {
        CraftsmansDelightCastingMobilityGameTestScenarios.gracedRainCastingMobilityFollowsEquipmentAndCastLifecycle(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaMendingCastingMobilityFollowsEquipmentAndCastLifecycle(GameTestHelper helper) {
        CraftsmansDelightCastingMobilityGameTestScenarios.manaMendingCastingMobilityFollowsEquipmentAndCastLifecycle(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void harvestMoonCastingMobilityFollowsEquipmentAndCastLifecycle(GameTestHelper helper) {
        CraftsmansDelightCastingMobilityGameTestScenarios.harvestMoonCastingMobilityFollowsEquipmentAndCastLifecycle(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightGracedRainAcceleratesBabyGrowth(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.craftsmansDelightGracedRainAcceleratesBabyGrowth(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightGracedRainReducesBreedingCooldown(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.craftsmansDelightGracedRainReducesBreedingCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void gracedRainWithoutCraftsmansDelightLeavesMobAgeUnchanged(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.gracedRainWithoutCraftsmansDelightLeavesMobAgeUnchanged(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void gracedRainUndeadTargetsKeepDamageBehavior(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.gracedRainUndeadTargetsKeepDamageBehavior(helper);
    }

    @GameTest(template = TEMPLATE, batch = CRAFTSMANS_DELIGHT_GRACED_RAIN_DENYLIST_CONFIG_BATCH)
    public static void craftsmansDelightGracedRainGrowthDenylistBlocksOnlyGrowth(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.craftsmansDelightGracedRainGrowthDenylistBlocksOnlyGrowth(helper);
    }

    @GameTest(template = TEMPLATE, batch = CRAFTSMANS_DELIGHT_GRACED_RAIN_DENYLIST_CONFIG_BATCH)
    public static void craftsmansDelightGracedRainBreedingCooldownDenylistBlocksOnlyCooldown(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.craftsmansDelightGracedRainBreedingCooldownDenylistBlocksOnlyCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightGracedRainDoesNotReduceAllayDuplicationCooldown(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.craftsmansDelightGracedRainDoesNotReduceAllayDuplicationCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void gracedRainHealingPostsSpellHealEvent(GameTestHelper helper) {
        CraftsmansDelightGracedRainGameTestScenarios.gracedRainHealingPostsSpellHealEvent(helper);
    }
}
