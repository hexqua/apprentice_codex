package jp.aquafactory.apprenticecodex.spell.lockonray;

import net.minecraft.world.phys.Vec3;

/** 接線は曲線全体のパラメーター長に対する微分。serverの判定とclientの軌跡を一致させる。 */
public record LockOnRayCurve(Vec3 start, Vec3 end, Vec3 startTangent, Vec3 endTangent) {
    public Vec3 position(double t) {
        double t2 = t * t, t3 = t2 * t;
        return start.add(end.subtract(start).scale(-2 * t3 + 3 * t2))
                .add(startTangent.scale(t3 - 2 * t2 + t)).add(endTangent.scale(t3 - t2));
    }

    public Vec3 tangent(double t) {
        return end.subtract(start).scale(-6 * t * t + 6 * t)
                .add(startTangent.scale(3 * t * t - 4 * t + 1))
                .add(endTangent.scale(3 * t * t - 2 * t));
    }

    public LockOnRayCurve prefix(double fraction) {
        return new LockOnRayCurve(start, position(fraction), startTangent.scale(fraction), tangent(fraction).scale(fraction));
    }
}
