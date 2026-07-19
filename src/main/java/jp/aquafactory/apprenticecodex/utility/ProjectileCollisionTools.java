package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

public final class ProjectileCollisionTools {
    private static final double CONTACT_EPSILON = 1.0E-5D;

    private ProjectileCollisionTools() {
    }

    /**
     * 中心線のレイキャストでは拾えない、投射物の当たり箱とブロック形状の接触を着弾結果へ変換する。
     */
    public static @Nullable BlockHitResult findPhysicalBlockHit(
            Projectile projectile,
            Vec3 movementStart,
            Vec3 requestedMovement
    ) {
        if (!projectile.horizontalCollision && !projectile.verticalCollision) {
            return null;
        }

        var actualMovement = projectile.position().subtract(movementStart);
        var impactFace = resolveImpactFace(requestedMovement, actualMovement);
        var projectileBox = projectile.getBoundingBox();
        var contactBox = projectileBox.inflate(CONTACT_EPSILON);
        BlockPos closestBlock = null;
        AABB closestShapeBounds = null;
        var closestContactDistance = Double.MAX_VALUE;

        for (var blockPos : BlockPos.betweenClosedStream(contactBox).map(BlockPos::immutable).toList()) {
            var collisionShape = projectile.level().getBlockState(blockPos)
                    .getCollisionShape(projectile.level(), blockPos, CollisionContext.of(projectile));
            if (collisionShape.isEmpty()) {
                continue;
            }

            var worldShape = collisionShape.move(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            if (!Shapes.joinIsNotEmpty(worldShape, Shapes.create(contactBox), BooleanOp.AND)) {
                continue;
            }

            var shapeBounds = worldShape.bounds();
            var contactDistance = contactDistance(projectileBox, shapeBounds, impactFace);
            if (contactDistance < closestContactDistance) {
                closestContactDistance = contactDistance;
                closestBlock = blockPos;
                closestShapeBounds = shapeBounds;
            }
        }

        if (closestBlock == null) {
            return null;
        }

        var hitLocation = contactLocation(projectile.position(), closestShapeBounds, impactFace);
        return new BlockHitResult(hitLocation, impactFace, closestBlock, false);
    }

    /**
     * NeoForge の着弾イベントがキャンセルされた場合、move が切り詰めた移動と速度を元に戻して飛行を続ける。
     */
    public static void continueAfterCancelledImpact(
            Projectile projectile,
            Vec3 movementStart,
            Vec3 requestedMovement
    ) {
        projectile.setPos(movementStart.add(requestedMovement));
        projectile.setDeltaMovement(requestedMovement);
        projectile.horizontalCollision = false;
        projectile.verticalCollision = false;
        projectile.verticalCollisionBelow = false;
        projectile.minorHorizontalCollision = false;
        projectile.setOnGround(false);
    }

    private static Direction resolveImpactFace(Vec3 requestedMovement, Vec3 actualMovement) {
        var blockedX = Math.abs(requestedMovement.x - actualMovement.x);
        var blockedY = Math.abs(requestedMovement.y - actualMovement.y);
        var blockedZ = Math.abs(requestedMovement.z - actualMovement.z);
        if (blockedY >= blockedX && blockedY >= blockedZ) {
            return requestedMovement.y >= 0.0D ? Direction.DOWN : Direction.UP;
        }
        if (blockedX >= blockedZ) {
            return requestedMovement.x >= 0.0D ? Direction.WEST : Direction.EAST;
        }
        return requestedMovement.z >= 0.0D ? Direction.NORTH : Direction.SOUTH;
    }

    private static double contactDistance(AABB projectileBox, AABB shapeBounds, Direction impactFace) {
        return switch (impactFace) {
            case WEST -> Math.abs(shapeBounds.minX - projectileBox.maxX);
            case EAST -> Math.abs(projectileBox.minX - shapeBounds.maxX);
            case DOWN -> Math.abs(shapeBounds.minY - projectileBox.maxY);
            case UP -> Math.abs(projectileBox.minY - shapeBounds.maxY);
            case NORTH -> Math.abs(shapeBounds.minZ - projectileBox.maxZ);
            case SOUTH -> Math.abs(projectileBox.minZ - shapeBounds.maxZ);
        };
    }

    private static Vec3 contactLocation(Vec3 projectilePosition, AABB shapeBounds, Direction impactFace) {
        var x = Mth.clamp(projectilePosition.x, shapeBounds.minX, shapeBounds.maxX);
        var y = Mth.clamp(projectilePosition.y, shapeBounds.minY, shapeBounds.maxY);
        var z = Mth.clamp(projectilePosition.z, shapeBounds.minZ, shapeBounds.maxZ);
        return switch (impactFace) {
            case WEST -> new Vec3(shapeBounds.minX, y, z);
            case EAST -> new Vec3(shapeBounds.maxX, y, z);
            case DOWN -> new Vec3(x, shapeBounds.minY, z);
            case UP -> new Vec3(x, shapeBounds.maxY, z);
            case NORTH -> new Vec3(x, y, shapeBounds.minZ);
            case SOUTH -> new Vec3(x, y, shapeBounds.maxZ);
        };
    }
}
