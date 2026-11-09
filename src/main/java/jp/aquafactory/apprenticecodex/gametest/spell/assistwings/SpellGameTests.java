package jp.aquafactory.apprenticecodex.gametest.spell.assistwings;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.ASSIST_WINGS_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsRegularAirCastPreservesHorizontalMovementWithoutSelfMotionSync(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsRegularAirCastPreservesHorizontalMovementWithoutSelfMotionSync(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsTaggedGroundCastKeepsWingAndBlocksFallProtection(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsTaggedGroundCastKeepsWingAndBlocksFallProtection(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsTaggedOffhandBlocksFallProtectionWithoutDiscardingWing(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsTaggedOffhandBlocksFallProtectionWithoutDiscardingWing(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsRemovingTaggedItemRestoresFallProtectionWithoutSlowingFall(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsRemovingTaggedItemRestoresFallProtectionWithoutSlowingFall(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsTaggedLandingResetsAirJumpsAndAllowsNextAirCast(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsTaggedLandingResetsAirJumpsAndAllowsNextAirCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsWaterRemovalUsesGraceAndResetsAirJumps(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsWaterRemovalUsesGraceAndResetsAirJumps(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsLavaContactDoesNotRemoveWing(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsLavaContactDoesNotRemoveWing(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsSuccessfulCastRestartsRemovalGrace(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsSuccessfulCastRestartsRemovalGrace(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsRejectsOtherBroomsWithoutChangingState(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsRejectsOtherBroomsWithoutChangingState(helper);
    }

    @GameTest(template = TEMPLATE, batch = ASSIST_WINGS_ISOLATED_BATCH)
    public static void assistWingsHoverrideUsesSurfaceAndAirJumpCounts(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsHoverrideUsesSurfaceAndAirJumpCounts(helper);
    }
}
