package jp.aquafactory.apprenticecodex.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmashcastDustPillarParticle extends TerrainParticle {
    private static final int VANILLA_MACE_SMASH_PARTICLE_COUNT = 750;
    private static final double VANILLA_MACE_SMASH_RING_RADIUS = 3.5D;

    protected SmashcastDustPillarParticle(ClientLevel level, double x, double y, double z,
                                          double xd, double yd, double zd,
                                          BlockState state) {
        super(level, x, y, z, xd, yd, zd, state, BlockPos.containing(x, y, z));
        setParticleSpeed(random.nextGaussian() / 30.0D, yd + random.nextGaussian() / 2.0D, random.nextGaussian() / 30.0D);
        lifetime = random.nextInt(20) + 20;
    }

    public static class Provider implements ParticleProvider<SmashcastDustPillarParticleOptions> {
        @Override
        public @Nullable Particle createParticle(@NotNull SmashcastDustPillarParticleOptions options,
                                                @NotNull ClientLevel level,
                                                double x,
                                                double y,
                                                double z,
                                                double xd,
                                                double yd,
                                                double zd) {
            var state = options.state();
            if (state.isAir() || state.is(Blocks.MOVING_PISTON)) {
                return null;
            }

            spawnVanillaMaceSmashBurst(level, x, y, z, state);
            return null;
        }

        private static void spawnVanillaMaceSmashBurst(ClientLevel level, double x, double y, double z, BlockState state) {
            var particleEngine = Minecraft.getInstance().particleEngine;

            // 1.20.1 には 1.21.1 の DUST_PILLAR / level event 2013 がないため、同じ発生式をクライアント側で展開する。
            for (int i = 0; (float) i < (float) VANILLA_MACE_SMASH_PARTICLE_COUNT / 3.0F; i++) {
                particleEngine.add(new SmashcastDustPillarParticle(
                        level,
                        x + level.random.nextGaussian() / 2.0D,
                        y,
                        z + level.random.nextGaussian() / 2.0D,
                        level.random.nextGaussian() * 0.2D,
                        level.random.nextGaussian() * 0.2D,
                        level.random.nextGaussian() * 0.2D,
                        state
                ));
            }

            for (int i = 0; (float) i < (float) VANILLA_MACE_SMASH_PARTICLE_COUNT / 1.5F; i++) {
                particleEngine.add(new SmashcastDustPillarParticle(
                        level,
                        x + VANILLA_MACE_SMASH_RING_RADIUS * Math.cos(i) + level.random.nextGaussian() / 2.0D,
                        y,
                        z + VANILLA_MACE_SMASH_RING_RADIUS * Math.sin(i) + level.random.nextGaussian() / 2.0D,
                        level.random.nextGaussian() * 0.05D,
                        level.random.nextGaussian() * 0.05D,
                        level.random.nextGaussian() * 0.05D,
                        state
                ));
            }
        }
    }
}
