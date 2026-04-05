package jp.aquafactory.apprenticecodex.spell.uniteluna;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class UniteLunaMoonRenderer extends EntityRenderer<UniteLunaMoonEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/unite_luna_moon.png");
    private static final RenderType SOLID_RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);
    private static final RenderType FADE_RENDER_TYPE =
            ApprenticeRenderTypes.entityTranslucentNoCull("unite_luna_moon_translucent", TEXTURE);
    private static final RenderType ADDITIVE_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("unite_luna_moon_additive", TEXTURE);

    public UniteLunaMoonRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(@NotNull UniteLunaMoonEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getSpinDegrees(partialTicks)));

        var mainAlpha = entity.getMainCubeAlpha(partialTicks);
        if (mainAlpha > 0.001f) {
            var mainRenderType = mainAlpha >= 0.995f ? SOLID_RENDER_TYPE : FADE_RENDER_TYPE;
            drawCube(poseStack, bufferSource.getBuffer(mainRenderType), UniteLunaMoonEntity.CUBE_SIZE, mainAlpha, 1.0f, 1.0f, 1.0f, false);
        }

        if (entity.getBurstKind() == UniteLunaMoonEntity.BURST_KIND_EXPLOSION) {
            var burstAlpha = entity.getBurstCubeAlpha(partialTicks);
            if (burstAlpha > 0.001f) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(entity.getBurstSpinDegrees(partialTicks)));
                drawCube(
                        poseStack,
                        bufferSource.getBuffer(ADDITIVE_RENDER_TYPE),
                        entity.getBurstCubeScale(partialTicks),
                        burstAlpha,
                        0.88f,
                        0.96f,
                        1.0f,
                        true
                );
                poseStack.popPose();
            }
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull UniteLunaMoonEntity entity) {
        return TEXTURE;
    }

    private static void drawCube(PoseStack poseStack, VertexConsumer buffer, float size, float alpha,
                                 float red, float green, float blue, boolean scaleRgbByAlpha) {
        var half = size * 0.5f;
        var pose = poseStack.last();
        var poseMatrix = pose.pose();
        var normalMatrix = pose.normal();
        var resolvedRed = scaleRgbByAlpha ? red * alpha : red;
        var resolvedGreen = scaleRgbByAlpha ? green * alpha : green;
        var resolvedBlue = scaleRgbByAlpha ? blue * alpha : blue;

        addQuad(buffer, poseMatrix, normalMatrix,
                -half, -half, half,
                half, -half, half,
                half, half, half,
                -half, half, half,
                0.0f, 0.0f, 1.0f,
                resolvedRed, resolvedGreen, resolvedBlue, alpha);
        addQuad(buffer, poseMatrix, normalMatrix,
                half, -half, -half,
                -half, -half, -half,
                -half, half, -half,
                half, half, -half,
                0.0f, 0.0f, -1.0f,
                resolvedRed, resolvedGreen, resolvedBlue, alpha);
        addQuad(buffer, poseMatrix, normalMatrix,
                -half, -half, -half,
                -half, -half, half,
                -half, half, half,
                -half, half, -half,
                -1.0f, 0.0f, 0.0f,
                resolvedRed, resolvedGreen, resolvedBlue, alpha);
        addQuad(buffer, poseMatrix, normalMatrix,
                half, -half, half,
                half, -half, -half,
                half, half, -half,
                half, half, half,
                1.0f, 0.0f, 0.0f,
                resolvedRed, resolvedGreen, resolvedBlue, alpha);
        addQuad(buffer, poseMatrix, normalMatrix,
                -half, half, -half,
                half, half, -half,
                half, half, half,
                -half, half, half,
                0.0f, 1.0f, 0.0f,
                resolvedRed, resolvedGreen, resolvedBlue, alpha);
        addQuad(buffer, poseMatrix, normalMatrix,
                -half, -half, half,
                half, -half, half,
                half, -half, -half,
                -half, -half, -half,
                0.0f, -1.0f, 0.0f,
                resolvedRed, resolvedGreen, resolvedBlue, alpha);
    }

    private static void addQuad(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float normalX, float normalY, float normalZ,
                                float red, float green, float blue, float alpha) {
        addVertex(buffer, poseMatrix, normalMatrix, x1, y1, z1, 0.0f, 1.0f, normalX, normalY, normalZ, red, green, blue, alpha);
        addVertex(buffer, poseMatrix, normalMatrix, x2, y2, z2, 1.0f, 1.0f, normalX, normalY, normalZ, red, green, blue, alpha);
        addVertex(buffer, poseMatrix, normalMatrix, x3, y3, z3, 1.0f, 0.0f, normalX, normalY, normalZ, red, green, blue, alpha);
        addVertex(buffer, poseMatrix, normalMatrix, x4, y4, z4, 0.0f, 0.0f, normalX, normalY, normalZ, red, green, blue, alpha);
    }

    private static void addVertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                                  float x, float y, float z, float u, float v,
                                  float normalX, float normalY, float normalZ,
                                  float red, float green, float blue, float alpha) {
        buffer.vertex(poseMatrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .endVertex();
    }
}
