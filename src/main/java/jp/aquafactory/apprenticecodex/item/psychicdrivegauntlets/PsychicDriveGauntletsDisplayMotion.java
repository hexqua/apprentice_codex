package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

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
        boolean sameFlight = current.phase() == Phase.OUTBOUND && next.phase() == Phase.OUTBOUND
                && current.launchTick() == next.launchTick()
                && current.flightStart().equals(next.flightStart()) && current.destination().equals(next.destination());
        from = displayed;
        current = next;
        receivedTime = time;
        if (!sameFlight) {
            flight = next;
            flightTime = time;
        }
        // 同じ飛行のpacketでは時計を更新しない。受信の揺らぎを軌道の加減速へ持ち込まない。
    }

    public Snapshot sample(double time) {
        lastTime = Math.max(lastTime, time);
        float progress = (float) Mth.clamp(lastTime - receivedTime, 0.0, 1.0);
        var position = current.phase() == Phase.OUTBOUND
                ? PsychicDriveGauntletsMotion.outbound(flight.flightStart(), flight.destination(),
                        (double) flight.sampleTick() - flight.launchTick() + Math.max(0.0, lastTime - flightTime), flight.charge())
                : from.position().lerp(current.position(), progress);
        var phase = current.phase() == Phase.IDLE && progress < 1.0F ? from.phase() : current.phase();
        return new Snapshot(phase, position, Mth.rotLerp(progress, from.yaw(), current.yaw()),
                Mth.lerp(progress, from.pitch(), current.pitch()),
                Mth.lerp(progress, from.drawnAmount(), current.drawnAmount()),
                current.flightStart(), current.destination(), current.launchTick(), current.sampleTick(), current.charge());
    }

    public boolean finished(double time) {
        return sample(time).phase() == Phase.IDLE;
    }
}
