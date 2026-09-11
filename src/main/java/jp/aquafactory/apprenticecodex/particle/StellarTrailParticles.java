package jp.aquafactory.apprenticecodex.particle;

import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class StellarTrailParticles {
    private StellarTrailParticles() {}
    private static final float SPARK_RED = 1.0f;
    private static final float SPARK_GREEN_MIN = 0.88f;
    private static final float SPARK_GREEN_MAX = 0.94f;
    private static final float SPARK_BLUE_MIN = 0.11f;
    private static final float SPARK_BLUE_MAX = 0.16f;
    private static final float SPARK_WARM_SHIFT_MAX = 0.03f;
    private static final int SPARK_WHITEN_TICKS = 10;
    private static final int SPARK_LIFETIME = 12;
    private static final int SPARK_LIFETIME_VARIANCE = 4;
    private static final float SPARK_MIN_SIZE_MULTIPLIER = 1.0f;
    private static final float SPARK_MAX_SIZE_MULTIPLIER = 1.35f;
    private static final float SPARK_MIN_ALPHA = 0.9f;
    private static final float SPARK_MAX_ALPHA = 1.0f;
    private static final float SPARK_FADE_IN_END = 0.06f;
    private static final float SPARK_FADE_OUT_START = 0.72f;
    private static final float SPARK_END_SCALE_MULTIPLIER = 0.72f;
    public static void spawn(net.minecraft.world.entity.Entity entity, net.minecraft.util.RandomSource random, Vec3 center, double offsetScale, float size) {
        // SPARK は preset 既定だと「小さい・短い・最後に強く縮む」が重なって RHOMBUS に埋もれやすい。
        // Illuminate 用だけ寿命と縮小率を override して、細いまま残る軌跡に寄せる。
        var offset = new Vec3(
                (random.nextDouble() - 0.5) * offsetScale,
                (random.nextDouble() - 0.5) * offsetScale,
                (random.nextDouble() - 0.5) * offsetScale
        );
        var velocity = entity.getDeltaMovement().scale(-0.06).add(
                (random.nextDouble() - 0.5) * 0.006,
                (random.nextDouble() - 0.5) * 0.006 + 0.006,
                (random.nextDouble() - 0.5) * 0.006
        );
        var warmth = random.nextFloat() * SPARK_WARM_SHIFT_MAX;
        var green = Mth.lerp(random.nextFloat(), SPARK_GREEN_MIN, SPARK_GREEN_MAX) - warmth;
        var blue = Mth.lerp(random.nextFloat(), SPARK_BLUE_MIN, SPARK_BLUE_MAX) - warmth * 0.5f;

        entity.level().addParticle(
                new AdditiveGlowParticleOptions(
                        ParticleRegistry.ADDITIVE_SPARK.get(),
                        size,
                        SPARK_RED,
                        clampColor(green),
                        clampColor(blue),
                        SPARK_WHITEN_TICKS,
                        SPARK_LIFETIME,
                        SPARK_LIFETIME_VARIANCE,
                        SPARK_MIN_SIZE_MULTIPLIER,
                        SPARK_MAX_SIZE_MULTIPLIER,
                        SPARK_MIN_ALPHA,
                        SPARK_MAX_ALPHA,
                        SPARK_FADE_IN_END,
                        SPARK_FADE_OUT_START,
                        SPARK_END_SCALE_MULTIPLIER,
                        true
                ),
                center.x + offset.x,
                center.y + offset.y + 0.02,
                center.z + offset.z,
                velocity.x,
                velocity.y,
                velocity.z
        );
    }

    private static float clampColor(float value) { return Mth.clamp(value, 0, 1); }
}
