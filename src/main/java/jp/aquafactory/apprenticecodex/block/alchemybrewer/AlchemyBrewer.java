package jp.aquafactory.apprenticecodex.block.alchemybrewer;

import com.mojang.serialization.MapCodec;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public final class AlchemyBrewer extends BaseEntityBlock {
    public static final MapCodec<AlchemyBrewer> CODEC = simpleCodec(AlchemyBrewer::new);
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape COLLISION_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D);
    private static final java.util.Map<Direction, VoxelShape> OUTLINE_SHAPES = java.util.Map.of(
            Direction.NORTH, createOutlineShape(Direction.NORTH),
            Direction.EAST, createOutlineShape(Direction.EAST),
            Direction.SOUTH, createOutlineShape(Direction.SOUTH),
            Direction.WEST, createOutlineShape(Direction.WEST)
    );

    public AlchemyBrewer(Properties properties) {
        super(properties.strength(2.5F).sound(SoundType.WOOD).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public AlchemyBrewer() { this(Properties.of()); }
    @Override protected @NotNull MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public @NotNull RenderShape getRenderShape(@NotNull BlockState state) { return RenderShape.MODEL; }
    @Override public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                                  @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return OUTLINE_SHAPES.get(state.getValue(FACING));
    }
    @Override public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                                           @NotNull BlockPos pos, @NotNull CollisionContext context) {
        // 装飾の1px段差を歩行判定へ含めると階段として自動昇降するため、上面はAtelierStationと同じ10pxへ統一する。
        return COLLISION_SHAPE;
    }
    @Override public @NotNull BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override public @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public @NotNull BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new AlchemyBrewerBlockEntity(pos, state);
    }
    @Override public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
                                                                                       @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, BlockEntityRegistry.ALCHEMY_BREWER.get(), AlchemyBrewerBlockEntity::serverTick);
    }
    @Override protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                                                   @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof AlchemyBrewerBlockEntity brewer && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new net.minecraft.world.MenuProvider() {
                @Override public @NotNull net.minecraft.network.chat.Component getDisplayName() {
                    return net.minecraft.network.chat.Component.translatable("container.apprenticecodex.alchemy_brewer");
                }
                @Override public @NotNull net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,
                        @NotNull net.minecraft.world.entity.player.Inventory inventory, @NotNull Player menuPlayer) {
                    return new AlchemyBrewerMenu(id, inventory, brewer);
                }
            }, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
    @Override public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                   @NotNull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AlchemyBrewerBlockEntity brewer) brewer.dropContents();
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static VoxelShape createOutlineShape(Direction facing) {
        var shape = COLLISION_SHAPE;
        shape = Shapes.or(shape, rotatedBox(facing, 9.5D, 9.0D, 9.5D, 15.5D, 17.0D, 15.5D));
        shape = Shapes.or(shape, rotatedBox(facing, 10.0D, 9.0D, 2.0D, 14.0D, 11.0D, 6.0D));
        shape = Shapes.or(shape, rotatedBox(facing, 2.0D, 9.0D, 9.0D, 7.0D, 16.0D, 14.0D));
        return shape.optimize();
    }

    private static VoxelShape rotatedBox(Direction facing, double minX, double minY, double minZ,
                                         double maxX, double maxY, double maxZ) {
        return switch (facing) {
            case EAST -> Block.box(16.0D - maxZ, minY, minX, 16.0D - minZ, maxY, maxX);
            case SOUTH -> Block.box(16.0D - maxX, minY, 16.0D - maxZ, 16.0D - minX, maxY, 16.0D - minZ);
            case WEST -> Block.box(minZ, minY, 16.0D - maxX, maxZ, maxY, 16.0D - minX);
            default -> Block.box(minX, minY, minZ, maxX, maxY, maxZ);
        };
    }
}
