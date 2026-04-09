package jp.aquafactory.apprenticecodex.spell.searchbeacon;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SearchBeaconState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SearchBeaconSearchService {
    private static final int CHUNK_PADDING = 2;
    private static final int MAX_SEARCH_RANGE = 5000;
    private static final int SEARCH_STEPS_PER_TICK = 48;

    private SearchBeaconSearchService() {
    }

    public static int clampRange(int requestedRange) {
        return Mth.clamp(requestedRange, 0, MAX_SEARCH_RANGE);
    }

    public static int getMaxSearchRange() {
        return MAX_SEARCH_RANGE;
    }

    public static int getSearchStepsPerTick() {
        return SEARCH_STEPS_PER_TICK;
    }

    public static SearchSession createSession(
            ServerLevel level,
            BlockPos origin,
            int requestedRange,
            SearchBeaconTargetList.Definition definition,
            SearchBeaconState state
    ) {
        var cappedRange = clampRange(requestedRange);
        var targets = SearchBeaconTargetManager.resolveTargets(level, definition);
        if (targets.isEmpty() || cappedRange <= 0) {
            return SearchSession.completed();
        }

        var structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var generatorState = level.getChunkSource().getGeneratorState();
        var structureTasks = new ArrayList<StructureSearchTask>();

        for (var holder : targets) {
            var structureId = structureRegistry.getKey(holder.value());
            if (structureId == null) {
                continue;
            }

            var placementTasks = new ArrayList<PlacementSearchTask>();
            // 同じ structure に対する複数 placement から同一 start を拾う場合があるため、重複検知は structure 単位で共有する。
            var processedStarts = new LinkedHashSet<Long>();
            for (var placement : generatorState.getPlacementsForStructure(holder)) {
                var task = createPlacementTask(level, origin, cappedRange, holder, structureId, state, processedStarts, placement);
                placementTasks.add(task);
            }

            if (!placementTasks.isEmpty()) {
                structureTasks.add(new StructureSearchTask(placementTasks));
            }
        }

        return new SearchSession(structureTasks);
    }

    private static @NotNull PlacementSearchTask createPlacementTask(
            ServerLevel level,
            BlockPos origin,
            int rangeBlocks,
            Holder<Structure> structureHolder,
            ResourceLocation structureId,
            SearchBeaconState state,
            Set<Long> processedStarts,
            StructurePlacement placement
    ) {
        if (placement instanceof RandomSpreadStructurePlacement randomSpreadPlacement) {
            return new RandomSpreadSearchTask(level, origin, rangeBlocks, structureHolder.value(), structureId, state, processedStarts, randomSpreadPlacement);
        }
        if (placement instanceof ConcentricRingsStructurePlacement concentricPlacement) {
            return new ConcentricSearchTask(level, origin, rangeBlocks, structureHolder.value(), structureId, state, processedStarts, concentricPlacement);
        }
        return new FallbackSearchTask(level, origin, rangeBlocks, structureHolder.value(), structureId, state, processedStarts, placement);
    }

    private static @Nullable LocatedStructure tryResolveLocatedStructure(
            ServerLevel level,
            BlockPos origin,
            Structure structure,
            ResourceLocation structureId,
            SearchBeaconState state,
            ChunkPos candidateChunk,
            double maxDistanceSq,
            Set<Long> processedStarts
    ) {
        var structureManager = level.structureManager();
        // /locate は最寄り 1 件前提なので使わず、生成候補を少しずつ進めながら start の有無だけを確認する。
        // 1.21.1 では周辺 API が変わる可能性が高いため、「一括走査せず段階処理する」意図をここに残す。
        var presence = structureManager.checkStructurePresence(candidateChunk, structure, false);
        if (presence == StructureCheckResult.START_NOT_PRESENT) {
            return null;
        }

        var chunk = level.getChunk(candidateChunk.x, candidateChunk.z, ChunkStatus.STRUCTURE_STARTS);
        var start = structureManager.getStartForStructure(SectionPos.bottomOf(chunk), structure, chunk);
        if (start == null || !start.isValid()) {
            return null;
        }
        var marker = new SearchBeaconState.StructureMarker(level.dimension().location(), structureId, start.getChunkPos().toLong());
        if (!processedStarts.add(marker.startChunkPos())) {
            return null;
        }

        var center = start.getBoundingBox().getCenter();
        var distanceSq = horizontalDistanceSq(origin, center);
        if (distanceSq > maxDistanceSq) {
            return null;
        }

        return new LocatedStructure(marker, center.immutable(), state.getKnowledge(marker), distanceSq);
    }

    private static double horizontalDistanceSq(BlockPos origin, BlockPos target) {
        var dx = origin.getX() - target.getX();
        var dz = origin.getZ() - target.getZ();
        return dx * dx + dz * dz;
    }

    private static int sectionCoord(int blockCoord) {
        return SectionPos.blockToSectionCoord(blockCoord);
    }

    private static int maxRing(int originCoord, int minCoord, int maxCoord) {
        return Math.max(Math.abs(originCoord - minCoord), Math.abs(maxCoord - originCoord));
    }

    private static RingOffset ringOffset(int ring, int index) {
        if (ring == 0) {
            return new RingOffset(0, 0);
        }

        var edgeLength = ring * 2;
        if (index < edgeLength) {
            return new RingOffset(-ring + index, -ring);
        }
        index -= edgeLength;
        if (index < edgeLength) {
            return new RingOffset(ring, -ring + index);
        }
        index -= edgeLength;
        if (index < edgeLength) {
            return new RingOffset(ring - index, ring);
        }
        index -= edgeLength;
        return new RingOffset(-ring, ring - index);
    }

    public static final class SearchSession {
        private final List<StructureSearchTask> structureTasks;
        private final List<LocatedStructure> locatedStructures = new ArrayList<>();
        private int nextTaskIndex;
        private boolean complete;

        private SearchSession(List<StructureSearchTask> structureTasks) {
            this.structureTasks = new ArrayList<>(structureTasks);
            complete = this.structureTasks.isEmpty();
        }

        public static SearchSession completed() {
            return new SearchSession(List.of());
        }

        public void advance(ServerLevel level, int maxSteps) {
            if (complete) {
                return;
            }

            var remainingSteps = Math.max(1, maxSteps);
            while (remainingSteps > 0 && !structureTasks.isEmpty()) {
                if (nextTaskIndex >= structureTasks.size()) {
                    nextTaskIndex = 0;
                }

                var task = structureTasks.get(nextTaskIndex);
                remainingSteps = task.advance(level, remainingSteps);
                locatedStructures.addAll(task.drainResults());
                if (task.isComplete()) {
                    structureTasks.remove(nextTaskIndex);
                } else {
                    nextTaskIndex++;
                }
            }

            complete = structureTasks.isEmpty();
        }

        public boolean isComplete() {
            return complete;
        }

        public SearchResult getResult() {
            return new SearchResult(locatedStructures);
        }
    }

    private static final class StructureSearchTask {
        private final List<PlacementSearchTask> placementTasks;
        private final List<LocatedStructure> pendingResults = new ArrayList<>();
        private int nextPlacementIndex;
        private boolean complete;

        private StructureSearchTask(List<PlacementSearchTask> placementTasks) {
            this.placementTasks = placementTasks;
            complete = placementTasks.isEmpty();
        }

        public int advance(ServerLevel level, int maxSteps) {
            var remainingSteps = maxSteps;
            while (remainingSteps > 0 && !complete) {
                var currentTask = placementTasks.get(nextPlacementIndex);
                remainingSteps = currentTask.advance(level, remainingSteps);
                pendingResults.addAll(currentTask.drainResults());

                if (currentTask.isComplete()) {
                    nextPlacementIndex++;
                    if (nextPlacementIndex >= placementTasks.size()) {
                        complete = true;
                    }
                }
            }
            return remainingSteps;
        }

        public boolean isComplete() {
            return complete;
        }

        public List<LocatedStructure> drainResults() {
            if (pendingResults.isEmpty()) {
                return List.of();
            }

            var drained = List.copyOf(pendingResults);
            pendingResults.clear();
            return drained;
        }
    }

    private interface PlacementSearchTask {
        int advance(ServerLevel level, int maxSteps);

        boolean isComplete();

        List<LocatedStructure> drainResults();
    }

    private abstract static class AbstractPlacementSearchTask implements PlacementSearchTask {
        protected final BlockPos origin;
        protected final Structure structure;
        protected final ResourceLocation structureId;
        protected final SearchBeaconState state;
        protected final double maxDistanceSq;
        protected final Set<Long> processedStarts;
        protected final List<LocatedStructure> pendingResults = new ArrayList<>();
        protected boolean complete;

        protected AbstractPlacementSearchTask(
                BlockPos origin,
                int rangeBlocks,
                Structure structure,
                ResourceLocation structureId,
                SearchBeaconState state,
                Set<Long> processedStarts
        ) {
            this.origin = origin.immutable();
            this.structure = structure;
            this.structureId = structureId;
            this.state = state;
            this.processedStarts = processedStarts;
            maxDistanceSq = (double) rangeBlocks * (double) rangeBlocks;
        }

        @Override
        public boolean isComplete() {
            return complete;
        }

        @Override
        public List<LocatedStructure> drainResults() {
            if (pendingResults.isEmpty()) {
                return List.of();
            }

            var drained = List.copyOf(pendingResults);
            pendingResults.clear();
            return drained;
        }
    }

    private static final class RandomSpreadSearchTask extends AbstractPlacementSearchTask {
        private final RandomSpreadStructurePlacement placement;
        private final net.minecraft.world.level.chunk.ChunkGeneratorStructureState generatorState;
        private final long levelSeed;
        private final int spacing;
        private final int minChunkX;
        private final int maxChunkX;
        private final int minChunkZ;
        private final int maxChunkZ;
        private final int originRegionX;
        private final int originRegionZ;
        private final int maxRing;
        private int currentRing;
        private int currentIndex;

        private RandomSpreadSearchTask(
                ServerLevel level,
                BlockPos origin,
                int rangeBlocks,
                Structure structure,
                ResourceLocation structureId,
                SearchBeaconState state,
                Set<Long> processedStarts,
                RandomSpreadStructurePlacement placement
        ) {
            super(origin, rangeBlocks, structure, structureId, state, processedStarts);
            this.placement = placement;
            generatorState = level.getChunkSource().getGeneratorState();
            levelSeed = generatorState.getLevelSeed();
            spacing = Math.max(1, placement.spacing());
            minChunkX = sectionCoord(origin.getX() - rangeBlocks) - CHUNK_PADDING;
            maxChunkX = sectionCoord(origin.getX() + rangeBlocks) + CHUNK_PADDING;
            minChunkZ = sectionCoord(origin.getZ() - rangeBlocks) - CHUNK_PADDING;
            maxChunkZ = sectionCoord(origin.getZ() + rangeBlocks) + CHUNK_PADDING;
            originRegionX = Math.floorDiv(sectionCoord(origin.getX()), spacing);
            originRegionZ = Math.floorDiv(sectionCoord(origin.getZ()), spacing);
            var minRegionX = Math.floorDiv(minChunkX, spacing);
            var maxRegionX = Math.floorDiv(maxChunkX, spacing);
            var minRegionZ = Math.floorDiv(minChunkZ, spacing);
            var maxRegionZ = Math.floorDiv(maxChunkZ, spacing);
            maxRing = Math.max(
                    maxRing(originRegionX, minRegionX, maxRegionX),
                    maxRing(originRegionZ, minRegionZ, maxRegionZ)
            );
        }

        @Override
        public int advance(ServerLevel level, int maxSteps) {
            var remainingSteps = maxSteps;
            while (remainingSteps > 0 && !complete) {
                var region = nextRegion();
                if (region == null) {
                    complete = true;
                    break;
                }

                remainingSteps--;
                var candidate = placement.getPotentialStructureChunk(levelSeed, region.x() * spacing, region.z() * spacing);
                if (candidate.x < minChunkX || candidate.x > maxChunkX || candidate.z < minChunkZ || candidate.z > maxChunkZ) {
                    continue;
                }
                if (!placement.isStructureChunk(generatorState, candidate.x, candidate.z)) {
                    continue;
                }

                var located = tryResolveLocatedStructure(level, origin, structure, structureId, state, candidate, maxDistanceSq, processedStarts);
                if (located != null) {
                    pendingResults.add(located);
                }
            }
            return remainingSteps;
        }

        private @Nullable RegionPos nextRegion() {
            while (currentRing <= maxRing) {
                if (currentRing == 0) {
                    currentRing = 1;
                    return new RegionPos(originRegionX, originRegionZ);
                }

                var positionsInRing = currentRing * 8;
                if (currentIndex >= positionsInRing) {
                    currentRing++;
                    currentIndex = 0;
                    continue;
                }

                var offset = ringOffset(currentRing, currentIndex++);
                return new RegionPos(originRegionX + offset.dx(), originRegionZ + offset.dz());
            }

            return null;
        }
    }

    private static final class ConcentricSearchTask extends AbstractPlacementSearchTask {
        private final List<ChunkPos> sortedCandidates;
        private int currentIndex;

        private ConcentricSearchTask(
                ServerLevel level,
                BlockPos origin,
                int rangeBlocks,
                Structure structure,
                ResourceLocation structureId,
                SearchBeaconState state,
                Set<Long> processedStarts,
                ConcentricRingsStructurePlacement placement
        ) {
            super(origin, rangeBlocks, structure, structureId, state, processedStarts);
            var generatorState = level.getChunkSource().getGeneratorState();
            var ringPositions = generatorState.getRingPositionsFor(placement);
            if (ringPositions == null || ringPositions.isEmpty()) {
                sortedCandidates = List.of();
                complete = true;
                return;
            }

            var minChunkX = sectionCoord(origin.getX() - rangeBlocks) - CHUNK_PADDING;
            var maxChunkX = sectionCoord(origin.getX() + rangeBlocks) + CHUNK_PADDING;
            var minChunkZ = sectionCoord(origin.getZ() - rangeBlocks) - CHUNK_PADDING;
            var maxChunkZ = sectionCoord(origin.getZ() + rangeBlocks) + CHUNK_PADDING;
            sortedCandidates = ringPositions.stream()
                    .filter(candidate -> candidate.x >= minChunkX && candidate.x <= maxChunkX
                            && candidate.z >= minChunkZ && candidate.z <= maxChunkZ)
                    .sorted(Comparator.comparingDouble(candidate ->
                            horizontalDistanceSq(origin, candidate.getMiddleBlockPosition(origin.getY()))))
                    .toList();
            complete = sortedCandidates.isEmpty();
        }

        @Override
        public int advance(ServerLevel level, int maxSteps) {
            var remainingSteps = maxSteps;
            while (remainingSteps > 0 && !complete) {
                if (currentIndex >= sortedCandidates.size()) {
                    complete = true;
                    break;
                }

                remainingSteps--;
                var candidate = sortedCandidates.get(currentIndex++);
                var located = tryResolveLocatedStructure(level, origin, structure, structureId, state, candidate, maxDistanceSq, processedStarts);
                if (located != null) {
                    pendingResults.add(located);
                }
            }
            return remainingSteps;
        }
    }

    private static final class FallbackSearchTask extends AbstractPlacementSearchTask {
        private final StructurePlacement placement;
        private final net.minecraft.world.level.chunk.ChunkGeneratorStructureState generatorState;
        private final int originChunkX;
        private final int originChunkZ;
        private final int minChunkX;
        private final int maxChunkX;
        private final int minChunkZ;
        private final int maxChunkZ;
        private final int maxRing;
        private int currentRing;
        private int currentIndex;

        private FallbackSearchTask(
                ServerLevel level,
                BlockPos origin,
                int rangeBlocks,
                Structure structure,
                ResourceLocation structureId,
                SearchBeaconState state,
                Set<Long> processedStarts,
                StructurePlacement placement
        ) {
            super(origin, rangeBlocks, structure, structureId, state, processedStarts);
            this.placement = placement;
            generatorState = level.getChunkSource().getGeneratorState();
            originChunkX = sectionCoord(origin.getX());
            originChunkZ = sectionCoord(origin.getZ());
            minChunkX = sectionCoord(origin.getX() - rangeBlocks) - CHUNK_PADDING;
            maxChunkX = sectionCoord(origin.getX() + rangeBlocks) + CHUNK_PADDING;
            minChunkZ = sectionCoord(origin.getZ() - rangeBlocks) - CHUNK_PADDING;
            maxChunkZ = sectionCoord(origin.getZ() + rangeBlocks) + CHUNK_PADDING;
            maxRing = Math.max(
                    maxRing(originChunkX, minChunkX, maxChunkX),
                    maxRing(originChunkZ, minChunkZ, maxChunkZ)
            );
        }

        @Override
        public int advance(ServerLevel level, int maxSteps) {
            var remainingSteps = maxSteps;
            while (remainingSteps > 0 && !complete) {
                var candidate = nextChunk();
                if (candidate == null) {
                    complete = true;
                    break;
                }

                remainingSteps--;
                if (!placement.isStructureChunk(generatorState, candidate.x, candidate.z)) {
                    continue;
                }

                var located = tryResolveLocatedStructure(level, origin, structure, structureId, state, candidate, maxDistanceSq, processedStarts);
                if (located != null) {
                    pendingResults.add(located);
                }
            }
            return remainingSteps;
        }

        private @Nullable ChunkPos nextChunk() {
            while (currentRing <= maxRing) {
                if (currentRing == 0) {
                    currentRing = 1;
                    return new ChunkPos(originChunkX, originChunkZ);
                }

                var positionsInRing = currentRing * 8;
                if (currentIndex >= positionsInRing) {
                    currentRing++;
                    currentIndex = 0;
                    continue;
                }

                var offset = ringOffset(currentRing, currentIndex++);
                var chunkX = originChunkX + offset.dx();
                var chunkZ = originChunkZ + offset.dz();
                if (chunkX < minChunkX || chunkX > maxChunkX || chunkZ < minChunkZ || chunkZ > maxChunkZ) {
                    continue;
                }
                return new ChunkPos(chunkX, chunkZ);
            }

            return null;
        }
    }

    public record SearchResult(List<LocatedStructure> locatedStructures) {
        public SearchResult {
            locatedStructures = locatedStructures.stream()
                    .sorted(Comparator.comparingDouble(LocatedStructure::distanceSq))
                    .toList();
        }

        public boolean isEmpty() {
            return locatedStructures.isEmpty();
        }

        public boolean hasUnknownStructures() {
            return locatedStructures.stream().anyMatch(located -> located.knowledge() == SearchBeaconState.StructureKnowledge.UNKNOWN);
        }

        public List<SearchBeaconState.StructureMarker> foundStructureMarkers() {
            return locatedStructures.stream()
                    .map(LocatedStructure::marker)
                    .distinct()
                    .toList();
        }
    }

    public record LocatedStructure(
            SearchBeaconState.StructureMarker marker,
            BlockPos center,
            SearchBeaconState.StructureKnowledge knowledge,
            double distanceSq
    ) {
    }

    private record RingOffset(int dx, int dz) {
    }

    private record RegionPos(int x, int z) {
    }
}
