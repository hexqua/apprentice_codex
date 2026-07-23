package jp.aquafactory.apprenticecodex.spell.terraresonance;

import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public final class TerraResonanceSearch {
    public static final int MAX_HIGHLIGHT_TARGETS = 4096;
    private static final Comparator<Candidate> NEAREST_FIRST =
            Comparator.comparingLong(Candidate::distanceSqr).thenComparingLong(Candidate::packedPos);
    private static final Comparator<Candidate> FARTHEST_FIRST = NEAREST_FIRST.reversed();

    private TerraResonanceSearch() {
    }

    public static SearchResult collect(ServerLevel level, BlockPos anchor, Direction selectedFace, int range) {
        var bounds = SearchBounds.create(level, anchor, selectedFace, range);
        if (bounds == null) {
            return SearchResult.EMPTY;
        }

        var nearest = new PriorityQueue<>(MAX_HIGHLIGHT_TARGETS, FARTHEST_FIRST);
        var found = false;
        var mutablePos = new BlockPos.MutableBlockPos();

        for (var chunkX = SectionPos.blockToSectionCoord(bounds.minX());
             chunkX <= SectionPos.blockToSectionCoord(bounds.maxX()); chunkX++) {
            for (var chunkZ = SectionPos.blockToSectionCoord(bounds.minZ());
                 chunkZ <= SectionPos.blockToSectionCoord(bounds.maxZ()); chunkZ++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                var chunkMinX = Math.max(bounds.minX(), chunk.getPos().getMinBlockX());
                var chunkMaxX = Math.min(bounds.maxX(), chunk.getPos().getMaxBlockX());
                var chunkMinZ = Math.max(bounds.minZ(), chunk.getPos().getMinBlockZ());
                var chunkMaxZ = Math.min(bounds.maxZ(), chunk.getPos().getMaxBlockZ());
                var sections = chunk.getSections();
                for (var sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                    LevelChunkSection section = sections[sectionIndex];
                    if (section == null || section.hasOnlyAir()) {
                        continue;
                    }

                    var sectionY = level.getMinSection() + sectionIndex;
                    var sectionMinY = Math.max(bounds.minY(), SectionPos.sectionToBlockCoord(sectionY));
                    var sectionMaxY = Math.min(bounds.maxY(), SectionPos.sectionToBlockCoord(sectionY) + 15);
                    if (sectionMinY > sectionMaxY) {
                        continue;
                    }

                    for (var y = sectionMinY; y <= sectionMaxY; y++) {
                        for (var x = chunkMinX; x <= chunkMaxX; x++) {
                            for (var z = chunkMinZ; z <= chunkMaxZ; z++) {
                                mutablePos.set(x, y, z);
                                if (!chunk.getBlockState(mutablePos).is(TagRegistry.Blocks.TERRA_RESONANCE_TARGETS)) {
                                    continue;
                                }

                                found = true;
                                offerNearest(nearest, new Candidate(
                                        mutablePos.immutable(),
                                        distanceSqr(mutablePos, anchor),
                                        mutablePos.asLong()
                                ));
                            }
                        }
                    }
                }
            }
        }

        if (!found) {
            return SearchResult.EMPTY;
        }

        var sorted = new ArrayList<>(nearest);
        sorted.sort(NEAREST_FIRST);
        return new SearchResult(true, sorted.stream().map(Candidate::position).toList());
    }

    private static void offerNearest(PriorityQueue<Candidate> nearest, Candidate candidate) {
        if (nearest.size() < MAX_HIGHLIGHT_TARGETS) {
            nearest.add(candidate);
            return;
        }

        var farthest = nearest.peek();
        if (farthest != null && NEAREST_FIRST.compare(candidate, farthest) < 0) {
            nearest.poll();
            nearest.add(candidate);
        }
    }

    private static long distanceSqr(BlockPos position, BlockPos anchor) {
        var dx = (long) position.getX() - anchor.getX();
        var dy = (long) position.getY() - anchor.getY();
        var dz = (long) position.getZ() - anchor.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    public record SearchResult(boolean found, List<BlockPos> highlightTargets) {
        private static final SearchResult EMPTY = new SearchResult(false, List.of());

        public SearchResult {
            highlightTargets = List.copyOf(highlightTargets);
        }
    }

    private record Candidate(BlockPos position, long distanceSqr, long packedPos) {
    }

    private record SearchBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        private static SearchBounds create(ServerLevel level, BlockPos anchor, Direction selectedFace, int requestedRange) {
            var range = Math.max(1, requestedRange);
            var half = range / 2;
            var inward = selectedFace.getOpposite();
            var minX = anchor.getX() - half;
            var maxX = anchor.getX() + half;
            var minY = anchor.getY() - half;
            var maxY = anchor.getY() + half;
            var minZ = anchor.getZ() - half;
            var maxZ = anchor.getZ() + half;

            switch (inward.getAxis()) {
                case X -> {
                    minX = inward.getStepX() > 0 ? anchor.getX() : anchor.getX() - range + 1;
                    maxX = inward.getStepX() > 0 ? anchor.getX() + range - 1 : anchor.getX();
                }
                case Y -> {
                    minY = inward.getStepY() > 0 ? anchor.getY() : anchor.getY() - range + 1;
                    maxY = inward.getStepY() > 0 ? anchor.getY() + range - 1 : anchor.getY();
                }
                case Z -> {
                    minZ = inward.getStepZ() > 0 ? anchor.getZ() : anchor.getZ() - range + 1;
                    maxZ = inward.getStepZ() > 0 ? anchor.getZ() + range - 1 : anchor.getZ();
                }
            }

            minY = Math.max(minY, level.getMinBuildHeight());
            maxY = Math.min(maxY, level.getMaxBuildHeight() - 1);
            return minY <= maxY ? new SearchBounds(minX, minY, minZ, maxX, maxY, maxZ) : null;
        }
    }
}
