package jp.aquafactory.apprenticecodex.gametest.spell.linearbuild;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.LINEAR_BUILD_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildUsesBaseAndCraftsmansDelightRanges(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildUsesBaseAndCraftsmansDelightRanges(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildConsumesConfiguredManaPerPlacedBlock(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildConsumesConfiguredManaPerPlacedBlock(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCraftsmansDelightDiscountsManaPerBlock(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCraftsmansDelightDiscountsManaPerBlock(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildStopsPartwayWhenManaIsDepleted(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildStopsPartwayWhenManaIsDepleted(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildZeroManaCostConfigAllowsPlacement(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildZeroManaCostConfigAllowsPlacement(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildScrollCastDoesNotConsumeAdditionalMana(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildScrollCastDoesNotConsumeAdditionalMana(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildPlacesUntilPlayerAxis(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildPlacesUntilPlayerAxis(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildTriesOneBlockWhenFirstPlacementTouchesPlayerAxis(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildTriesOneBlockWhenFirstPlacementTouchesPlayerAxis(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildUpwardFromPlayerYBlockPlacesOnlyOne(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildUpwardFromPlayerYBlockPlacesOnlyOne(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildPrefersOffhandBlockTemplate(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildPrefersOffhandBlockTemplate(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildUsesOffhandLuminousDeviceBeforeShulkerSource(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildUsesOffhandLuminousDeviceBeforeShulkerSource(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildResolvesZeroCountLuminousDeviceSelectionAndEmptyFallback(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildResolvesZeroCountLuminousDeviceSelectionAndEmptyFallback(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildConsumesReplaceablePlacedBlockTemplate(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildConsumesReplaceablePlacedBlockTemplate(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildRejectsLargeAndDenylistedTemplates(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildRejectsLargeAndDenylistedTemplates(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildRejectsOffhandTemplateWithoutMainHandFallback(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildRejectsOffhandTemplateWithoutMainHandFallback(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildSkipsBlockedPositionsByDefault(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildSkipsBlockedPositionsByDefault(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildAbortOnFailedPlacementConfigStopsAtBlockedPosition(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildAbortOnFailedPlacementConfigStopsAtBlockedPosition(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCopiesTopSlabAndConsumesCompanionTrunkFirst(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCopiesTopSlabAndConsumesCompanionTrunkFirst(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCopiesBottomSlabType(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCopiesBottomSlabType(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildDoesNotCopyDoubleSlabType(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildDoesNotCopyDoubleSlabType(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCopiesFurnaceFacing(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCopiesFurnaceFacing(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCopiesLogAxis(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCopiesLogAxis(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCopiesPistonFacing(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCopiesPistonFacing(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCopiesSpellDispenserFacing(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCopiesSpellDispenserFacing(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildKeepsShulkerBlockEntityTagContents(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildKeepsShulkerBlockEntityTagContents(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCopiesStairFacingAndHalfOnly(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCopiesStairFacingAndHalfOnly(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildRecalculatesLastStairShape(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildRecalculatesLastStairShape(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildCreativeCopiesHeldBlockWithoutConsumingStorage(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCreativeCopiesHeldBlockWithoutConsumingStorage(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildConsumesPersonalShelfWithoutNearbyShelf(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildConsumesPersonalShelfWithoutNearbyShelf(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildPrefersEnderChestBeforeCreateToolbox(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildPrefersEnderChestBeforeCreateToolbox(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildConsumesPlacedCreateToolboxBeforeCompanionTrunk(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildConsumesPlacedCreateToolboxBeforeCompanionTrunk(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildIgnoresInventoryCreateToolboxAfterPlacedToolboxMisses(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildIgnoresInventoryCreateToolboxAfterPlacedToolboxMisses(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildShulkerSourceFollowsServerConfig(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildShulkerSourceFollowsServerConfig(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildShulkerSourceKeepsSlotAfterPartialConsume(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildShulkerSourceKeepsSlotAfterPartialConsume(helper);
    }

    @GameTest(template = TEMPLATE, batch = LINEAR_BUILD_ISOLATED_BATCH)
    public static void linearBuildBundleSourceFollowsServerConfig(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildBundleSourceFollowsServerConfig(helper);
    }
}
