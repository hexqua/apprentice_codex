package jp.aquafactory.apprenticecodex.block.alchemybrewer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

final class AlchemyBrewerWaterEffects {
    static final Vec3 JAR_MOUTH_LOCAL = new Vec3(12.5d / 16.0d, 17.0d / 16.0d, 12.5d / 16.0d);

    private AlchemyBrewerWaterEffects() {
    }

    static Vec3 localToWorld(BlockPos blockPos, Direction facing, Vec3 localPoint) {
        var relativeX = localPoint.x - 0.5d;
        var relativeZ = localPoint.z - 0.5d;
        var rotated = switch (facing) {
            case EAST -> new Vec3(-relativeZ, localPoint.y, relativeX);
            case SOUTH -> new Vec3(-relativeX, localPoint.y, -relativeZ);
            case WEST -> new Vec3(relativeZ, localPoint.y, -relativeX);
            default -> new Vec3(relativeX, localPoint.y, relativeZ);
        };
        return new Vec3(
                blockPos.getX() + rotated.x + 0.5d,
                blockPos.getY() + rotated.y,
                blockPos.getZ() + rotated.z + 0.5d
        );
    }
}
