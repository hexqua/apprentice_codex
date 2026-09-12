package jp.aquafactory.apprenticecodex.particle;

import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;

// 月の魔法で色と減衰を共有し、弾体の挙動から演出設定を切り離す。
public final class LunarParticles {
    private LunarParticles() {}

    private static final float SPARK_RED = 0.9f;
    private static final float SPARK_GREEN = 0.97f;
    private static final float SPARK_BLUE = 1.0f;
    private static final int SPARK_WHITEN_TICKS = 8;
    private static final int TRAIL_SPARK_LIFETIME = 17;
    private static final int TRAIL_SPARK_LIFETIME_VARIANCE = 5;
    private static final float TRAIL_SPARK_MIN_SIZE_MULTIPLIER = 1.0f;
    private static final float TRAIL_SPARK_MAX_SIZE_MULTIPLIER = 1.28f;
    private static final float TRAIL_SPARK_MIN_ALPHA = 0.82f;
    private static final float TRAIL_SPARK_MAX_ALPHA = 0.98f;
    private static final float TRAIL_SPARK_FADE_IN_END = 0.05f;
    private static final float TRAIL_SPARK_FADE_OUT_START = 0.76f;
    private static final float TRAIL_SPARK_END_SCALE_MULTIPLIER = 0.82f;
    private static final int BURST_SPARK_LIFETIME = 28;
    private static final int BURST_SPARK_LIFETIME_VARIANCE = 8;
    private static final float BURST_SPARK_MIN_SIZE_MULTIPLIER = 1.05f;
    private static final float BURST_SPARK_MAX_SIZE_MULTIPLIER = 1.45f;
    private static final float BURST_SPARK_MIN_ALPHA = 0.88f;
    private static final float BURST_SPARK_MAX_ALPHA = 1.0f;
    private static final float BURST_SPARK_FADE_IN_END = 0.04f;
    private static final float BURST_SPARK_FADE_OUT_START = 0.78f;
    private static final float BURST_SPARK_END_SCALE_MULTIPLIER = 0.72f;

    public static AdditiveGlowParticleOptions createTrailSpark(float size) {
        return createTrailSpark(size, TRAIL_SPARK_LIFETIME, TRAIL_SPARK_LIFETIME_VARIANCE);
    }

    public static AdditiveGlowParticleOptions createTrailSpark(float size, int lifetime, int lifetimeVariance) {
        return new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_SPARK.get(),
                size,
                SPARK_RED,
                SPARK_GREEN,
                SPARK_BLUE,
                SPARK_WHITEN_TICKS,
                lifetime,
                lifetimeVariance,
                TRAIL_SPARK_MIN_SIZE_MULTIPLIER,
                TRAIL_SPARK_MAX_SIZE_MULTIPLIER,
                TRAIL_SPARK_MIN_ALPHA,
                TRAIL_SPARK_MAX_ALPHA,
                TRAIL_SPARK_FADE_IN_END,
                TRAIL_SPARK_FADE_OUT_START,
                TRAIL_SPARK_END_SCALE_MULTIPLIER,
                true
        );
    }

    public static AdditiveGlowParticleOptions createBurstSpark(float size) {
        return new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_SPARK.get(),
                size,
                SPARK_RED,
                SPARK_GREEN,
                SPARK_BLUE,
                SPARK_WHITEN_TICKS,
                BURST_SPARK_LIFETIME,
                BURST_SPARK_LIFETIME_VARIANCE,
                BURST_SPARK_MIN_SIZE_MULTIPLIER,
                BURST_SPARK_MAX_SIZE_MULTIPLIER,
                BURST_SPARK_MIN_ALPHA,
                BURST_SPARK_MAX_ALPHA,
                BURST_SPARK_FADE_IN_END,
                BURST_SPARK_FADE_OUT_START,
                BURST_SPARK_END_SCALE_MULTIPLIER,
                true
        );
    }

}
