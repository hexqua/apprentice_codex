package jp.aquafactory.apprenticecodex.spell.terraresonance;

import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public final class TerraResonanceSearch {
    // 密集地でも通信量とクライアント描画負荷が過大にならないよう、近い対象だけを返す。
    public static final int MAX_HIGHLIGHT_TARGETS = 1024;
    private static final Comparator<Candidate> NEAREST_FIRST =
            Comparator.comparingLong(Candidate::distanceSqr).thenComparingLong(Candidate::packedPos);
    private static final Comparator<Candidate> FARTHEST_FIRST = NEAREST_FIRST.reversed();

    private TerraResonanceSearch() {
    }

    public static SearchJob start(ServerLevel level, BlockPos anchor, Direction selectedFace, int range) {
        return new SearchJob(level, anchor, selectedFace, range);
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

    public static final class SearchJob {
        private final BlockPos anchor;
        private final SearchBounds bounds;
        private final PriorityQueue<Candidate> nearest =
                new PriorityQueue<>(MAX_HIGHLIGHT_TARGETS, FARTHEST_FIRST);
        private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        private final int maxChunkX;
        private final int minChunkZ;
        private final int maxChunkZ;
        private int chunkX;
        private int chunkZ;
        private int nextSectionIndex;
        private int scanMinX;
        private int scanMaxX;
        private int scanMinZ;
        private int scanMaxZ;
        private int scanMaxY;
        private int scanX;
        private int scanY;
        private int scanZ;
        private boolean sectionReady;
        private boolean complete;
        private SearchResult result;

        private SearchJob(ServerLevel level, BlockPos anchor, Direction selectedFace, int range) {
            this.anchor = anchor.immutable();
            bounds = SearchBounds.create(level, anchor, selectedFace, range);
            if (bounds == null) {
                chunkX = 0;
                maxChunkX = -1;
                minChunkZ = 0;
                maxChunkZ = -1;
                complete = true;
                result = SearchResult.EMPTY;
                return;
            }

            chunkX = SectionPos.blockToSectionCoord(bounds.minX());
            maxChunkX = SectionPos.blockToSectionCoord(bounds.maxX());
            minChunkZ = SectionPos.blockToSectionCoord(bounds.minZ());
            maxChunkZ = SectionPos.blockToSectionCoord(bounds.maxZ());
            chunkZ = minChunkZ;
        }

        public int advance(ServerLevel level, int blockBudget) {
            if (complete || blockBudget <= 0) {
                return 0;
            }

            var inspected = 0;
            while (inspected < blockBudget) {
                if (!sectionReady && !prepareNextSection(level)) {
                    finish();
                    break;
                }

                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    sectionReady = false;
                    continue;
                }

                while (sectionReady && inspected < blockBudget) {
                    mutablePos.set(scanX, scanY, scanZ);
                    if (chunk.getBlockState(mutablePos).is(TagRegistry.Blocks.TERRA_RESONANCE_TARGETS)) {
                        offerNearest(nearest, new Candidate(
                                mutablePos.immutable(),
                                distanceSqr(mutablePos, anchor),
                                mutablePos.asLong()
                        ));
                    }
                    inspected++;
                    advanceBlockCursor();
                }
            }
            return inspected;
        }

        public boolean isComplete() {
            return complete;
        }

        public SearchResult result() {
            if (!complete) {
                throw new IllegalStateException("Terra Resonance search has not completed");
            }
            return result;
        }

        private boolean prepareNextSection(ServerLevel level) {
            while (chunkX <= maxChunkX) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    advanceChunkCursor();
                    continue;
                }

                var sections = chunk.getSections();
                while (nextSectionIndex < sections.length) {
                    var sectionIndex = nextSectionIndex++;
                    var section = sections[sectionIndex];
                    if (section == null || section.hasOnlyAir()) {
                        continue;
                    }

                    var sectionY = level.getMinSection() + sectionIndex;
                    var sectionMinY = Math.max(bounds.minY(), SectionPos.sectionToBlockCoord(sectionY));
                    var sectionMaxY = Math.min(bounds.maxY(), SectionPos.sectionToBlockCoord(sectionY) + 15);
                    if (sectionMinY > sectionMaxY) {
                        continue;
                    }

                    scanMinX = Math.max(bounds.minX(), chunk.getPos().getMinBlockX());
                    scanMaxX = Math.min(bounds.maxX(), chunk.getPos().getMaxBlockX());
                    scanMinZ = Math.max(bounds.minZ(), chunk.getPos().getMinBlockZ());
                    scanMaxZ = Math.min(bounds.maxZ(), chunk.getPos().getMaxBlockZ());
                    scanX = scanMinX;
                    scanY = sectionMinY;
                    scanZ = scanMinZ;
                    scanMaxY = sectionMaxY;
                    sectionReady = true;
                    return true;
                }
                advanceChunkCursor();
            }
            return false;
        }

        private void advanceBlockCursor() {
            scanZ++;
            if (scanZ <= scanMaxZ) {
                return;
            }
            scanZ = scanMinZ;
            scanX++;
            if (scanX <= scanMaxX) {
                return;
            }
            scanX = scanMinX;
            scanY++;
            if (scanY > scanMaxY) {
                sectionReady = false;
            }
        }

        private void advanceChunkCursor() {
            nextSectionIndex = 0;
            chunkZ++;
            if (chunkZ > maxChunkZ) {
                chunkZ = minChunkZ;
                chunkX++;
            }
        }

        private void finish() {
            complete = true;
            if (nearest.isEmpty()) {
                result = SearchResult.EMPTY;
                return;
            }

            var sorted = new ArrayList<>(nearest);
            sorted.sort(NEAREST_FIRST);
            result = new SearchResult(true, sorted.stream().map(Candidate::position).toList());
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
