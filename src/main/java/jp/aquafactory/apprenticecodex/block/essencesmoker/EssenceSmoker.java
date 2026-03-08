package jp.aquafactory.apprenticecodex.block.essencesmoker;

import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class EssenceSmoker extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public EssenceSmoker() {
        super(Properties.of()
                .strength(1.5f, 6.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()
                .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
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
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (!(level.getBlockEntity(pos) instanceof EssenceSmokerBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        var heldStack = player.getItemInHand(hand);
        if (blockEntity.isProcessing()) {
            displayError(player, "ui.apprenticecodex.now_smoke_processing");
            return InteractionResult.CONSUME;
        }

        if (blockEntity.isCompleted()) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            for (var completedItem : blockEntity.collectCompletedItems()) {
                blockEntity.giveItemToPlayer(player, completedItem);
            }
            return InteractionResult.CONSUME;
        }

        if (heldStack.isEmpty()) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            if (blockEntity.hasMaterials()) {
                blockEntity.giveItemToPlayer(player, blockEntity.popLastMaterial());
                return InteractionResult.CONSUME;
            }

            if (blockEntity.hasCatalyst()) {
                blockEntity.giveItemToPlayer(player, blockEntity.popCatalyst());
                return InteractionResult.CONSUME;
            }
            return InteractionResult.CONSUME;
        }

        if (!blockEntity.hasCatalyst()) {
            if (!blockEntity.canAcceptCatalyst(heldStack)) {
                displayError(player, "ui.apprenticecodex.not_match_catalyst", heldStack.getHoverName());
                return InteractionResult.CONSUME;
            }

            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            if (blockEntity.setCatalyst(heldStack) && !player.getAbilities().instabuild) {
                heldStack.shrink(1);
            }
            return InteractionResult.CONSUME;
        }

        if (blockEntity.hasMaterials() && blockEntity.isMaterialSlotsFull()) {
            if (blockEntity.canAcceptMaterial(heldStack)) {
                displayError(player, "ui.apprenticecodex.max_count_material");
            } else {
                displayError(player, "ui.apprenticecodex.need_ignite",
                        Items.FLINT_AND_STEEL.getDescription(),
                        Component.translatable("spell.irons_spellbooks.firebolt"));
            }
            return InteractionResult.CONSUME;
        }

        if (ItemStack.isSameItemSameTags(heldStack, blockEntity.getCatalyst())) {
            displayError(player, "ui.apprenticecodex.already_set_catalyst");
            return InteractionResult.CONSUME;
        }

        if (blockEntity.canAcceptMaterial(heldStack)) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            if (blockEntity.addMaterial(heldStack) && !player.getAbilities().instabuild) {
                heldStack.shrink(1);
            }
            return InteractionResult.CONSUME;
        }

        if (blockEntity.hasMaterials() && heldStack.is(Items.FLINT_AND_STEEL)) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            if (blockEntity.startProcessing(level.getGameTime()) && !player.getAbilities().instabuild) {
                heldStack.hurtAndBreak(1, player, livingEntity -> livingEntity.broadcastBreakEvent(hand));
            }
            return InteractionResult.CONSUME;
        }

        if (blockEntity.hasMaterials() && heldStack.getMaxStackSize() == 1) {
            displayError(player, "ui.apprenticecodex.need_ignite",
                    Items.FLINT_AND_STEEL.getDescription(),
                    Component.translatable("spell.irons_spellbooks.firebolt"));
            return InteractionResult.CONSUME;
        }

        displayError(player, "ui.apprenticecodex.not_match_material", heldStack.getHoverName(), blockEntity.getCatalyst().getHoverName());
        return InteractionResult.CONSUME;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return BlockEntityRegistry.ESSENCE_SMOKER.get().create(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
                                                                            @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(
                type,
                BlockEntityRegistry.ESSENCE_SMOKER.get(),
                EssenceSmokerBlockEntity::serverTick
        );
    }

    @Override
    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (!(level.getBlockEntity(pos) instanceof EssenceSmokerBlockEntity blockEntity) || !blockEntity.isProcessing()) {
            return;
        }

        var x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.2D;
        var y = pos.getY() + 0.9D + random.nextDouble() * 0.2D;
        var z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.2D;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.04D, 0.0D);
        if (random.nextBoolean()) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0.0D, 0.03D, 0.0D);
        }
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder builder) {
        var drops = new ArrayList<>(super.getDrops(state, builder));
        if (!(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof EssenceSmokerBlockEntity blockEntity)) {
            return drops;
        }

        drops.addAll(blockEntity.getDropsForBlockBreak());
        return drops;
    }

    private static void displayError(Player player, String key, Object... args) {
        if (player.level().isClientSide) {
            return;
        }

        player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED), true);
    }
}
