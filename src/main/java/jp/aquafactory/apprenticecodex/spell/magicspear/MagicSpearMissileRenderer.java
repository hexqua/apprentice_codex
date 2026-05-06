package jp.aquafactory.apprenticecodex.spell.magicspear;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.model.MagicSpearMissileModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MagicSpearMissileRenderer extends GeoEntityRenderer<MagicSpearMissileEntity> {
    private static final String TIP_CORE_BONE = "tip_core";
    private static final String CHAMBER_CORE_BONE = "chamber_core";
    private static final String REAR_CORE_BONE = "rear_core";
    private static final int FULL_BRIGHT_LIGHT = 0x00F000F0;
    private static final net.minecraft.resources.ResourceLocation MISSILE_TEXTURE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/magic_spear_missile.png");
    private static final net.minecraft.resources.ResourceLocation BURST_TEXTURE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/unite_luna_moon.png");

    private final RenderType emissiveRenderType =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("magic_spear_missile_core_additive", MISSILE_TEXTURE);
    private final RenderType burstRenderType =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("magic_spear_missile_burst_additive", BURST_TEXTURE);

    public MagicSpearMissileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MagicSpearMissileModel<>());
        shadowRadius = 0.0f;
    }

    @Override
    public void render(@NotNull MagicSpearMissileEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        if (entity.isBursting()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(entity.getBurstSpinDegrees(partialTick)));
            var alpha = entity.getBurstCubeAlpha(partialTick);
            if (alpha > 0.001f) {
                drawCube(
                        poseStack,
                        buffer.getBuffer(burstRenderType),
                        entity.getBurstCubeScale(partialTick),
                        alpha,
                        1.0f,
                        0.48f,
                        0.12f,
                        true
                );
            }
        } else {
            var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTick);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
            poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        }
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, MagicSpearMissileEntity animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour
        );

        if (!isEmissiveBone(bone)) {
            return;
        }

        var emissiveBuffer = bufferSource.getBuffer(emissiveRenderType);
        super.renderRecursively(
                poseStack, animatable, bone, emissiveRenderType, bufferSource, emissiveBuffer, isReRender, partialTick,
                FULL_BRIGHT_LIGHT, packedOverlay, packColor(1.0f, 1.0f, 0.72f, 0.24f)
        );
    }

    private static boolean isEmissiveBone(GeoBone bone) {
        return switch (bone.getName()) {
            case TIP_CORE_BONE, CHAMBER_CORE_BONE, REAR_CORE_BONE -> true;
            default -> false;
        };
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
        var transformedNormal = normalMatrix.transform(new org.joml.Vector3f(normalX, normalY, normalZ));
        buffer.addVertex(poseMatrix, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
    }

    private static int packColor(float alpha, float red, float green, float blue) {
        return (toChannel(alpha) << 24) | (toChannel(red) << 16) | (toChannel(green) << 8) | toChannel(blue);
    }

    private static int toChannel(float value) {
        return Math.round(Mth.clamp(value, 0.0f, 1.0f) * 255.0f);
    }
}
