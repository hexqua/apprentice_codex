package jp.aquafactory.apprenticecodex.gametest.spell.assistwings;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.assist_wings_isolated")
    public static void assistWingsRegularAirCastPreservesHorizontalMovementWithoutSelfMotionSync(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsRegularAirCastPreservesHorizontalMovementWithoutSelfMotionSync(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.assist_wings_isolated")
    public static void assistWingsTaggedGroundCastKeepsWingAndBlocksFallProtection(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsTaggedGroundCastKeepsWingAndBlocksFallProtection(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.assist_wings_isolated")
    public static void assistWingsTaggedOffhandBlocksFallProtectionWithoutDiscardingWing(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsTaggedOffhandBlocksFallProtectionWithoutDiscardingWing(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.assist_wings_isolated")
    public static void assistWingsRemovingTaggedItemRestoresFallProtectionWithoutSlowingFall(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsRemovingTaggedItemRestoresFallProtectionWithoutSlowingFall(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.assist_wings_isolated")
    public static void assistWingsTaggedLandingResetsAirJumpsAndAllowsNextAirCast(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsTaggedLandingResetsAirJumpsAndAllowsNextAirCast(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.assist_wings_isolated")
    public static void assistWingsWaterRemovalUsesGraceAndResetsAirJumps(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsWaterRemovalUsesGraceAndResetsAirJumps(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.assist_wings_isolated")
    public static void assistWingsLavaContactDoesNotRemoveWing(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsLavaContactDoesNotRemoveWing(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.assist_wings_isolated")
    public static void assistWingsSuccessfulCastRestartsRemovalGrace(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsSuccessfulCastRestartsRemovalGrace(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.assist_wings_isolated")
    public static void assistWingsRejectsOtherBroomsWithoutChangingState(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsRejectsOtherBroomsWithoutChangingState(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.assist_wings_isolated")
    public static void assistWingsHoverrideUsesSurfaceAndAirJumpCounts(GameTestHelper helper) {
        AssistWingsScenarios.assistWingsHoverrideUsesSurfaceAndAirJumpCounts(helper);
    }
}
