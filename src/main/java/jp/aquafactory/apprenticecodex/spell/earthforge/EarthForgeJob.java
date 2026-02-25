package jp.aquafactory.apprenticecodex.spell.earthforge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public class EarthForgeJob {
    private final BlockPos center;
    private final List<BlockPos> randomTargets = new ArrayList<>();
    private final RandomSource random;
    private boolean centerPending;
    private long nextPlaceGameTime;
    private boolean complete;

    public EarthForgeJob(BlockPos center, List<BlockPos> placeablePositions, long currentGameTime) {
        this.center = center.immutable();
        centerPending = true;
        for (var pos : placeablePositions) {
            var immutable = pos.immutable();
            if (immutable.equals(this.center)) {
                continue;
            }
            randomTargets.add(immutable);
        }

        random = RandomSource.create(currentGameTime ^ this.center.asLong());
        nextPlaceGameTime = currentGameTime + 2L;
        complete = false;
    }

    public boolean isComplete() {
        return complete;
    }

    public void tick(ServerLevel level) {
        if (complete) {
            return;
        }

        if (level.getGameTime() < nextPlaceGameTime) {
            return;
        }

        placeNext(level);
        nextPlaceGameTime = level.getGameTime() + 2L;

        if (!centerPending && randomTargets.isEmpty()) {
            complete = true;
        }
    }

    private void placeNext(ServerLevel level) {
        if (centerPending) {
            centerPending = false;
            tryPlace(level, center);
            return;
        }

        if (randomTargets.isEmpty()) {
            return;
        }

        var index = random.nextInt(randomTargets.size());
        var pos = randomTargets.remove(index);
        tryPlace(level, pos);
    }

    private static void tryPlace(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) {
            return;
        }

        level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
    }
}
