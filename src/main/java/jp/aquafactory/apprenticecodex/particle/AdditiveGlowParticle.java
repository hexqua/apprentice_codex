package jp.aquafactory.apprenticecodex.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class AdditiveGlowParticle extends TextureSheetParticle {
    private static final float TAU = (float) (Math.PI * 2.0);

    private final Preset preset;
    private final float startSize;
    private final float baseAlpha;
    private final float targetRed;
    private final float targetGreen;
    private final float targetBlue;
    private final int whitenTicks;
    private final float rollSpeed;

    protected AdditiveGlowParticle(ClientLevel level, double x, double y, double z,
                                   double xd, double yd, double zd,
                                   AdditiveGlowParticleOptions options,
                                   SpriteSet sprites,
                                   Preset preset) {
        super(level, x, y, z);
        this.preset = preset;
        this.startSize = options.size() * Mth.lerp(random.nextFloat(), preset.minSizeMultiplier, preset.maxSizeMultiplier);
        this.baseAlpha = Mth.lerp(random.nextFloat(), preset.minAlpha, preset.maxAlpha);
        this.targetRed = options.red();
        this.targetGreen = options.green();
        this.targetBlue = options.blue();
        this.whitenTicks = options.whitenTicks();
        this.rollSpeed = (random.nextFloat() - 0.5F) * preset.rollSpeedRange;

        setParticleSpeed(xd, yd, zd);
        hasPhysics = false;
        friction = preset.friction;
        gravity = preset.gravity;
        lifetime = preset.minLifetime + random.nextInt(preset.lifetimeVariance + 1);
        quadSize = startSize;
        roll = random.nextFloat() * TAU;
        oRoll = roll;
        alpha = 0.0F;
        pickSprite(sprites);
        applyTint();
    }

    @Override
    public void tick() {
        oRoll = roll;
        super.tick();
        if (removed) {
            return;
        }

        roll += rollSpeed;
        applyTint();
        setAlpha(AdditiveParticleUtil.computeAlpha(age, lifetime, preset.fadeInEnd, preset.fadeOutStart, baseAlpha));
    }

    @Override
    public float getQuadSize(float partialTick) {
        var progress = Mth.clamp(((float) age + partialTick) / (float) lifetime, 0.0F, 1.0F);
        return startSize * Mth.lerp(progress, 1.0F, preset.endScaleMultiplier);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return AdditiveParticleRenderType.PARTICLE_SHEET_ADDITIVE;
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    private void applyTint() {
        rCol = AdditiveParticleUtil.mixFromWhite(targetRed, age, whitenTicks);
        gCol = AdditiveParticleUtil.mixFromWhite(targetGreen, age, whitenTicks);
        bCol = AdditiveParticleUtil.mixFromWhite(targetBlue, age, whitenTicks);
    }

    public static class Provider implements ParticleProvider<AdditiveGlowParticleOptions> {
        private final SpriteSet sprites;
        private final Preset preset;

        public Provider(SpriteSet sprites, Preset preset) {
            this.sprites = sprites;
            this.preset = preset;
        }

        @Override
        public Particle createParticle(@NotNull AdditiveGlowParticleOptions options, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new AdditiveGlowParticle(level, x, y, z, xd, yd, zd, options, sprites, preset);
        }
    }

    public enum Preset {
        CIRCLE(
                12, 6,
                0.92F, -0.03F,
                0.90F, 1.35F,
                0.58F, 0.76F,
                0.10F, 0.46F,
                0.16F, 0.58F
        ),
        SPARK(
                6, 4,
                0.88F, -0.01F,
                0.70F, 1.15F,
                0.72F, 0.95F,
                0.08F, 0.28F,
                0.85F, 0.28F
        );

        private final int minLifetime;
        private final int lifetimeVariance;
        private final float friction;
        private final float gravity;
        private final float minSizeMultiplier;
        private final float maxSizeMultiplier;
        private final float minAlpha;
        private final float maxAlpha;
        private final float fadeInEnd;
        private final float fadeOutStart;
        private final float rollSpeedRange;
        private final float endScaleMultiplier;

        Preset(int minLifetime, int lifetimeVariance,
               float friction, float gravity,
               float minSizeMultiplier, float maxSizeMultiplier,
               float minAlpha, float maxAlpha,
               float fadeInEnd, float fadeOutStart,
               float rollSpeedRange, float endScaleMultiplier) {
            this.minLifetime = minLifetime;
            this.lifetimeVariance = lifetimeVariance;
            this.friction = friction;
            this.gravity = gravity;
            this.minSizeMultiplier = minSizeMultiplier;
            this.maxSizeMultiplier = maxSizeMultiplier;
            this.minAlpha = minAlpha;
            this.maxAlpha = maxAlpha;
            this.fadeInEnd = fadeInEnd;
            this.fadeOutStart = fadeOutStart;
            this.rollSpeedRange = rollSpeedRange;
            this.endScaleMultiplier = endScaleMultiplier;
        }
    }
}
