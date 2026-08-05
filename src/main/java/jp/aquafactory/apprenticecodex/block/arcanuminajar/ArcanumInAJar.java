package jp.aquafactory.apprenticecodex.block.arcanuminajar;

import com.mojang.serialization.MapCodec;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.utility.AdvancementTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ArcanumInAJar extends BaseEntityBlock {
    public static final MapCodec<ArcanumInAJar> CODEC = simpleCodec(ArcanumInAJar::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 14.0D, 13.0D);

    public ArcanumInAJar(Properties properties) {
        super(properties
                .strength(0.3f)
                .sound(SoundType.GLASS)
                .lightLevel(state -> 8)
                .noOcclusion()
                .isSuffocating((state, getter, pos) -> false)
                .isViewBlocking((state, getter, pos) -> false));
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    public ArcanumInAJar() {
        this(Properties.of());
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return defaultBlockState().setValue(OPEN, false);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        // 瓶本体は JSON モデルで描画し、中身の演出だけを BlockEntityRenderer に委ねる.
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                                 @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @NotNull VoxelShape getOcclusionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return true;
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof ArcanumInAJarBlockEntity blockEntity)) {
            return 0;
        }

        return Mth.clamp(blockEntity.getStoredProductCount(), 0, ArcanumInAJarBlockEntity.MAX_STORED_PARAMETER);
    }

    @Override
    public float getShadeBrightness(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level,
                                                       @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand,
                                                       @NotNull BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ArcanumInAJarBlockEntity blockEntity)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (rejectInvalidItemSettings(level, player)) {
            return level.isClientSide ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
        }

        if (hand != InteractionHand.MAIN_HAND) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        var materialItem = ApprenticeCodexServerConfig.arcanumInAJarItemSettings().materialItem();
        if (!stack.is(materialItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!blockEntity.canAcceptMoreMaterial()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(
                                "ui.apprenticecodex.arcane_in_a_jar.max_supply_material",
                                materialItem.getDescription()
                        ).withStyle(ChatFormatting.RED), true);
            }
            // FAIL にすると手前に設置を試みてしまうので CONSUME にする.
            return ItemInteractionResult.CONSUME;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        var inserted = blockEntity.insertMaterial(level.getGameTime(), stack.getCount());
        if (inserted <= 0) {
            return ItemInteractionResult.CONSUME;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(inserted);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                                        @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof ArcanumInAJarBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        if (rejectInvalidItemSettings(level, player)) {
            return InteractionResult.CONSUME;
        }

        if (blockEntity.isDispensing()) {
            return InteractionResult.CONSUME;
        }

        if (isTopBlocked(level, pos)) {
            player.displayClientMessage(Component.translatable("ui.apprenticecodex.arcane_in_a_jar.not_open_top")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        var storedProductCount = blockEntity.getStoredProductCount();
        if (storedProductCount <= 0) {
            var message = blockEntity.hasNoWorkLoaded()
                    ? Component.translatable(
                            "ui.apprenticecodex.arcane_in_a_jar.not_supply_material",
                            ApprenticeCodexServerConfig.arcanumInAJarItemSettings().materialItem().getDescription()
                    )
                    : Component.translatable("ui.apprenticecodex.arcane_in_a_jar.not_stored_product");
            player.displayClientMessage(message.withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            AdvancementTools.award(serverPlayer,
                    AdvancementTools.RETRIEVE_ONCE_ARCANUM_IN_A_JAR,
                    AdvancementTools.RETRIEVE_ARCANE_ESSENCE_CRITERION);
            if (storedProductCount >= ArcanumInAJarBlockEntity.MAX_STORED_PARAMETER) {
                AdvancementTools.award(serverPlayer,
                        AdvancementTools.RETRIEVE_MAX_ARCANUM_IN_A_JAR,
                        AdvancementTools.RETRIEVE_FULLY_CHARGED_ARCANUM_CRITERION);
            }
        }

        blockEntity.startDispenseSequence();
        return InteractionResult.CONSUME;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return BlockEntityRegistry.ARCANUM_IN_A_JAR.get().create(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
                                                                            @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(
                type,
                BlockEntityRegistry.ARCANUM_IN_A_JAR.get(),
                ArcanumInAJarBlockEntity::serverTick
        );
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder builder) {
        var drops = new ArrayList<>(super.getDrops(state, builder));
        if (!(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ArcanumInAJarBlockEntity blockEntity)) {
            return drops;
        }

        blockEntity.appendRemovalDrops(drops);
        return drops;
    }

    private static boolean rejectInvalidItemSettings(Level level, Player player) {
        if (ApprenticeCodexServerConfig.arcanumInAJarItemSettings().isValid()) {
            return false;
        }

        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(
                    "ui.apprenticecodex.arcane_in_a_jar.error_item_settings"
            ).withStyle(ChatFormatting.RED), true);
            ApprenticeCodexServerConfig.warnInvalidArcanumInAJarItemSettingsOnce();
        }
        return true;
    }

    private static boolean isTopBlocked(Level level, BlockPos pos) {
        return level.getBlockState(pos.above()).canOcclude();
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
