package jp.aquafactory.apprenticecodex.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

public class MuzzleFlashParticle extends TextureSheetParticle {

    private static final RandomSource RNG = RandomSource.create();
    private static final float BASE_ALPHA = 0.95F;

    protected MuzzleFlashParticle(ClientLevel level, double x, double y, double z,
                                 double xd, double yd, double zd, SpriteSet sprites) {

        super(level, x, y, z, xd, yd, zd);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        gravity = 0.0f;
        friction = 1.0f;
        lifetime = 1;
        quadSize = 0.35f + RNG.nextFloat() * 0.05f;
        roll = RNG.nextFloat() * (float)Math.PI;
        oRoll = roll;

        rCol = 1.0f;
        gCol = 1.0f;
        bCol = 0.9f + RNG.nextFloat() * 0.05f;
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
    public int getLightColor(float partialTick) {
        return 0xF000F0;
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
            return new MuzzleFlashParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
