package jp.aquafactory.apprenticecodex.block.spellcasteraccessorycase;

import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCaseMenu;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public final class SpellcasterAccessoryCaseBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape NORTH_SHAPE = Block.box(1.5D, 0.0D, 3.5D, 14.5D, 11.5D, 14.0D);
    private static final VoxelShape SOUTH_SHAPE = Block.box(1.5D, 0.0D, 2.0D, 14.5D, 11.5D, 12.5D);
    private static final VoxelShape EAST_SHAPE = Block.box(2.0D, 0.0D, 1.5D, 12.5D, 11.5D, 14.5D);
    private static final VoxelShape WEST_SHAPE = Block.box(3.5D, 0.0D, 1.5D, 14.0D, 11.5D, 14.5D);

    public SpellcasterAccessoryCaseBlock(Properties properties) {
        super(properties
                .instabreak()
                .sound(SoundType.WOOD)
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public SpellcasterAccessoryCaseBlock() {
        this(Properties.of());
    }

    @Override
    public @NotNull BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull VoxelShape getShape(
            @NotNull BlockState state,
            @NotNull BlockGetter level,
            @NotNull BlockPos pos,
            @NotNull CollisionContext context
    ) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    public @NotNull InteractionResult use(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull InteractionHand hand,
            @NotNull BlockHitResult hitResult
    ) {
        return openMenu(level, pos, player)
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @Nullable LivingEntity placer,
            @NotNull ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SpellcasterAccessoryCaseBlockEntity blockEntity) {
            blockEntity.setCaseStack(stack);
        }
    }

    @Override
    public void playerWillDestroy(
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @NotNull Player player
    ) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SpellcasterAccessoryCaseBlockEntity blockEntity) {
            giveToPlayerOrDrop(player, blockEntity.takeCaseStack());
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder builder) {
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof SpellcasterAccessoryCaseBlockEntity blockEntity) {
            var stack = blockEntity.copyCaseStackForDrop();
            return stack.isEmpty() ? List.of() : List.of(stack);
        }
        return List.of();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return BlockEntityRegistry.SPELLCASTER_ACCESSORY_CASE.get().create(pos, state);
    }

    private static boolean openMenu(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof SpellcasterAccessoryCaseBlockEntity blockEntity)) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(
                    serverPlayer,
                    blockEntity,
                    buffer -> SpellcasterAccessoryCaseMenu.writeBlockSource(buffer, pos)
            );
        }
        return true;
    }

    private static void giveToPlayerOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        } else if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
