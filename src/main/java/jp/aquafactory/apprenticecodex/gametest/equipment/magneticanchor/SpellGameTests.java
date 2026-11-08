package jp.aquafactory.apprenticecodex.gametest.equipment.magneticanchor;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_magnet_isolated")
    public static void magneticStabilityAnchorProtectsItemsByPositionWithoutBlockingOwner(GameTestHelper helper) {
        MagneticStabilityAnchorGameTestScenarios.protectsItemsByPositionWithoutBlockingOwner(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_magnet_isolated")
    public static void magneticStabilityAnchorRemovalRestoresCollectionAndExperienceRemainsUnaffected(GameTestHelper helper) {
        MagneticStabilityAnchorGameTestScenarios.removalRestoresItemCollectionAndExperienceRemainsUnaffected(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_magnet_isolated")
    public static void magneticStabilityAnchorProtectsItemsAcrossChunkBoundary(GameTestHelper helper) {
        MagneticStabilityAnchorGameTestScenarios.protectsItemsAcrossChunkBoundary(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_magnet_isolated")
    public static void magneticStabilityAnchorUnregistersOnlyRemovedAnchorWithinChunk(GameTestHelper helper) {
        MagneticStabilityAnchorGameTestScenarios.unregistersOnlyRemovedAnchorWithinChunk(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_magnet_isolated")
    public static void magneticStabilityAnchorSupportsWaterloggingAndAlwaysDropsPlainItem(GameTestHelper helper) {
        MagneticStabilityAnchorGameTestScenarios.supportsWaterloggingAndAlwaysDropsPlainItem(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_magnet_isolated")
    public static void magneticStabilityAnchorBlockItemPlacementPreservesWaterSource(GameTestHelper helper) {
        MagneticStabilityAnchorGameTestScenarios.blockItemPlacementPreservesWaterSource(helper);
    }
}
