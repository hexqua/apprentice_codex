package jp.aquafactory.apprenticecodex.remoteownercast;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RemoteOwnerCastGeometry {
    private RemoteOwnerCastGeometry() {
    }

    public static Rotation rotationFromForward(Vec3 forward) {
        var normalizedForward = normalizeOrFallback(forward);
        var yaw = (float) Mth.wrapDegrees(Mth.atan2(-normalizedForward.x, normalizedForward.z) * Mth.RAD_TO_DEG);
        var horizontal = Math.sqrt(normalizedForward.x * normalizedForward.x + normalizedForward.z * normalizedForward.z);
        var pitch = (float) Mth.wrapDegrees(-Mth.atan2(normalizedForward.y, horizontal) * Mth.RAD_TO_DEG);
        return new Rotation(yaw, pitch);
    }

    public static Vec3 normalizeOrFallback(Vec3 forward) {
        return forward.lengthSqr() > 1.0E-6D ? forward.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    public record Rotation(float yaw, float pitch) {
    }
}
