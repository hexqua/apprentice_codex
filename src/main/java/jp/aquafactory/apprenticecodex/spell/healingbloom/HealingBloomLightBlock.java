package jp.aquafactory.apprenticecodex.spell.healingbloom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HealingBloomLightBlock extends BaseEntityBlock implements EntityBlock {
    public static final MapCodec<HealingBloomLightBlock> CODEC = simpleCodec(HealingBloomLightBlock::new);
    private static final VoxelShape OUTLINE_SHAPE = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 4.0D, 10.0D);

    public HealingBloomLightBlock(Properties properties) {
        super(properties);
    }

    public HealingBloomLightBlock() {
        this(Properties.of()
                .strength(0.0F)
                .noCollission()
                .noOcclusion()
                .sound(SoundType.WOOD)
                .lightLevel(state -> 15));
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull CollisionContext context) {
        // 不可視のままでも手で撤去できるよう、中央に小さい選択判定だけを残す。
        return OUTLINE_SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new HealingBloomLightBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
                                                                            @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(
                type,
                jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry.HEALING_BLOOM_LIGHT.get(),
                HealingBloomLightBlockEntity::serverTick
        );
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
