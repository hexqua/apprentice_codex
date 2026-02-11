package jp.aquafactory.apprenticecodex.common.spells.magelight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class MageLightTorchBlock extends Block implements EntityBlock {
    private static final VoxelShape OUTLINE_SHAPE = Block.box(6.0, 4.0, 6.0, 10.0, 16.0, 10.0);

    public MageLightTorchBlock() {
        // 魔法の松明なので普通の松明と比べて1明るい.
        super(Properties.of()
                .strength(0.0F)
                .noCollission()
                .noOcclusion()
                .sound(SoundType.WOOD)
                .lightLevel(s -> 15));
    }

    // 害は無いっぽいので警告握りつぶしをする.
    @Override
    @SuppressWarnings("deprecation")
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        // チャンク焼き込み描画をしない.
        return RenderShape.INVISIBLE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull net.minecraft.world.level.BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return OUTLINE_SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new MageLightTorchBlockEntity(pos, state);
    }
}
