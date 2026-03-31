package jp.aquafactory.apprenticecodex.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class AdditiveRhombusParticle extends TextureSheetParticle {
    private static final float HORIZONTAL_SCALE = 1.14F;
    private static final float VERTICAL_SCALE = 0.84F;

    private final Preset preset;
    private final float startSize;
    private final float baseAlpha;
    private final float targetRed;
    private final float targetGreen;
    private final float targetBlue;
    private final int whitenTicks;
    private final float scaleX;
    private final float scaleY;
    private final float fadeInEnd;
    private final float fadeOutStart;
    private final float endScaleMultiplier;

    protected AdditiveRhombusParticle(ClientLevel level, double x, double y, double z,
                                      double xd, double yd, double zd,
                                      AdditiveGlowParticleOptions options,
                                      SpriteSet sprites,
                                      Preset preset) {
        super(level, x, y, z);
        this.preset = preset;
        this.startSize = options.size() * pickRange(random,
                options.minSizeMultiplier(), options.maxSizeMultiplier(),
                preset.minSizeMultiplier, preset.maxSizeMultiplier);
        this.baseAlpha = pickRange(random,
                options.minAlpha(), options.maxAlpha(),
                preset.minAlpha, preset.maxAlpha);
        this.targetRed = options.red();
        this.targetGreen = options.green();
        this.targetBlue = options.blue();
        this.whitenTicks = options.whitenTicks();
        this.fadeInEnd = options.fadeInEnd() >= 0.0F ? options.fadeInEnd() : preset.fadeInEnd;
        this.fadeOutStart = options.fadeOutStart() >= 0.0F ? options.fadeOutStart() : preset.fadeOutStart;
        this.endScaleMultiplier = options.endScaleMultiplier() >= 0.0F ? options.endScaleMultiplier() : preset.endScaleMultiplier;
        if (random.nextBoolean()) {
            this.scaleX = HORIZONTAL_SCALE;
            this.scaleY = VERTICAL_SCALE;
        } else {
            this.scaleX = VERTICAL_SCALE;
            this.scaleY = HORIZONTAL_SCALE;
        }

        setParticleSpeed(xd, yd, zd);
        hasPhysics = false;
        friction = preset.friction;
        gravity = preset.gravity;
        lifetime = resolveLifetime(random, options, preset.minLifetime, preset.lifetimeVariance);
        quadSize = startSize;
        roll = 0.0F;
        oRoll = 0.0F;
        alpha = 0.0F;
        pickSprite(sprites);
        applyTint();
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }

        applyTint();
        alpha = AdditiveParticleUtil.computeAlpha(age, lifetime, fadeInEnd, fadeOutStart, baseAlpha);
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        Vec3 cameraPos = renderInfo.getPosition();
        float x = (float) (Mth.lerp(partialTicks, xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(partialTicks, yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(partialTicks, zo, this.z) - cameraPos.z());
        Quaternionf rotation = new Quaternionf(renderInfo.rotation());
        float size = getQuadSize(partialTicks);

        Vector3f[] vertices = new Vector3f[]{
                new Vector3f(-scaleX, -scaleY, 0.0F),
                new Vector3f(-scaleX, scaleY, 0.0F),
                new Vector3f(scaleX, scaleY, 0.0F),
                new Vector3f(scaleX, -scaleY, 0.0F)
        };

        for (var vertex : vertices) {
            vertex.rotate(rotation);
            vertex.mul(size);
            vertex.add(x, y, z);
        }

        float u0 = getU0();
        float u1 = getU1();
        float v0 = getV0();
        float v1 = getV1();
        int light = getLightColor(partialTicks);

        buffer.vertex(vertices[0].x(), vertices[0].y(), vertices[0].z()).uv(u1, v1).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        buffer.vertex(vertices[1].x(), vertices[1].y(), vertices[1].z()).uv(u1, v0).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        buffer.vertex(vertices[2].x(), vertices[2].y(), vertices[2].z()).uv(u0, v0).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        buffer.vertex(vertices[3].x(), vertices[3].y(), vertices[3].z()).uv(u0, v1).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
    }

    @Override
    public float getQuadSize(float partialTick) {
        var progress = Mth.clamp(((float) age + partialTick) / (float) lifetime, 0.0F, 1.0F);
        return startSize * Mth.lerp(progress, 1.0F, endScaleMultiplier);
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

    private static int resolveLifetime(net.minecraft.util.RandomSource random, AdditiveGlowParticleOptions options,
                                       int presetMinLifetime, int presetLifetimeVariance) {
        if (options.lifetime() >= 0) {
            var variance = Math.max(0, options.lifetimeVariance());
            return Math.max(1, options.lifetime()) + random.nextInt(variance + 1);
        }

        return presetMinLifetime + random.nextInt(presetLifetimeVariance + 1);
    }

    private static float pickRange(net.minecraft.util.RandomSource random,
                                   float minOverride, float maxOverride,
                                   float presetMin, float presetMax) {
        var min = minOverride >= 0.0F ? minOverride : presetMin;
        var max = maxOverride >= 0.0F ? maxOverride : presetMax;
        if (max < min) {
            max = min;
        }
        return Mth.lerp(random.nextFloat(), min, max);
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
            return new AdditiveRhombusParticle(level, x, y, z, xd, yd, zd, options, sprites, preset);
        }
    }

    public enum Preset {
        RHOMBUS(
                12, 6,
                0.92F, -0.03F,
                0.90F, 1.35F,
                0.58F, 0.76F,
                0.10F, 0.46F,
                0.58F
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
        private final float endScaleMultiplier;

        Preset(int minLifetime, int lifetimeVariance,
               float friction, float gravity,
               float minSizeMultiplier, float maxSizeMultiplier,
               float minAlpha, float maxAlpha,
               float fadeInEnd, float fadeOutStart,
               float endScaleMultiplier) {
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
            this.endScaleMultiplier = endScaleMultiplier;
        }
    }
}
