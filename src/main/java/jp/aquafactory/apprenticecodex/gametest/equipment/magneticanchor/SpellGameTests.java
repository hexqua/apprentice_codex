package jp.aquafactory.apprenticecodex.gametest.equipment.magneticanchor;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.AUTO_MAGNET_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void magneticStabilityAnchorProtectsItemsByPositionWithoutBlockingOwner(GameTestHelper helper) {
        MagneticStabilityAnchorGameTestScenarios.protectsItemsByPositionWithoutBlockingOwner(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void magneticStabilityAnchorRemovalRestoresCollectionAndExperienceRemainsUnaffected(GameTestHelper helper) {
        MagneticStabilityAnchorGameTestScenarios.removalRestoresItemCollectionAndExperienceRemainsUnaffected(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void magneticStabilityAnchorProtectsItemsAcrossChunkBoundary(GameTestHelper helper) {
        MagneticStabilityAnchorGameTestScenarios.protectsItemsAcrossChunkBoundary(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void magneticStabilityAnchorUnregistersOnlyRemovedAnchorWithinChunk(GameTestHelper helper) {
        MagneticStabilityAnchorGameTestScenarios.unregistersOnlyRemovedAnchorWithinChunk(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void magneticStabilityAnchorSupportsWaterloggingAndAlwaysDropsPlainItem(GameTestHelper helper) {
        MagneticStabilityAnchorGameTestScenarios.supportsWaterloggingAndAlwaysDropsPlainItem(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_MAGNET_ISOLATED_BATCH)
    public static void magneticStabilityAnchorBlockItemPlacementPreservesWaterSource(GameTestHelper helper) {
        MagneticStabilityAnchorGameTestScenarios.blockItemPlacementPreservesWaterSource(helper);
    }
}
