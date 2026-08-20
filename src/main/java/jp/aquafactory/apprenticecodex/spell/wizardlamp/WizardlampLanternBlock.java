package jp.aquafactory.apprenticecodex.spell.wizardlamp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class WizardlampLanternBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
    public static final double FLOATING_OFFSET = 0.25D;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public WizardlampLanternBlock() {
        // pickaxeタグ付きブロックの素手破壊は100除数になるため、硬さ0.3で30tickに合わせる。
        super(Properties.of()
                .strength(0.3F)
                .noOcclusion()
                .sound(SoundType.LANTERN)
                .lightLevel(state -> 15));
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Blocks.LANTERN.defaultBlockState().getShape(level, pos, context)
                .move(0.0D, FLOATING_OFFSET, 0.0D);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                                 @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Blocks.LANTERN.defaultBlockState().getCollisionShape(level, pos, context)
                .move(0.0D, FLOATING_OFFSET, 0.0D);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull net.minecraft.core.Direction direction,
                                           @NotNull BlockState neighborState, @NotNull LevelAccessor level,
                                           @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public @NotNull FluidState getFluidState(@NotNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new WizardlampLanternBlockEntity(pos, state);
    }
}
