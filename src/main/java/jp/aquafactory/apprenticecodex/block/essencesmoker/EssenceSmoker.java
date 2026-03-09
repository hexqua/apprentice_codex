package jp.aquafactory.apprenticecodex.block.essencesmoker;

import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
    private static final double PIT_CENTER_XZ = 0.5D;
    private static final double PIT_CENTER_Y = 3.0D / 16.0D;
    private static final double PIT_PARTICLE_SPREAD = 0.18D;
    private static final double SMOKER_SOUND_CHANCE = 0.1D;

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

            var completedItems = blockEntity.collectCompletedItems();
            for (var completedItem : completedItems) {
                blockEntity.giveItemToPlayer(player, completedItem);
            }
            if (!completedItems.isEmpty()) {
                playItemSetSound(level, pos);
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

        // 火打ち石は素材・触媒チェックより先に判定し、着火操作を他のエラーで潰さない。
        if (heldStack.is(Items.FLINT_AND_STEEL)) {
            if (!blockEntity.hasCatalyst() || !blockEntity.hasMaterials()) {
                displayError(player, "ui.apprenticecodex.need_material_and_catalyst");
                return InteractionResult.CONSUME;
            }

            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }

            if (blockEntity.ignite(level.getGameTime()) && !player.getAbilities().instabuild) {
                damageIgniter(player, hand);
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

            var catalystSet = blockEntity.setCatalyst(heldStack);
            if (catalystSet) {
                playItemSetSound(level, pos);
            }
            if (catalystSet && !player.getAbilities().instabuild) {
                heldStack.shrink(1);
            }
            return InteractionResult.CONSUME;
        }

        if (blockEntity.hasMaterials() && blockEntity.isMaterialSlotsFull()) {
            if (blockEntity.matchesCurrentCatalystMaterial(heldStack)) {
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

            var materialAdded = blockEntity.addMaterial(heldStack);
            if (materialAdded) {
                playItemSetSound(level, pos);
            }
            if (materialAdded && !player.getAbilities().instabuild) {
                heldStack.shrink(1);
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

        // pit グループ内の炭位置に寄せて、中央下部から燃えている見た目を出す。
        var centerX = pos.getX() + PIT_CENTER_XZ;
        var centerY = pos.getY() + PIT_CENTER_Y;
        var centerZ = pos.getZ() + PIT_CENTER_XZ;
        var x = centerX + (random.nextDouble() - 0.5D) * PIT_PARTICLE_SPREAD;
        var y = centerY + random.nextDouble() * 0.04D;
        var z = centerZ + (random.nextDouble() - 0.5D) * PIT_PARTICLE_SPREAD;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.015D + random.nextDouble() * 0.01D, 0.0D);
        if (random.nextFloat() < 0.7F) {
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.01D, 0.0D);
        }
        if (random.nextDouble() < SMOKER_SOUND_CHANCE) {
            level.playLocalSound(centerX, centerY, centerZ, SoundEvents.SMOKER_SMOKE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
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

    private static void playItemSetSound(Level level, BlockPos pos) {
        AudioTools.playSoundFromPosition(level, pos.getCenter(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.65F, 1.1F, 0.1F);
    }

    private static void damageIgniter(Player player, InteractionHand hand) {
        var heldStack = player.getItemInHand(hand);
        heldStack.hurtAndBreak(1, player, livingEntity -> livingEntity.broadcastBreakEvent(hand));
    }
}
