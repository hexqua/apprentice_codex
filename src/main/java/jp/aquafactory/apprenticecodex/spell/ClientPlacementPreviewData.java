package jp.aquafactory.apprenticecodex.spell;

import net.minecraft.world.phys.Vec3;

public record ClientPlacementPreviewData(Vec3 baseCenter, float radius, float height) {
    public static ClientPlacementPreviewData singleBlockColumn(Vec3 baseCenter) {
        return new ClientPlacementPreviewData(baseCenter, 0.5f, 1.0f);
    }
}
