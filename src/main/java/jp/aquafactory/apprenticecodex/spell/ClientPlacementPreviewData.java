package jp.aquafactory.apprenticecodex.spell;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public record ClientPlacementPreviewData(Vec3 baseCenter, float radius, float height, Direction normal) {
    public ClientPlacementPreviewData(Vec3 baseCenter, float radius, float height) {
        this(baseCenter, radius, height, Direction.UP);
    }

    public static ClientPlacementPreviewData singleBlockColumn(Vec3 baseCenter) {
        return new ClientPlacementPreviewData(baseCenter, 0.5f, 1.0f, Direction.UP);
    }

    public static ClientPlacementPreviewData orientedColumn(Vec3 baseCenter, float radius, float height, Direction normal) {
        return new ClientPlacementPreviewData(baseCenter, radius, height, normal);
    }
}
