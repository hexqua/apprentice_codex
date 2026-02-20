package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class RotationTools {
    private RotationTools(){}
    
    public record YawPitch(float yaw, float pitch) {
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


    public static Vec3 calculateBehindPosition(Entity owner, double backOffSet, double xOffset, double yOffset) {
        var yawAngle = owner.getYRot() * Mth.DEG_TO_RAD;
        var forwardX = -Mth.sin(yawAngle);
        var forwardZ = Mth.cos(yawAngle);

        var back = new Vec3(-forwardX, 0, -forwardZ).normalize();
        var right = new Vec3(back.z, 0, -back.x).normalize();

        var behindOffset = back.scale(backOffSet).add(new Vec3(0, yOffset, 0)).add(right.scale(xOffset));
        return owner.getEyePosition().add(behindOffset);
    }

    public static Vec3 steerTowards(Vec3 currentDir, Vec3 targetDir, double maxTurnDeg) {
        // 度数を計算用のラジアンに変換.
        var maxTurnRad = maxTurnDeg * ((float) Math.PI / 180f);
        var dot = Mth.clamp(currentDir.dot(targetDir), -1.0, 1.0);
        var angle = Math.acos(dot);

        // 極小か追いつける場合.
        if (angle < 1.0e-6 || angle <= maxTurnRad) {
            return targetDir;
        }

        // 目標へ maxTurnRad だけ進める.
        var t = maxTurnRad / angle;

        // slerp: sin((1-t)a)/sin(a)*v + sin(ta)/sin(a)*u
        var sinA = Math.sin(angle);
        var w1 = Math.sin((1.0 - t) * angle) / sinA;
        var w2 = Math.sin(t * angle) / sinA;
        return currentDir.scale(w1).add(targetDir.scale(w2)).normalize();
    }

    public static Vec3 getFlatForward(LivingEntity entity) {
        var yawDeg = entity.getYRot();
        var yawRad = yawDeg * Mth.DEG_TO_RAD;

        var x = -Mth.sin(yawRad);
        var z =  Mth.cos(yawRad);

        return new Vec3(x, 0.0, z).normalize();
    }
}
