package jp.aquafactory.apprenticecodex.spell.earthforge;

import jp.aquafactory.apprenticecodex.utility.BlockTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EarthForgeJob {
    private final @Nullable ServerPlayer placingPlayer;
    private final BlockPos center;
    private final Direction effectDirection;
    private final List<BlockPos> randomTargets = new ArrayList<>();
    private final RandomSource random;
    private boolean centerPending;
    private long nextPlaceGameTime;
    private boolean complete;

    public EarthForgeJob(@Nullable ServerPlayer placingPlayer, BlockPos center, List<BlockPos> placeablePositions,
                         Direction effectDirection, long currentGameTime) {
        this.placingPlayer = placingPlayer;
        this.center = center.immutable();
        this.effectDirection = effectDirection;
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

    private void tryPlace(ServerLevel level, BlockPos pos) {
        if (!EarthForgePlacementRules.canReplaceWithDirt(level.getBlockState(pos))) {
            return;
        }

        var dirtState = Blocks.DIRT.defaultBlockState();
        if (!tryPlaceDirt(level, pos)) {
            return;
        }
        level.playSound(
                null,
                pos,
                dirtState.getSoundType().getPlaceSound(),
                SoundSource.BLOCKS,
                1.0f,
                1.0f
        );
        spawnPlaceParticles(level, pos, dirtState);
    }

    private boolean tryPlaceDirt(ServerLevel level, BlockPos pos) {
        var player = placingPlayer;
        if (player == null || player.isRemoved()) {
            return level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        }

        var result = BlockTools.useItemOnBlockByPlayerMainHand(
                level,
                player,
                pos,
                new ItemStack(Items.DIRT),
                effectDirection.getOpposite()
        );
        return result.consumesAction() && level.getBlockState(pos).is(Blocks.DIRT);
    }

    private void spawnPlaceParticles(ServerLevel level, BlockPos pos, BlockState dirtState) {
        var dir = effectDirection.getNormal();
        var centerX = pos.getX() + 0.5 + dir.getX() * 0.52;
        var centerY = pos.getY() + 0.5 + dir.getY() * 0.52;
        var centerZ = pos.getZ() + 0.5 + dir.getZ() * 0.52;

        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, dirtState),
                centerX,
                centerY,
                centerZ,
                8,
                0.08,
                0.08,
                0.08,
                0.02
        );
    }
}
