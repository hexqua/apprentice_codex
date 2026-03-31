package jp.aquafactory.apprenticecodex.block.particletest;

import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ParticleTestBlockEntity extends BlockEntity {
    private static final float CIRCLE_SIZE = 0.22F;
    private static final float RHOMBUS_SIZE = 0.22F;
    private static final float SPARK_SIZE = 0.11F;
    private static final float FLAME_RED = 1.0F;
    private static final float FLAME_GREEN = 0.62F;
    private static final float FLAME_BLUE = 0.20F;
    private static final float SPARK_RED = 1.0F;
    private static final float SPARK_GREEN = 0.84F;
    private static final float SPARK_BLUE = 0.50F;
    private static final int NORMAL_WHITEN_TICKS = 4;
    private static final int SPARK_WHITEN_TICKS = 2;
    private static final int CIRCLE_COUNT_PER_TICK = 0;
    private static final int RHOMBUS_COUNT_PER_TICK = 2;
    private static final float SPARK_SPAWN_CHANCE = 0.45F;

    public ParticleTestBlockEntity(BlockPos pos, BlockState state) {
        super(ParticleRegistryHolder.TYPE.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ParticleTestBlockEntity blockEntity) {
        if (!level.isClientSide) {
            return;
        }

        var random = level.random;
        var baseX = pos.getX() + 0.5D;
        var baseY = pos.getY() + 0.18D;
        var baseZ = pos.getZ() + 0.5D;
        for (var i = 0; i < CIRCLE_COUNT_PER_TICK; i++) {
            spawnCircle(level, random, baseX, baseY, baseZ);
        }
        for (var i = 0; i < RHOMBUS_COUNT_PER_TICK; i++) {
            spawnRhombus(level, random, baseX, baseY, baseZ);
        }
        if (random.nextFloat() < SPARK_SPAWN_CHANCE) {
            spawnSpark(level, random, baseX, baseY, baseZ);
        }
    }

    private static void spawnCircle(Level level, RandomSource random, double baseX, double baseY, double baseZ) {
        var x = baseX + (random.nextDouble() - 0.5D) * 0.22D;
        var y = baseY + random.nextDouble() * 0.16D;
        var z = baseZ + (random.nextDouble() - 0.5D) * 0.22D;
        var xd = (random.nextDouble() - 0.5D) * 0.008D;
        var yd = 0.015D + random.nextDouble() * 0.022D;
        var zd = (random.nextDouble() - 0.5D) * 0.008D;
        var size = CIRCLE_SIZE * (0.9F + random.nextFloat() * 0.4F);
        var green = clampColor(FLAME_GREEN + (random.nextFloat() - 0.5F) * 0.08F);
        var blue = clampColor(FLAME_BLUE + (random.nextFloat() - 0.5F) * 0.06F);

        level.addParticle(
                new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_CIRCLE.get(), size, FLAME_RED, green, blue, NORMAL_WHITEN_TICKS),
                x, y, z, xd, yd, zd
        );
    }

    private static void spawnRhombus(Level level, RandomSource random, double baseX, double baseY, double baseZ) {
        var x = baseX + (random.nextDouble() - 0.5D) * 0.20D;
        var y = baseY + random.nextDouble() * 0.16D;
        var z = baseZ + (random.nextDouble() - 0.5D) * 0.20D;
        var xd = (random.nextDouble() - 0.5D) * 0.007D;
        var yd = 0.015D + random.nextDouble() * 0.021D;
        var zd = (random.nextDouble() - 0.5D) * 0.007D;
        var size = RHOMBUS_SIZE * (0.88F + random.nextFloat() * 0.36F);
        var green = clampColor(FLAME_GREEN + (random.nextFloat() - 0.5F) * 0.08F);
        var blue = clampColor(FLAME_BLUE + (random.nextFloat() - 0.5F) * 0.06F);

        level.addParticle(
                new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_RHOMBUS.get(), size, FLAME_RED, green, blue, NORMAL_WHITEN_TICKS),
                x, y, z, xd, yd, zd
        );
    }

    private static void spawnSpark(Level level, RandomSource random, double baseX, double baseY, double baseZ) {
        var x = baseX + (random.nextDouble() - 0.5D) * 0.14D;
        var y = baseY + 0.04D + random.nextDouble() * 0.12D;
        var z = baseZ + (random.nextDouble() - 0.5D) * 0.14D;
        var xd = (random.nextDouble() - 0.5D) * 0.018D;
        var yd = 0.03D + random.nextDouble() * 0.03D;
        var zd = (random.nextDouble() - 0.5D) * 0.018D;
        var size = SPARK_SIZE * (0.85F + random.nextFloat() * 0.5F);
        var green = clampColor(SPARK_GREEN + (random.nextFloat() - 0.5F) * 0.06F);
        var blue = clampColor(SPARK_BLUE + (random.nextFloat() - 0.5F) * 0.08F);

        level.addParticle(
                new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_SPARK.get(), size, SPARK_RED, green, blue, SPARK_WHITEN_TICKS),
                x, y, z, xd, yd, zd
        );
    }

    private static float clampColor(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    /**
     * BlockEntityRegistry との循環 import を局所化するための遅延参照。
     */
    private static final class ParticleRegistryHolder {
        private static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<ParticleTestBlockEntity>> TYPE =
                jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry.PARTICLE_TEST_BLOCK;

        private ParticleRegistryHolder() {
        }
    }
}
