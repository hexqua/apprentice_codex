package jp.aquafactory.apprenticecodex.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public class ReticleDotParticle extends TextureSheetParticle {

    private static final float BASE_ALPHA = 0.75F;

    protected ReticleDotParticle(ClientLevel level, double x, double y, double z,
                                 double xd, double yd, double zd, SpriteSet sprites) {

        super(level, x, y, z, xd, yd, zd);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        gravity = 0.0F;
        friction = 1.0F;
        lifetime = 4;
        quadSize = 0.03F;

        rCol = 1.0F;
        gCol = 1.0F;
        bCol = 1.0F;
        alpha = BASE_ALPHA;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();

        // フェードアウトをかける(sin)
        var t = (double) age / lifetime;
        alpha = BASE_ALPHA * ((float) Math.sin(Math.PI * (1 - t) * 0.5));
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new ReticleDotParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
