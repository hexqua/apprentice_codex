package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Snapshot;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsDisplayMotion;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsMotion;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // 描画計算を共通クラスに分離し、clientを起動せずに受信タイミングを検証する。
public final class PsychicDriveGauntletsMotionGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final Vec3 START = new Vec3(1, 2, 3);
    private static final Vec3 DESTINATION = START.add(0, 0, 6);
    private PsychicDriveGauntletsMotionGameTests() {}

    private static Snapshot flight(long launch, long tick) {
        return new Snapshot(Phase.OUTBOUND, PsychicDriveGauntletsMotion.outbound(START, DESTINATION, tick - launch),
                0, 0, 1, START, DESTINATION, launch, tick);
    }

    private static Snapshot stationary(Phase phase, Vec3 point, long tick) {
        return new Snapshot(phase, point, 0, 0, 1, START, DESTINATION, 100, tick, 1);
    }

    private static void equal(GameTestHelper helper, Vec3 actual, Vec3 expected, String message) {
        helper.assertTrue(actual.distanceTo(expected) < 1.0e-6, message + ": " + actual + " != " + expected);
    }

    @GameTest(template = TEMPLATE)
    public static void singleFlightAcceleratesWithoutChangingFullRangeDuration(GameTestHelper helper) {
        var first = PsychicDriveGauntletsMotion.outbound(START, DESTINATION, 1);
        var second = PsychicDriveGauntletsMotion.outbound(START, DESTINATION, 2);
        equal(helper, first, START.add(0, 0, 4.0 / 3.0), "First tick must use gentle acceleration");
        equal(helper, second, START.add(0, 0, 10.0 / 3.0), "Second tick must accelerate");
        equal(helper, PsychicDriveGauntletsMotion.outbound(START, DESTINATION, 3), DESTINATION,
                "Six-block travel must finish in three ticks");
        helper.assertTrue(first.distanceTo(START) < second.distanceTo(first)
                        && second.distanceTo(first) < DESTINATION.distanceTo(second),
                "Travel increments must increase toward the endpoint");
        equal(helper, PsychicDriveGauntletsMotion.outbound(START, DESTINATION, -1), START,
                "Negative elapsed time must not move behind the launch point");
        equal(helper, PsychicDriveGauntletsMotion.outbound(START, DESTINATION, 100), DESTINATION,
                "Late sampling must not overshoot");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nearTargetsUseSixBlockCurveRatherThanRescaling(GameTestHelper helper) {
        var near = START.add(0, 0, 3);
        equal(helper, PsychicDriveGauntletsMotion.outbound(START, near, 0.5),
                PsychicDriveGauntletsMotion.outbound(START, DESTINATION, 0.5),
                "Near and far attacks must share initial speed");
        equal(helper, PsychicDriveGauntletsMotion.outbound(START, near, 2), near,
                "A near target must stop the six-block curve early");
        equal(helper, PsychicDriveGauntletsMotion.outbound(START, START, 1), START,
                "Zero-length flight must remain finite");
        var extended = START.add(0, 0, 6.2);
        equal(helper, PsychicDriveGauntletsMotion.outbound(START, extended, 3), extended,
                "An offset arm path slightly beyond six blocks must still reach its target");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fractionalTickWrapAndMidTickPacketsDoNotJumpFlight(GameTestHelper helper) {
        var motion = new PsychicDriveGauntletsDisplayMotion(flight(100, 100), 1000.4);
        var beforeBoundary = motion.sample(1000.9).position();
        var afterBoundary = motion.sample(1001.1).position();
        helper.assertTrue(afterBoundary.z > beforeBoundary.z,
                "A fractional tick reset must not rewind flight");
        var beforePacket = motion.sample(1001.2).position();
        motion.accept(flight(100, 101), 1001.2);
        equal(helper, motion.sample(1001.2).position(), beforePacket,
                "A packet received mid-tick must not jump the displayed position");
        equal(helper, motion.sample(1001.4).position(),
                PsychicDriveGauntletsMotion.outbound(START, DESTINATION, 1.0),
                "Repeated packets must not restart the flight clock");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void coalescedAndDelayedPacketsKeepTheSameFlightClock(GameTestHelper helper) {
        var motion = new PsychicDriveGauntletsDisplayMotion(flight(100, 100), 10.25);
        var before = motion.sample(11.5).position();
        motion.accept(flight(100, 101), 11.5);
        motion.accept(flight(100, 102), 11.5);
        equal(helper, motion.sample(11.5).position(), before,
                "Coalesced packets must not teleport the arm");
        var later = motion.sample(12.5).position();
        equal(helper, motion.sample(12.0).position(), later,
                "An older render clock sample must not rewind the arm");
        equal(helper, motion.sample(20).position(), DESTINATION,
                "A missing update must not extrapolate beyond the fixed destination");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void impactAndReturnUpdatesRetargetFromTheDisplayedPosition(GameTestHelper helper) {
        var motion = new PsychicDriveGauntletsDisplayMotion(flight(100, 100), 0.25);
        var atImpact = motion.sample(0.75).position();
        var impact = START.add(0, 0, 1);
        motion.accept(stationary(Phase.HOLDING, impact, 101), 0.75);
        equal(helper, motion.sample(0.75).position(), atImpact,
                "Impact correction must start at the currently displayed point");
        equal(helper, motion.sample(1.75).position(), impact, "Impact correction must settle in one tick");
        var returning = impact.lerp(START, 0.2);
        motion.accept(stationary(Phase.RETURNING, returning, 111), 2.1);
        var beforePacket = motion.sample(2.6).position();
        motion.accept(stationary(Phase.RETURNING, impact.lerp(START, 0.4), 112), 2.6);
        equal(helper, motion.sample(2.6).position(), beforePacket,
                "Mid-tick return updates must not reset interpolation to a raw server position");
        var beforeComplete = motion.sample(3.0).position();
        motion.accept(stationary(Phase.IDLE, START, 115), 3.0);
        equal(helper, motion.sample(3.0).position(), beforeComplete,
                "Completion must interpolate its last segment");
        helper.assertTrue(!motion.finished(3.5), "Completion must keep drawing until its final interpolation finishes");
        helper.assertTrue(motion.finished(4.0), "Completion must release the arm after one tick");
        equal(helper, motion.sample(4.0).position(), START, "Completed return must reach the attachment");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aNewLaunchDoesNotReuseThePreviousTrajectoryClock(GameTestHelper helper) {
        var motion = new PsychicDriveGauntletsDisplayMotion(flight(100, 100), 0);
        motion.sample(3);
        motion.accept(flight(120, 120), 20);
        equal(helper, motion.sample(20).position(), START, "A new launch must start at its own origin");
        equal(helper, motion.sample(20.5).position(),
                PsychicDriveGauntletsMotion.outbound(START, DESTINATION, 0.5),
                "A new launch must use a new elapsed time even when its path is identical");
        helper.succeed();
    }
}
