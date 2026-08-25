package jp.aquafactory.apprenticecodex.block.magneticstabilityanchor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class MagneticStabilityAnchorProtection {
    public static final int RANGE_DIAMETER = 5;
    private static final double RANGE_RADIUS = RANGE_DIAMETER / 2.0D;
    private static final Map<ServerLevel, Map<Long, Set<BlockPos>>> ACTIVE_ANCHORS = new WeakHashMap<>();

    private MagneticStabilityAnchorProtection() {
    }

    static void register(ServerLevel level, BlockPos pos) {
        ACTIVE_ANCHORS.computeIfAbsent(level, ignored -> new HashMap<>())
                .computeIfAbsent(ChunkPos.asLong(pos), ignored -> new HashSet<>())
                .add(pos.immutable());
    }

    static void unregister(ServerLevel level, BlockPos pos) {
        var anchorsByChunk = ACTIVE_ANCHORS.get(level);
        if (anchorsByChunk == null) {
            return;
        }

        var chunkKey = ChunkPos.asLong(pos);
        var anchors = anchorsByChunk.get(chunkKey);
        if (anchors == null) {
            return;
        }

        anchors.remove(pos);
        if (anchors.isEmpty()) {
            anchorsByChunk.remove(chunkKey);
        }
        if (anchorsByChunk.isEmpty()) {
            ACTIVE_ANCHORS.remove(level);
        }
    }

    public static boolean preventsItemCollection(ItemEntity item) {
        if (!(item.level() instanceof ServerLevel level)) {
            return false;
        }
        var anchorsByChunk = ACTIVE_ANCHORS.get(level);
        if (anchorsByChunk == null) {
            return false;
        }

        var itemPosition = item.position();
        var minChunkX = SectionPos.blockToSectionCoord(minimumAnchorCoordinate(itemPosition.x));
        var maxChunkX = SectionPos.blockToSectionCoord(maximumAnchorCoordinate(itemPosition.x));
        var minChunkZ = SectionPos.blockToSectionCoord(minimumAnchorCoordinate(itemPosition.z));
        var maxChunkZ = SectionPos.blockToSectionCoord(maximumAnchorCoordinate(itemPosition.z));
        // 5ブロック幅の候補座標は各軸で最大2チャンクにしかまたがらないため、最大4チャンクだけを調べる。
        for (var chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (var chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                var anchors = anchorsByChunk.get(ChunkPos.asLong(chunkX, chunkZ));
                if (anchors != null && preventsItemCollection(itemPosition, anchors)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int minimumAnchorCoordinate(double itemCoordinate) {
        // 保護範囲の上端が排他的なため、成立し得るアンカー座標の下限も排他的になる。
        return Mth.floor(itemCoordinate - RANGE_RADIUS - 0.5D) + 1;
    }

    private static int maximumAnchorCoordinate(double itemCoordinate) {
        return Mth.floor(itemCoordinate + RANGE_RADIUS - 0.5D);
    }

    private static boolean preventsItemCollection(Vec3 itemPosition, Set<BlockPos> anchors) {
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
