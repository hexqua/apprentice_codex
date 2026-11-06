package jp.aquafactory.apprenticecodex.gametest.spell.linearbuild;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildUsesBaseAndCraftsmansDelightRanges(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildUsesBaseAndCraftsmansDelightRanges(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildConsumesConfiguredManaPerPlacedBlock(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildConsumesConfiguredManaPerPlacedBlock(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildCraftsmansDelightDiscountsManaPerBlock(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCraftsmansDelightDiscountsManaPerBlock(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildStopsPartwayWhenManaIsDepleted(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildStopsPartwayWhenManaIsDepleted(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildZeroManaCostConfigAllowsPlacement(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildZeroManaCostConfigAllowsPlacement(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildScrollCastDoesNotConsumeAdditionalMana(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildScrollCastDoesNotConsumeAdditionalMana(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildPlacesUntilPlayerAxis(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildPlacesUntilPlayerAxis(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildTriesOneBlockWhenFirstPlacementTouchesPlayerAxis(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildTriesOneBlockWhenFirstPlacementTouchesPlayerAxis(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildUpwardFromPlayerYBlockPlacesOnlyOne(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildUpwardFromPlayerYBlockPlacesOnlyOne(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildPrefersOffhandBlockTemplate(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildPrefersOffhandBlockTemplate(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildUsesOffhandLuminousDeviceBeforeShulkerSource(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildUsesOffhandLuminousDeviceBeforeShulkerSource(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildConsumesSoulwovenPouchContents(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildConsumesSoulwovenPouchContents(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildConsumesLastRavenousPouchItem(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildConsumesLastRavenousPouchItem(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildUsesMalumPouchBeforeLuminousDevice(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildUsesMalumPouchBeforeLuminousDevice(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildMalumPouchRequiresMatchingComponents(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildMalumPouchRequiresMatchingComponents(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildResolvesZeroCountLuminousDeviceSelectionAndEmptyFallback(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildResolvesZeroCountLuminousDeviceSelectionAndEmptyFallback(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildConsumesReplaceablePlacedBlockTemplate(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildConsumesReplaceablePlacedBlockTemplate(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildRejectsLargeAndDenylistedTemplates(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildRejectsLargeAndDenylistedTemplates(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildRejectsOffhandTemplateWithoutMainHandFallback(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildRejectsOffhandTemplateWithoutMainHandFallback(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildSkipsBlockedPositionsByDefault(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildSkipsBlockedPositionsByDefault(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildAbortOnFailedPlacementConfigStopsAtBlockedPosition(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildAbortOnFailedPlacementConfigStopsAtBlockedPosition(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildCopiesTopSlabAndConsumesCompanionTrunkFirst(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCopiesTopSlabAndConsumesCompanionTrunkFirst(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildCopiesBottomSlabType(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCopiesBottomSlabType(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildDoesNotCopyDoubleSlabType(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildDoesNotCopyDoubleSlabType(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildCopiesFurnaceFacing(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCopiesFurnaceFacing(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildCopiesLogAxis(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCopiesLogAxis(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildCopiesPistonFacing(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCopiesPistonFacing(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildCopiesSpellDispenserFacing(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCopiesSpellDispenserFacing(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildKeepsShulkerBlockEntityTagContents(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildKeepsShulkerBlockEntityTagContents(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildCopiesStairFacingAndHalfOnly(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCopiesStairFacingAndHalfOnly(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildRecalculatesLastStairShape(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildRecalculatesLastStairShape(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildCreativeCopiesHeldBlockWithoutConsumingStorage(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildCreativeCopiesHeldBlockWithoutConsumingStorage(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildConsumesPersonalShelfWithoutNearbyShelf(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildConsumesPersonalShelfWithoutNearbyShelf(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildPrefersEnderChestBeforeCreateToolbox(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildPrefersEnderChestBeforeCreateToolbox(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildConsumesPlacedCreateToolboxBeforeCompanionTrunk(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildConsumesPlacedCreateToolboxBeforeCompanionTrunk(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildIgnoresInventoryCreateToolboxAfterPlacedToolboxMisses(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildIgnoresInventoryCreateToolboxAfterPlacedToolboxMisses(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildShulkerSourceFollowsServerConfig(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildShulkerSourceFollowsServerConfig(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildShulkerSourceKeepsSlotAfterPartialConsume(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildShulkerSourceKeepsSlotAfterPartialConsume(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.linear_build_isolated")
    public static void linearBuildBundleSourceFollowsServerConfig(GameTestHelper helper) {
        LinearBuildScenarios.linearBuildBundleSourceFollowsServerConfig(helper);
    }
}
