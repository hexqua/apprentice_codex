package jp.aquafactory.apprenticecodex.block.spelldispenser;

import com.mojang.serialization.MapCodec;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public final class SpellDispenser extends BaseEntityBlock {
    public static final MapCodec<SpellDispenser> CODEC = simpleCodec(SpellDispenser::new);
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = DispenserBlock.FACING;
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    public SpellDispenser(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TRIGGERED, false));
    }

    public SpellDispenser() {
        this(Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.5F)
                .sound(SoundType.WOOD));
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(TRIGGERED, false);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
                            @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide || !(placer instanceof Player player)) {
            return;
        }

        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SpellDispenserBlockEntity spellDispenser) {
            spellDispenser.setOwnerProfile(player.getGameProfile());
        }
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
        builder.add(FACING, TRIGGERED);
    }

    @Override
    public @NotNull BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new SpellDispenserBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level,
                                                                            @NotNull BlockState state,
                                                                            @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(
                type,
                BlockEntityRegistry.SPELL_DISPENSER.get(),
                (tickLevel, tickPos, tickState, blockEntity) ->
                        SpellDispenserBlockEntity.serverTick((net.minecraft.server.level.ServerLevel) tickLevel, tickPos, tickState, blockEntity)
        );
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                                        @NotNull Player player, @NotNull net.minecraft.world.phys.BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SpellDispenserBlockEntity spellDispenser && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(spellDispenser, buffer -> {
                buffer.writeBoolean(false);
                buffer.writeBlockPos(pos);
            });
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }

        var powered = level.hasNeighborSignal(pos);
        var triggered = state.getValue(TRIGGERED);
        if (powered && !triggered) {
            var blockEntity = level.getBlockEntity(pos);
            var result = blockEntity instanceof SpellDispenserBlockEntity spellDispenser ? spellDispenser.tryActivate() : null;
            playActivationSound(level, pos, result);
            var latestState = level.getBlockState(pos);
            if (latestState.is(this)) {
                level.setBlock(pos, latestState.setValue(TRIGGERED, true), 4);
            }
        } else if (!powered && triggered) {
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SpellDispenserBlockEntity spellDispenser) {
                spellDispenser.clearContinuousResetRequired();
            }
            var latestState = level.getBlockState(pos);
            if (latestState.is(this)) {
                level.setBlock(pos, latestState.setValue(TRIGGERED, false), 4);
            }
        }
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                         @NotNull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SpellDispenserBlockEntity spellDispenser) {
                spellDispenser.stopContinuousCast(true);
                spellDispenser.dropStoredItems();
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    public Direction getFacing(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    private static void playActivationSound(
            @NotNull Level level,
            @NotNull BlockPos pos,
            @Nullable SpellDispenserCastHelper.CastResult result
    ) {
        if (result == null) {
            level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }

        if (result.succeeded()) {
            level.playSound(null, pos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }

        if (!result.reachedOnCast()) {
            level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}
