package jp.aquafactory.apprenticecodex.item.curios.manathruster;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ManaThrusterMovement {
    // Create: Jetpack の縦速度 0.4 を基準に、魔法アクセサリとして少し強めの上限にする。
    private static final double VERTICAL_ACCELERATION_PER_TICK = 0.16D;
    private static final double MAX_VERTICAL_SPEED = 0.55D;
    private static final double HORIZONTAL_ACCELERATION_PER_TICK = 0.045D;
    private static final double MAX_HORIZONTAL_SPEED = 0.34D;
    private static final double DIRECTIONAL_ACCELERATION_PER_TICK = 0.08D;
    private static final double MAX_DIRECTIONAL_SPEED = 1.5D;

    private ManaThrusterMovement() {
    }

    public static void applyThrust(Entity entity) {
        if (entity instanceof LivingEntity livingEntity && (livingEntity.isFallFlying() || livingEntity.isSwimming())) {
            applyDirectionalAcceleration(entity);
            return;
        }

        var movement = entity.getDeltaMovement();
        var horizontal = applyHorizontalAcceleration(entity, movement);
        var nextY = Math.min(movement.y + VERTICAL_ACCELERATION_PER_TICK, MAX_VERTICAL_SPEED);
        entity.setDeltaMovement(horizontal.x, nextY, horizontal.z);
        entity.hasImpulse = true;
        entity.hurtMarked = true;
    }

    private static void applyDirectionalAcceleration(Entity entity) {
        var direction = entity.getLookAngle();
        if (direction.lengthSqr() < 1.0e-6D) {
            return;
        }

        var nextMovement = entity.getDeltaMovement()
                .add(direction.normalize().scale(DIRECTIONAL_ACCELERATION_PER_TICK));
        var speedSqr = nextMovement.lengthSqr();
        if (speedSqr > MAX_DIRECTIONAL_SPEED * MAX_DIRECTIONAL_SPEED) {
            nextMovement = nextMovement.normalize().scale(MAX_DIRECTIONAL_SPEED);
        }

        entity.setDeltaMovement(nextMovement);
        entity.hasImpulse = true;
        entity.hurtMarked = true;
    }

    private static Vec3 applyHorizontalAcceleration(Entity entity, Vec3 movement) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return movement;
        }

        var input = resolveInputDirection(livingEntity);
        if (input.lengthSqr() < 1.0e-6D) {
            return movement;
        }

        var horizontal = new Vec3(movement.x, 0.0D, movement.z).add(input.scale(HORIZONTAL_ACCELERATION_PER_TICK));
        var horizontalSpeedSqr = horizontal.lengthSqr();
        if (horizontalSpeedSqr > MAX_HORIZONTAL_SPEED * MAX_HORIZONTAL_SPEED) {
            horizontal = horizontal.normalize().scale(MAX_HORIZONTAL_SPEED);
        }
        return new Vec3(horizontal.x, movement.y, horizontal.z);
    }

    private static Vec3 resolveInputDirection(LivingEntity entity) {
        var forwardInput = entity.zza;
        var strafeInput = entity.xxa;
        if (Math.abs(forwardInput) < 1.0e-4F && Math.abs(strafeInput) < 1.0e-4F) {
            return Vec3.ZERO;
        }

        var yawRadians = entity.getYRot() * Mth.DEG_TO_RAD;
        var sin = Mth.sin(yawRadians);
        var cos = Mth.cos(yawRadians);
        var x = strafeInput * cos - forwardInput * sin;
        var z = forwardInput * cos + strafeInput * sin;
        var direction = new Vec3(x, 0.0D, z);
        return direction.lengthSqr() > 1.0D ? direction.normalize() : direction;
    }
}
