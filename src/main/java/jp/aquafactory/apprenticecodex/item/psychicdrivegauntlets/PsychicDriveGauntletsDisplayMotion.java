package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.MotionData;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Snapshot;
import net.minecraft.util.Mth;

public final class PsychicDriveGauntletsDisplayMotion {
    private Snapshot current;
    private Snapshot from;
    private Snapshot flight;
    private double receivedTime;
    private double flightTime;
    private double lastTime;

    public PsychicDriveGauntletsDisplayMotion(Snapshot initial, double time) {
        current = initial;
        from = initial;
        flight = initial;
        receivedTime = time;
        flightTime = time;
        lastTime = time;
    }

    public void accept(Snapshot next, double time) {
        time = Math.max(lastTime, time);
        var displayed = sample(time);
        boolean sameFlight = current.phase() == next.phase()
                && current.launchTick() == next.launchTick()
                && current.motion().phaseStartedTick() == next.motion().phaseStartedTick()
                && current.flightStart().equals(next.flightStart()) && current.destination().equals(next.destination());
        from = displayed;
        current = next;
        receivedTime = time;
        if (!sameFlight) {
            flight = next;
            flightTime = time;
        }
        // 同じ段階のpacketでは時計を更新しない。受信の揺らぎを軌道の加減速へ持ち込まない。
    }

    public Snapshot sample(double time) {
        lastTime = Math.max(lastTime, time);
        float progress = (float) Mth.clamp(lastTime - receivedTime, 0.0, 1.0);
        double elapsed = (double) flight.sampleTick() - flight.motion().phaseStartedTick()
                + Math.max(0.0, lastTime - flightTime);
        var position = switch (current.phase()) {
            case PREPARING -> PsychicDriveGauntletsMotion.preparing(current.flightStart(),
                    current.destination().subtract(current.flightStart()).normalize(),
                    elapsed / PsychicDriveGauntletsMotion.PREPARE_TICKS);
            case OUTBOUND -> PsychicDriveGauntletsMotion.outbound(flight.flightStart(), flight.destination(), elapsed, flight.charge());
            case FOLLOW_THROUGH -> PsychicDriveGauntletsMotion.followingThrough(current.destination(),
                    current.motion().returnSide(), elapsed);
            case RETURNING -> current.charge() >= 1 ? from.position().lerp(current.position(), progress)
                    : PsychicDriveGauntletsMotion.returning(current.motion().returnStart(),
                            from.motion().returnEnd().lerp(current.motion().returnEnd(), progress),
                            current.motion().returnSide(), elapsed);
            default -> from.position().lerp(current.position(), progress);
        };
        var phase = current.phase() == Phase.IDLE && progress < 1.0F ? from.phase() : current.phase();
        // 姿勢の時計も受信時刻へ写し、軌道と独立したclient/serverの時計差を作らない。
        return new Snapshot(phase, position, Mth.rotLerp(progress, from.yaw(), current.yaw()),
                Mth.lerp(progress, from.pitch(), current.pitch()),
                Mth.lerp(progress, from.drawnAmount(), current.drawnAmount()),
                current.flightStart(), current.destination(), current.launchTick(), current.sampleTick(), current.charge(),
                current.recoveryStartedTick(), current.returnRecovery(), current.impact(), current.impactTick(), current.holdTicks(),
                new MotionData(flight.motion().phaseStartedTick() + flightTime - flight.sampleTick(),
                        current.motion().returnStart(), current.motion().returnEnd(), current.motion().returnSide(),
                        current.motion().returnPower(), current.motion().returnTwist()));
    }

    public boolean finished(double time) {
        return sample(time).phase() == Phase.IDLE;
    }
}
