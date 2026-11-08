package jp.aquafactory.apprenticecodex.gametest.spell.healingbloom;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.healing_bloom_isolated")
    public static void healingBloomLightHasReducedLevelAndNoOutline(GameTestHelper helper) {
        HealingBloomScenarios.healingBloomLightHasReducedLevelAndNoOutline(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.healing_bloom_isolated")
    public static void healingBloomLightSelfCleansWithoutBloom(GameTestHelper helper) {
        HealingBloomScenarios.healingBloomLightSelfCleansWithoutBloom(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.healing_bloom_isolated")
    public static void healingBloomAcceptsOwnerDamageAndStaysSavable(GameTestHelper helper) {
        HealingBloomScenarios.healingBloomAcceptsOwnerDamageAndStaysSavable(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.healing_bloom_isolated")
    public static void healingBloomRootLossUsesDeathState(GameTestHelper helper) {
        HealingBloomScenarios.healingBloomRootLossUsesDeathState(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.healing_bloom_isolated")
    public static void healingBloomDeathDropsOnlyStoredFruitWithoutPlantingBush(GameTestHelper helper) {
        HealingBloomScenarios.healingBloomDeathDropsOnlyStoredFruitWithoutPlantingBush(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.healing_bloom_isolated")
    public static void healingBloomImmediateDeathDropsNothingAndPlantsNoBush(GameTestHelper helper) {
        HealingBloomScenarios.healingBloomImmediateDeathDropsNothingAndPlantsNoBush(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.healing_bloom_isolated")
    public static void healingBloomSkipsSelfRegenerationAndUsesSlowNaturalHealing(GameTestHelper helper) {
        HealingBloomScenarios.healingBloomSkipsSelfRegenerationAndUsesSlowNaturalHealing(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.healing_bloom_isolated")
    public static void healingBloomCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        HealingBloomScenarios.healingBloomCanBePlacedOnSupportedSlab(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.healing_bloom_isolated")
    public static void healingBloomNormalRecastFailsForSameOwner(GameTestHelper helper) {
        HealingBloomScenarios.healingBloomNormalRecastFailsForSameOwner(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.healing_bloom_isolated")
    public static void healingBloomAllowsDifferentOwnersToEachHaveOne(GameTestHelper helper) {
        HealingBloomScenarios.healingBloomAllowsDifferentOwnersToEachHaveOne(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.healing_bloom_isolated")
    public static void healingBloomMissingManagedBloomDoesNotBlockRecast(GameTestHelper helper) {
        HealingBloomScenarios.healingBloomMissingManagedBloomDoesNotBlockRecast(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.healing_bloom_isolated")
    public static void healingBloomOfflineDeathDoesNotBlockRecast(GameTestHelper helper) {
        HealingBloomScenarios.healingBloomOfflineDeathDoesNotBlockRecast(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.healing_bloom_isolated")
    public static void healingBloomSneakCastRecoversOfflineDeathState(GameTestHelper helper) {
        HealingBloomScenarios.healingBloomSneakCastRecoversOfflineDeathState(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.healing_bloom_isolated")
    public static void healingBloomSneakCastReplacesOnlyOwnersPreviousBloom(GameTestHelper helper) {
        HealingBloomScenarios.healingBloomSneakCastReplacesOnlyOwnersPreviousBloom(helper);
    }
}
