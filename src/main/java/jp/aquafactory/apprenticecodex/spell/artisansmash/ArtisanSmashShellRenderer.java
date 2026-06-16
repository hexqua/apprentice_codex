package jp.aquafactory.apprenticecodex.spell.artisansmash;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.renderer.extrudedsprite.ExtrudedSpriteRenderer;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
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

public class ArtisanSmashShellRenderer extends EntityRenderer<ArtisanSmashShellEntity> {
    private static final ResourceLocation SHELL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/artisan_smash_shell.png");
    private static final ResourceLocation BURST_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/unite_luna_moon.png");

    private final RenderType burstRenderType =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("artisan_smash_shell_burst_additive", BURST_TEXTURE);

    public ArtisanSmashShellRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(@NotNull ArtisanSmashShellEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        if (entity.isBursting()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(entity.getBurstSpinDegrees(partialTicks)));
            var alpha = entity.getBurstCubeAlpha(partialTicks);
            if (alpha > 0.001f) {
                drawCube(
                        poseStack,
                        buffer.getBuffer(burstRenderType),
                        entity.getBurstCubeScale(partialTicks),
                        alpha,
                        1.0f,
                        0.48f,
                        0.12f,
                        true
                );
            }
        } else {
            var motion = entity.getDeltaMovement();
            var yawPitch = motion.lengthSqr() > 1.0e-6
                    ? RotationTools.calculateYawPitchByDirection(motion)
                    : RotationTools.calculateYawPitchByEntity(entity, partialTicks);

            poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
            poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-135.0f));
            poseStack.translate(-0.5f, -0.5f, -(1.0f / 16.0f) * 0.5f);

            ExtrudedSpriteRenderer.render(poseStack, buffer, packedLight, SHELL_TEXTURE);
        }
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ArtisanSmashShellEntity entity) {
        return SHELL_TEXTURE;
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
