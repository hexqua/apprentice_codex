package jp.aquafactory.apprenticecodex.block.comfortberrybush;

import com.mojang.serialization.MapCodec;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class ComfortBerryBushBlock extends BushBlock implements BonemealableBlock {
    public static final MapCodec<ComfortBerryBushBlock> CODEC = simpleCodec(ComfortBerryBushBlock::new);
    public static final int MAX_AGE = 4;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_4;
    // 5段階化した各テクスチャの透過余白に合わせ、見た目と選択アウトラインを揃える。
    private static final VoxelShape[] OUTLINE_SHAPES_BY_AGE = new VoxelShape[]{
            Block.box(5.0D, 0.0D, 5.0D, 11.0D, 9.0D, 11.0D),
            Block.box(5.0D, 0.0D, 5.0D, 11.0D, 11.0D, 11.0D),
            Block.box(3.0D, 0.0D, 3.0D, 12.0D, 12.0D, 12.0D),
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D),
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D)
    };

    public ComfortBerryBushBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    public ComfortBerryBushBlock() {
        this(BlockBehaviour.Properties.of()
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.SWEET_BERRY_BUSH)
                .lightLevel(ComfortBerryBushBlock::getLightLevel));
    }

    private static int getLightLevel(BlockState state) {
        int age = state.getValue(AGE);
        if (age >= MAX_AGE) {
            return 10;
        }
        if (age == MAX_AGE - 1) {
            return 7;
        }
        return 2;
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return new ItemStack(ItemRegistry.COMFORT_BERRIES.get());
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return OUTLINE_SHAPES_BY_AGE[state.getValue(AGE)];
    }

    @Override
    public boolean isRandomlyTicking(@NotNull BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE
                && level.getRawBrightness(pos.above(), 0) >= 9
                && random.nextInt(6) == 0) {
            var grown = state.setValue(AGE, age + 1);
            level.setBlock(pos, grown, 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(grown));
        }
    }

    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        // 安楽の果実の低木は移動阻害やダメージを一切持たせない。
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                                        @NotNull Player player, @NotNull BlockHitResult hit) {
        int age = state.getValue(AGE);
        if (age == MAX_AGE) {
            popResource(level, pos, new ItemStack(ItemRegistry.COMFORT_BERRIES.get(), 2 + level.random.nextInt(2)));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS,
                    1.0f, 0.8f + level.random.nextFloat() * 0.4f);
            // 最終段階の収穫後は発光と再成長を最初からやり直したいので stage0 に戻す。
            var harvested = state.setValue(AGE, 0);
            level.setBlock(pos, harvested, 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, harvested));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public boolean isValidBonemealTarget(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(@NotNull Level level, @NotNull RandomSource random, @NotNull BlockPos pos, @NotNull BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(@NotNull ServerLevel level, @NotNull RandomSource random, @NotNull BlockPos pos, @NotNull BlockState state) {
        level.setBlock(pos, state.setValue(AGE, Math.min(MAX_AGE, state.getValue(AGE) + 1)), 2);
    }

    @Override
    protected @NotNull MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }
}
