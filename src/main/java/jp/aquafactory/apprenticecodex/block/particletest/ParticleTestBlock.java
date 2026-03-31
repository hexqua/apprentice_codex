package jp.aquafactory.apprenticecodex.block.particletest;

import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParticleTestBlock extends BaseEntityBlock {
    private static final VoxelShape OUTLINE_SHAPE = Block.box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0);

    public ParticleTestBlock() {
        super(Properties.of()
                .strength(0.0F)
                .noCollission()
                .noOcclusion()
                .sound(SoundType.WOOD)
                .lightLevel(state -> 15));
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        // パーティクルだけ見せたいのでブロックそのものは描画しない.
        return RenderShape.INVISIBLE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return OUTLINE_SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return BlockEntityRegistry.PARTICLE_TEST_BLOCK.get().create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
                                                                            @NotNull BlockState state,
                                                                            @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(
                type,
                BlockEntityRegistry.PARTICLE_TEST_BLOCK.get(),
                ParticleTestBlockEntity::clientTick
        ) : null;
    }
}
