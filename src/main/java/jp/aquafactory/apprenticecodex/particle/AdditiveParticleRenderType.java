package jp.aquafactory.apprenticecodex.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

@OnlyIn(Dist.CLIENT)
public final class AdditiveParticleRenderType {
    public static final ParticleRenderType PARTICLE_SHEET_ADDITIVE = new ParticleRenderType() {
        @Override
        public void begin(@NotNull BufferBuilder builder, @NotNull TextureManager textureManager) {
            var minecraft = Minecraft.getInstance();
            // CUSTOM の回収エフェクトは entity 描画の後処理で描画先とライトマップを戻すため、
            // 後続の加算バッチは ParticleEngine 開始時の状態を引き継げるとは限らない。
            var renderTarget = Minecraft.useShaderTransparency()
                    ? minecraft.levelRenderer.getParticlesTarget()
                    : minecraft.getMainRenderTarget();
            if (renderTarget == null) {
                renderTarget = minecraft.getMainRenderTarget();
            }
            renderTarget.bindWrite(false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            // 代替手段ないので抑制.
            //noinspection deprecation
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            minecraft.gameRenderer.lightTexture().turnOnLightLayer();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "PARTICLE_SHEET_ADDITIVE";
        }
    };

    private AdditiveParticleRenderType() {
    }
}
