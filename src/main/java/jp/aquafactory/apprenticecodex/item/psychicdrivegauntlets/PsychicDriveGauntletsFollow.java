package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import net.minecraft.world.phys.Vec3;

public final class PsychicDriveGauntletsFollow {
    private static final double HALF_LIFE_TICKS = 1.5;
    private static final double MAX_LAG_DISTANCE = 0.6;
    private static final double TELEPORT_DISTANCE = 4.0;
    private static final double MAX_FRAME_GAP_TICKS = 10.0;

    private Vec3 position;
    private Vec3 lastAnchor;
    private double lastTime;

    public Vec3 follow(Vec3 target, Vec3 anchor, double time, float drawnAmount) {
        var elapsed = time - lastTime;
        // 納刀時は従来どおり固定する。テレポートや描画再開時に古い座標から飛ばさない。
        if (position == null || drawnAmount == 0.0F || elapsed < 0.0 || elapsed > MAX_FRAME_GAP_TICKS
                || anchor.distanceToSqr(lastAnchor) > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
            position = target;
        } else if (elapsed > 0.0) {
            // tick単位の経過時間を使い、一定目標への追従速度がFPSで変わらないようにする。
            var alpha = 1.0 - Math.pow(0.5, elapsed / HALF_LIFE_TICKS);
            position = position.lerp(target, alpha);
        }
        var lag = position.subtract(target);
        if (lag.lengthSqr() > MAX_LAG_DISTANCE * MAX_LAG_DISTANCE) {
            position = target.add(lag.normalize().scale(MAX_LAG_DISTANCE));
        }
        lastAnchor = anchor;
        lastTime = time;
        // モード補間とは別に遅れを弱め、納刀完了時には背面の完成位置へ戻す。
        return target.lerp(position, drawnAmount);
    }
}
