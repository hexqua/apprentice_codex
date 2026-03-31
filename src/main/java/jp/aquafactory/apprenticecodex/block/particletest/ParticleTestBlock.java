package jp.aquafactory.apprenticecodex.block.particletest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class ParticleTestBlock extends Block {
    private static final VoxelShape OUTLINE_SHAPE = Block.box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0);

    public ParticleTestBlock() {
        super(Properties.of()
                .strength(0.0F)
                .noCollission()
                .noOcclusion()
                .sound(SoundType.WOOD)
                .lightLevel(state -> 15));
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        // パーティクルだけ見せたいのでブロックそのものは描画しない.
        return RenderShape.INVISIBLE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return OUTLINE_SHAPE;
    }

    @Override
    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        // サーバー同期不要の実験用なので、クライアントの表示ティックだけで固定粒子を炊く。
        var baseX = pos.getX() + 0.5;
        var baseY = pos.getY() + 0.5;
        var baseZ = pos.getZ() + 0.5;

        for (var i = 0; i < 2; i++) {
            var offsetX = (random.nextDouble() - 0.5) * 0.3;
            var offsetY = (random.nextDouble() - 0.5) * 0.3;
            var offsetZ = (random.nextDouble() - 0.5) * 0.3;
            var speedX = (random.nextDouble() - 0.5) * 0.01;
            var speedY = 0.01 + random.nextDouble() * 0.01;
            var speedZ = (random.nextDouble() - 0.5) * 0.01;

            level.addParticle(ParticleTypes.END_ROD,
                    baseX + offsetX,
                    baseY + offsetY,
                    baseZ + offsetZ,
                    speedX,
                    speedY,
                    speedZ);
        }
    }
}
