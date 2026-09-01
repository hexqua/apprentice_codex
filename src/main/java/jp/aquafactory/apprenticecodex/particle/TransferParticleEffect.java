package jp.aquafactory.apprenticecodex.particle;

import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class TransferParticleEffect {
    private static final double PARTICLE_SPACING = 2.0D;
    private static final int MIN_PARTICLE_COUNT = 4;
    private static final int MAX_PARTICLE_COUNT = 16;

    private TransferParticleEffect() {
    }

    public static void spawn(ServerLevel level, Vec3 start, Vec3 end, Palette palette) {
        var travel = end.subtract(start);
        var particleCount = calculateParticleCount(travel.length());
        for (var i = 1; i <= particleCount; ++i) {
            var point = start.add(travel.scale((double) i / (particleCount + 1)));
            var particle = i % 3 == 0 ? palette.rhombus : palette.spark;
            level.sendParticles(particle, point.x, point.y, point.z,
                    1, 0.025D, 0.025D, 0.025D, 0.0D);
        }
    }

    public static int calculateParticleCount(double distance) {
        return Math.min(
                MAX_PARTICLE_COUNT,
                Math.max(MIN_PARTICLE_COUNT, (int) Math.ceil(distance / PARTICLE_SPACING))
        );
    }

    public enum Palette {
        HEALING(
                new AdditiveGlowParticleOptions(
                        ParticleRegistry.ADDITIVE_SPARK.get(), 0.11F,
                        0.35F, 1.0F, 0.45F, 2, 11, 3, 0.65F, 1.3F,
                        0.62F, 0.95F, 0.08F, 0.42F, 0.55F, true
                ),
                new AdditiveGlowParticleOptions(
                        ParticleRegistry.ADDITIVE_RHOMBUS.get(), 0.16F,
                        0.35F, 1.0F, 0.45F, 3, 14, 4, 0.75F, 1.25F,
                        0.5F, 0.82F, 0.08F, 0.58F, 0.35F, true
                )
        ),
        MANA(
                new AdditiveGlowParticleOptions(
                        ParticleRegistry.ADDITIVE_SPARK.get(), 0.11F,
                        0.35F, 0.75F, 1.0F, 2, 11, 3, 0.65F, 1.3F,
                        0.62F, 0.95F, 0.08F, 0.42F, 0.55F, true
                ),
                new AdditiveGlowParticleOptions(
                        ParticleRegistry.ADDITIVE_RHOMBUS.get(), 0.16F,
                        0.35F, 0.75F, 1.0F, 3, 14, 4, 0.75F, 1.25F,
                        0.5F, 0.82F, 0.08F, 0.58F, 0.35F, true
                )
        );

        private final AdditiveGlowParticleOptions spark;
        private final AdditiveGlowParticleOptions rhombus;

        Palette(AdditiveGlowParticleOptions spark, AdditiveGlowParticleOptions rhombus) {
            this.spark = spark;
            this.rhombus = rhombus;
        }
    }
}
