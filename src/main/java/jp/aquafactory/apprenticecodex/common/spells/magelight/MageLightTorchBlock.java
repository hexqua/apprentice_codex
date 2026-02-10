package jp.aquafactory.apprenticecodex.common.spells.magelight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class MageLightTorchBlock extends Block implements EntityBlock {
    private static final VoxelShape OUTLINE_SHAPE = Block.box(6.0, 4.0, 6.0, 10.0, 16.0, 10.0);

    public MageLightTorchBlock() {
        super(Properties.of()
                .strength(0.0F)
                .noCollission()
                .noOcclusion()
                .lightLevel(s -> 14));
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        // チャンク焼き込み描画をしない.
        return RenderShape.INVISIBLE;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull net.minecraft.world.level.BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return OUTLINE_SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new MageLightTorchBlockEntity(pos, state);
    }
}
