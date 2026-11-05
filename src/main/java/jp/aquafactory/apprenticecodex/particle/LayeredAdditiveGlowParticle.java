package jp.aquafactory.apprenticecodex.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import org.jetbrains.annotations.NotNull;

public final class LayeredAdditiveGlowParticle extends AdditiveGlowParticle {
    private LayeredAdditiveGlowParticle(ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd,
                                       AdditiveGlowParticleOptions options, SpriteSet sprites, Preset preset) {
        super(level, x, y, z, xd, yd, zd, options, sprites, preset);
        setLayerSprites(sprites);
    }

    public static final class Provider implements ParticleProvider<AdditiveGlowParticleOptions> {
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
            return new LayeredAdditiveGlowParticle(level, x, y, z, xd, yd, zd, options, sprites, preset);
        }
    }
}
