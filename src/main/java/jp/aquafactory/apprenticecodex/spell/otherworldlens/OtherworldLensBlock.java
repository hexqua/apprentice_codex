package jp.aquafactory.apprenticecodex.spell.otherworldlens;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

public final class OtherworldLensBlock extends Block {
    static final int ORPHAN_CHECK_INTERVAL_TICKS = 20;

    public OtherworldLensBlock() {
        super(Properties.of()
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.GLASS)
                .pushReaction(PushReaction.BLOCK)
                .isValidSpawn((state, level, pos, entityType) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false));
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                                 @NotNull BlockPos pos, @NotNull CollisionContext context) {
        // X-rayに必要な遮蔽形状は完全立方体のまま残し、Entityとの衝突だけを無効化する。
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(@NotNull BlockState state, @NotNull net.minecraft.world.level.Level level,
                        @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, ORPHAN_CHECK_INTERVAL_TICKS);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos,
                     @NotNull RandomSource random) {
        if (OtherworldLensSessionManager.isActiveAt(level, pos)) {
            level.scheduleTick(pos, this, ORPHAN_CHECK_INTERVAL_TICKS);
            return;
        }

        level.removeBlock(pos, false);
        OtherworldLensSessionManager.logOrphanCleanup(level, pos);
    }
}
