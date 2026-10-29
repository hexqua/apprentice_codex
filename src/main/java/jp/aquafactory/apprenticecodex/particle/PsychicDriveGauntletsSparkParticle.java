package jp.aquafactory.apprenticecodex.particle;

import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsVisualState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class PsychicDriveGauntletsSparkParticle extends TextureSheetParticle {
    private final float startSize;
    private final int initialLifetime;
    private boolean bounced;

    private PsychicDriveGauntletsSparkParticle(ClientLevel level, double x, double y, double z,
                                              double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        startSize = Mth.lerp(random.nextFloat(), PsychicDriveGauntletsVisualState.SPARK_MIN_SIZE,
                PsychicDriveGauntletsVisualState.SPARK_MAX_SIZE);
        quadSize = startSize;
        lifetime = PsychicDriveGauntletsVisualState.SPARK_MIN_LIFETIME
                + random.nextInt(PsychicDriveGauntletsVisualState.SPARK_LIFETIME_VARIANCE + 1);
        initialLifetime = lifetime;
        gravity = PsychicDriveGauntletsVisualState.SPARK_GRAVITY;
        friction = PsychicDriveGauntletsVisualState.SPARK_FRICTION;
        hasPhysics = true;
        setSize(startSize * 2, startSize * 2);
        setParticleSpeed(xd, yd, zd);
        pickSprite(sprites);
        alpha = 0.8F;
    }

    @Override
    public void move(double x, double y, double z) {
        var requested = new Vec3(x, y, z);
        var actual = Entity.collideBoundingBox(null, requested, getBoundingBox(), level, List.of());
        setBoundingBox(getBoundingBox().move(actual));
        setLocationFromBoundingbox();
        // vanillaのmoveは接地で恒久停止するため、地形衝突だけを使い反発を独立して処理する。
        var contact = PsychicDriveGauntletsSparkMotion.collide(requested, actual, age, lifetime, bounced);
        setParticleSpeed(contact.velocity().x, contact.velocity().y, contact.velocity().z);
        lifetime = contact.lifetime();
        bounced = contact.bounced();
        if (contact.removed()) remove();
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        float tint = Mth.clamp(age / 2.0F, 0, 1);
        setColor(Mth.lerp(tint, 1, PsychicDriveGauntletsVisualState.SPARK_RED),
                Mth.lerp(tint, 1, PsychicDriveGauntletsVisualState.SPARK_GREEN),
                Mth.lerp(tint, 1, PsychicDriveGauntletsVisualState.SPARK_BLUE));
        float fade = bounced
                ? Mth.clamp((lifetime - age) / (float) PsychicDriveGauntletsVisualState.SPARK_BOUNCE_TICKS, 0, 1)
                : Mth.clamp((initialLifetime - age) / (initialLifetime / 3.0F), 0, 1);
        alpha = 0.8F * fade;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float remaining = lifetime - age - partialTick;
        float fade = bounced ? remaining / PsychicDriveGauntletsVisualState.SPARK_BOUNCE_TICKS
                : remaining / (initialLifetime / 3.0F);
        return startSize * Mth.lerp(Mth.clamp(fade, 0, 1), 0.25F, 1);
    }

    @Override
    public int getLightColor(float partialTick) { return LightTexture.FULL_BRIGHT; }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return AdditiveParticleRenderType.PARTICLE_SHEET_ADDITIVE;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new PsychicDriveGauntletsSparkParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
