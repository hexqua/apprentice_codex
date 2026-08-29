package jp.aquafactory.apprenticecodex.spell.shock;

import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ShockImpactParticles {
    private static final int RHOMBUS_COUNT = 3;
    private static final int SPARK_COUNT = 9;
    private static final float RHOMBUS_SIZE_MIN = 0.16F;
    private static final float RHOMBUS_SIZE_MAX = 0.24F;
    private static final float SPARK_SIZE_MIN = 0.07F;
    private static final float SPARK_SIZE_MAX = 0.12F;
    private static final int RHOMBUS_LIFETIME = 6;
    private static final int RHOMBUS_LIFETIME_VARIANCE = 2;
    private static final int SPARK_LIFETIME = 7;
    private static final int SPARK_LIFETIME_VARIANCE = 3;

    private ShockImpactParticles() {
    }

    public static void spawn(Level level, Vec3 impactPosition, Vec3 incoming, Palette palette) {
        if (incoming.lengthSqr() <= 1.0E-8D) {
            return;
        }

        var forward = incoming.normalize();
        var backward = forward.scale(-1.0D);
        var right = computeRightVector(forward);
        var up = forward.cross(right).normalize();
        var random = level.random;

        for (var i = 0; i < RHOMBUS_COUNT; ++i) {
            spawnRhombus(level, random, impactPosition, backward, right, up, palette);
        }
        for (var i = 0; i < SPARK_COUNT; ++i) {
            spawnSpark(level, random, impactPosition, backward, right, up, palette);
        }
    }

    private static void spawnRhombus(
            Level level,
            RandomSource random,
            Vec3 impactPosition,
            Vec3 backward,
            Vec3 right,
            Vec3 up,
            Palette palette
    ) {
        var offset = createOffset(random, backward, right, up, 0.12D, 0.02D, 0.045D);
        var velocity = backward.scale(0.025D + random.nextDouble() * 0.035D)
                .add(right.scale((random.nextDouble() - 0.5D) * 0.12D))
                .add(up.scale((random.nextDouble() - 0.5D) * 0.12D));
        var size = Mth.lerp(random.nextFloat(), RHOMBUS_SIZE_MIN, RHOMBUS_SIZE_MAX);

        level.addParticle(
                new AdditiveGlowParticleOptions(
                        ParticleRegistry.ADDITIVE_RHOMBUS.get(),
                        size,
                        palette.red(),
                        palette.green(),
                        palette.blue(),
                        2,
                        RHOMBUS_LIFETIME,
                        RHOMBUS_LIFETIME_VARIANCE,
                        0.9F,
                        1.15F,
                        0.86F,
                        1.0F,
                        0.02F,
                        0.4F,
                        0.52F,
                        false
                ),
                impactPosition.x + offset.x,
                impactPosition.y + offset.y,
                impactPosition.z + offset.z,
                velocity.x,
                velocity.y,
                velocity.z
        );
    }

    private static void spawnSpark(
            Level level,
            RandomSource random,
            Vec3 impactPosition,
            Vec3 backward,
            Vec3 right,
            Vec3 up,
            Palette palette
    ) {
        var offset = createOffset(random, backward, right, up, 0.1D, 0.025D, 0.05D);
        var velocity = backward.scale(0.07D + random.nextDouble() * 0.11D)
                .add(right.scale((random.nextDouble() - 0.5D) * 0.24D))
                .add(up.scale((random.nextDouble() - 0.5D) * 0.24D));
        var size = Mth.lerp(random.nextFloat(), SPARK_SIZE_MIN, SPARK_SIZE_MAX);

        level.addParticle(
                new AdditiveGlowParticleOptions(
                        ParticleRegistry.ADDITIVE_SPARK.get(),
                        size,
                        palette.red(),
                        palette.green(),
                        palette.blue(),
                        3,
                        SPARK_LIFETIME,
                        SPARK_LIFETIME_VARIANCE,
                        0.9F,
                        1.2F,
                        0.88F,
                        1.0F,
                        0.02F,
                        0.55F,
                        0.58F,
                        true
                ),
                impactPosition.x + offset.x,
                impactPosition.y + offset.y,
                impactPosition.z + offset.z,
                velocity.x,
                velocity.y,
                velocity.z
        );
    }

    private static Vec3 createOffset(
            RandomSource random,
            Vec3 backward,
            Vec3 right,
            Vec3 up,
            double spread,
            double minBackward,
            double maxBackward
    ) {
        return backward.scale(Mth.lerp(random.nextDouble(), minBackward, maxBackward))
                .add(right.scale((random.nextDouble() - 0.5D) * spread))
                .add(up.scale((random.nextDouble() - 0.5D) * spread));
    }

    private static Vec3 computeRightVector(Vec3 forward) {
        var right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
        if (right.lengthSqr() <= 1.0E-8D) {
            right = new Vec3(1.0D, 0.0D, 0.0D).cross(forward);
        }
        return right.normalize();
    }

    public record Palette(float red, float green, float blue) {
        public static final Palette SHOCK = new Palette(0.42F, 0.86F, 1.0F);
        public static final Palette FIRE = new Palette(1.0F, 0.32F, 0.08F);
    }
}
