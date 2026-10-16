package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Snapshot;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class PsychicDriveGauntletsMotion {
    private static final double BASE_SPEED = 2.0;
    public static final int PREPARE_TICKS = 2;
    public static final double PREPARE_DISTANCE = 0.1;
    public record SinglePose(float power, float twist) {
        public static final SinglePose REST = new SinglePose(0, 0);
    }
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
        if (charge < 1) {
            // 単発は接触まで加速し、腕の横ずれや射程延長があっても最大3tickに収める。
            double progress = Mth.clamp(elapsedTicks / 3.0, 0, 1);
            double reference = Math.max(PsychicDriveGauntletsRaycast.RANGE * (1 + charge), distance);
            double traveled = reference * (0.5 * progress + 0.5 * progress * progress);
            return start.lerp(destination, Math.min(1, traveled / distance));
        }
        // 曲線全体を射程比で伸ばし、最大射程までの所要時間を通常攻撃とそろえる。
        var scale = 1.0 + charge;
        var reference = charge == 0 ? referenceDistance(start, destination)
                : Math.max(PsychicDriveGauntletsRaycast.RANGE * scale, distance);
        var progress = Mth.clamp(elapsedTicks / (reference / (BASE_SPEED * scale)), 0.0, 1.0);
        var traveled = reference * progress * (2.0 - progress);
        if (traveled >= distance) return destination;
        return start.lerp(destination, traveled / distance);
    }

    public static Vec3 preparing(Vec3 attachment, Vec3 direction, double progress) {
        return attachment.add(direction.scale(-PREPARE_DISTANCE * Mth.clamp(progress, 0, 1)));
    }

    public static float returnProgress(double ticks) {
        return (float) Mth.clamp(ticks / PsychicDriveGauntletsArm.RETURN_TICKS, 0, 1);
    }

    private static float missProgress(double ticks) {
        float progress = (float) Mth.clamp((ticks - 1) / (PsychicDriveGauntletsArm.MISS_TICKS - 1), 0, 1);
        return progress * progress * (3 - 2 * progress);
    }

    public static Vec3 followingThrough(Vec3 destination, Vec3 side, double ticks) {
        // 最初の1tickは伸ばし切り、残りで外側へ抜いて帰還へつなぐ。攻撃範囲は延長しない。
        return destination.add(side.scale(0.15 * missProgress(ticks)));
    }

    public static float returnEase(float progress) {
        return 1 - (1 - progress) * (1 - progress) * (1 - progress);
    }

    public static Vec3 returning(Vec3 start, Vec3 end, Vec3 side, double ticks) {
        float progress = returnProgress(ticks);
        return start.lerp(end, returnEase(progress))
                .add(side.scale(0.15 * Math.sin(Math.PI * progress)));
    }

    public static SinglePose singlePose(Snapshot arm, double time) {
        double elapsed = Math.max(0, time - arm.motion().phaseStartedTick());
        return switch (arm.phase()) {
            case PREPARING -> {
                float progress = (float) Mth.clamp(elapsed / PREPARE_TICKS, 0, 1);
                yield new SinglePose(progress, 10 * progress);
            }
            case OUTBOUND -> {
                float progress = (float) Mth.clamp(elapsed, 0, 1);
                yield new SinglePose(arm.charge() > 0 ? progress : 1, arm.charge() > 0 ? 0 : 10 * (1 - progress));
            }
            case HOLDING -> new SinglePose(1, 0);
            case FOLLOW_THROUGH -> {
                float progress = missProgress(elapsed);
                yield new SinglePose(1 - 0.25F * progress, -5 * progress);
            }
            case RETURNING -> {
                float remaining = 1 - returnEase(returnProgress(elapsed));
                yield new SinglePose(arm.motion().returnPower() * remaining, arm.motion().returnTwist() * remaining);
            }
            default -> SinglePose.REST;
        };
    }
}
