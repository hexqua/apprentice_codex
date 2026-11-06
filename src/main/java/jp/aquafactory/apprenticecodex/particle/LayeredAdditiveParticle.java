package jp.aquafactory.apprenticecodex.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.NotNull;

public abstract class LayeredAdditiveParticle extends TextureSheetParticle {
    private static final float HALO_BRIGHTNESS = 0.25F;

    private TextureAtlasSprite haloSprite;

    protected LayeredAdditiveParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    protected final void setLayerSprites(SpriteSet sprites) {
        // JSON の2枚はアニメーションではなく、位置・回転・寿命を共有する描画層。
        setSprite(sprites.get(0, 1));
        haloSprite = sprites.get(1, 1);
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        super.render(buffer, camera, partialTicks);
        if (haloSprite == null) {
            return;
        }

        var coreSprite = sprite;
        float red = rCol;
        float green = gCol;
        float blue = bCol;
        try {
            setSprite(haloSprite);
            // alpha を弱めると shader の破棄閾値へ先に達するため、光量は RGB で抑える。
            rCol = red * HALO_BRIGHTNESS;
            gCol = green * HALO_BRIGHTNESS;
            bCol = blue * HALO_BRIGHTNESS;
            super.render(buffer, camera, partialTicks);
        } finally {
            setSprite(coreSprite);
            rCol = red;
            gCol = green;
            bCol = blue;
        }
    }
}
