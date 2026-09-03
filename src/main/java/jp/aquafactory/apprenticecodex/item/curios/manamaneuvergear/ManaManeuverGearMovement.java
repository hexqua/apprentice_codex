package jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ManaManeuverGearMovement {
    public static final double WALL_JUMP_ACCELERATION = 0.4D;
    public static final double WALL_SLIDE_MINIMUM_Y_SPEED = -0.12D;
    private static final double WALL_PROBE_DISTANCE = 1.0e-3D;
    private static final double WALL_PROBE_VERTICAL_INSET = 1.0e-3D;

    private ManaManeuverGearMovement() {
    }

    public static boolean isTouchingWall(Entity entity) {
        var bounds = entity.getBoundingBox();
        var probe = new AABB(
                bounds.minX - WALL_PROBE_DISTANCE,
                bounds.minY + WALL_PROBE_VERTICAL_INSET,
                bounds.minZ - WALL_PROBE_DISTANCE,
                bounds.maxX + WALL_PROBE_DISTANCE,
                bounds.maxY - WALL_PROBE_VERTICAL_INSET,
                bounds.maxZ + WALL_PROBE_DISTANCE
        );
        return !entity.level().noCollision(entity, probe);
    }

    public static void applyWallJump(Entity entity, Vec3 impulse) {
        var currentVelocity = entity.getDeltaMovement();
        entity.setDeltaMovement(
                currentVelocity.x + impulse.x,
                impulse.y,
                currentVelocity.z + impulse.z
        );
        entity.hasImpulse = true;
        entity.fallDistance = 0.0F;
    }

    public static boolean applyWallSlide(Entity entity) {
        var currentVelocity = entity.getDeltaMovement();
        if (currentVelocity.y >= WALL_SLIDE_MINIMUM_Y_SPEED) {
            entity.fallDistance = 0.0F;
            return false;
        }

        entity.setDeltaMovement(currentVelocity.x, WALL_SLIDE_MINIMUM_Y_SPEED, currentVelocity.z);
        entity.hasImpulse = true;
        // 壁滑りはサーバーだけで判定するため、操作中の本人にも制限後の落下速度を明示的に同期する。
        entity.hurtMarked = true;
        entity.fallDistance = 0.0F;
        return true;
    }
}
