package jp.aquafactory.apprenticecodex.block.magneticstabilityanchor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class MagneticStabilityAnchorProtection {
    public static final int RANGE_DIAMETER = 5;
    private static final double RANGE_RADIUS = RANGE_DIAMETER / 2.0D;
    private static final Map<ServerLevel, Set<BlockPos>> ACTIVE_ANCHORS = new WeakHashMap<>();

    private MagneticStabilityAnchorProtection() {
    }

    static void register(ServerLevel level, BlockPos pos) {
        ACTIVE_ANCHORS.computeIfAbsent(level, ignored -> new HashSet<>()).add(pos.immutable());
    }

    static void unregister(ServerLevel level, BlockPos pos) {
        var anchors = ACTIVE_ANCHORS.get(level);
        if (anchors == null) {
            return;
        }
        anchors.remove(pos);
        if (anchors.isEmpty()) {
            ACTIVE_ANCHORS.remove(level);
        }
    }

    public static boolean preventsItemCollection(ItemEntity item) {
        if (!(item.level() instanceof ServerLevel level)) {
            return false;
        }
        var anchors = ACTIVE_ANCHORS.get(level);
        if (anchors == null) {
            return false;
        }

        var itemPosition = item.position();
        for (var anchor : anchors) {
            var centerX = anchor.getX() + 0.5D;
            var centerY = anchor.getY() + 0.5D;
            var centerZ = anchor.getZ() + 0.5D;
            if (itemPosition.x >= centerX - RANGE_RADIUS && itemPosition.x < centerX + RANGE_RADIUS
                    && itemPosition.y >= centerY - RANGE_RADIUS && itemPosition.y < centerY + RANGE_RADIUS
                    && itemPosition.z >= centerZ - RANGE_RADIUS && itemPosition.z < centerZ + RANGE_RADIUS) {
                return true;
            }
        }
        return false;
    }
}
