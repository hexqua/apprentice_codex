package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class PsychicDriveGauntletsMotion {
    private static final double BASE_SPEED = 2.0;
    private PsychicDriveGauntletsMotion() {}

    public static double referenceDistance(Vec3 start, Vec3 destination) {
        // 近距離でも6ブロック用の曲線を途中まで使う。腕の横ずれで6を少し超える経路だけ延長する。
        return Math.max(PsychicDriveGauntletsRaycast.RANGE, start.distanceTo(destination));
    }

    public static Vec3 outbound(Vec3 start, Vec3 destination, double elapsedTicks) {
        return outbound(start, destination, elapsedTicks, 0);
    }

    public static Vec3 outbound(Vec3 start, Vec3 destination, double elapsedTicks, float charge) {
        var distance = start.distanceTo(destination);
        if (distance < 1.0e-9) return destination;
        // 曲線全体を射程比で伸ばし、最大射程までの所要時間を通常攻撃とそろえる。
        var scale = 1.0 + charge;
        var reference = charge == 0 ? referenceDistance(start, destination)
                : Math.max(PsychicDriveGauntletsRaycast.RANGE * scale, distance);
        var progress = Mth.clamp(elapsedTicks / (reference / (BASE_SPEED * scale)), 0.0, 1.0);
        var traveled = reference * progress * (2.0 - progress);
        if (traveled >= distance) return destination;
        return start.lerp(destination, traveled / distance);
    }
}
