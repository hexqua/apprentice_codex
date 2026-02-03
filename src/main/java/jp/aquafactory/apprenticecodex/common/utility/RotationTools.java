package jp.aquafactory.apprenticecodex.common.utility;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RotationTools {
    public record YawPitch(float yaw, float pitch) {
    }

    private RotationTools(){
        // do nothing.
    }

    public static YawPitch calculateYawPitchByDirection(final Vec3 direction) {
        var normalizedDirection = direction.normalize();
        var yaw = (float) (Mth.atan2(-normalizedDirection.x, normalizedDirection.z) * Mth.RAD_TO_DEG);
        var xzLen = Math.sqrt(normalizedDirection.x * normalizedDirection.x + normalizedDirection.z * normalizedDirection.z);
        var pitch = (float) (Mth.atan2(-normalizedDirection.y, xzLen) * Mth.RAD_TO_DEG);
        return new YawPitch(yaw, pitch);
    }

    public static YawPitch calculateYawPitchByEntity(Entity entity, float partialTicks) {
        var yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        var pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        return new YawPitch(yaw, pitch);
    }
}
