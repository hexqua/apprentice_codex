package jp.aquafactory.apprenticecodex.particle;

import java.util.List;
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

public final class ManaForceReleaseSparkParticle extends TextureSheetParticle {
    private final float startSize;
    private final int initialLifetime;
    private boolean bounced;

    private ManaForceReleaseSparkParticle(ClientLevel level, double x, double y, double z,
                                              double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        startSize = Mth.lerp(random.nextFloat(), 0.05F, 0.08F);
        quadSize = startSize;
        // Gauntletsより長い飛散・接地後の余韻で、全マナ放出の重さを出す。
        lifetime = 24 + random.nextInt(11);
        initialLifetime = lifetime;
        gravity = 0.6F;
        friction = 0.96F;
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
        var contact = PsychicDriveGauntletsSparkMotion.collide(requested, actual, age, lifetime, bounced, 12);
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
        setColor(Mth.lerp(tint, 1, 0.3F),
                Mth.lerp(tint, 1, 1.0F),
                Mth.lerp(tint, 1, 0.4F));
        float fade = bounced
                ? Mth.clamp((lifetime - age) / (float) 12, 0, 1)
                : Mth.clamp((initialLifetime - age) / (initialLifetime / 3.0F), 0, 1);
        alpha = 0.8F * fade;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float remaining = lifetime - age - partialTick;
        float fade = bounced ? remaining / 12
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
            return new ManaForceReleaseSparkParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
