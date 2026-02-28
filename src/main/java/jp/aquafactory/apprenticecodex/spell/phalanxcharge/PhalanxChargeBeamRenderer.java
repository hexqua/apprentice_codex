package jp.aquafactory.apprenticecodex.spell.phalanxcharge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PhalanxChargeBeamRenderer extends EntityRenderer<PhalanxChargeBeamEntity> {
    private static final ResourceLocation BEAM_TEX =
            ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    private static final int FULL_BRIGHT_LIGHT = 0x00F000F0;

    public PhalanxChargeBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(@NotNull PhalanxChargeBeamEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        var direction = Vec3.directionFromRotation(yawPitch.pitch(), yawPitch.yaw()).normalize();
        var from = new Vector3f(0.0f, 1.0f, 0.0f);
        var to = new Vector3f((float) direction.x, (float) direction.y, (float) direction.z);
        var rotation = new Quaternionf().rotationTo(from, to);

        // Cubic.
        var lifeProgress = entity.getLifeProgress(partialTicks);
        lifeProgress = lifeProgress * lifeProgress * lifeProgress;

        var radiusScale = Mth.lerp(lifeProgress, 0.1f, 2f);
        var alphaScale = Mth.lerp(lifeProgress, 0.75f, 0.1f);

        var length = entity.getLength();
        var radius = entity.getRadius() * radiusScale;
        var time = (entity.tickCount + partialTicks) * 0.25f;
        var beamBuffer = buffer.getBuffer(ApprenticeRenderTypes.beamNoCull(BEAM_TEX));

        poseStack.pushPose();
        poseStack.mulPose(rotation);
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTicks) * 7.1f));
        drawBeam(poseStack, beamBuffer, length, radius, 0.55f * alphaScale, time);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(rotation);
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTicks) * 3.3f));
        drawBeam(poseStack, beamBuffer, length, radius * 0.7f, 0.95f * alphaScale, time);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull PhalanxChargeBeamEntity entity) {
        return BEAM_TEX;
    }

    private void drawBeam(PoseStack poseStack, VertexConsumer consumer, float length, float radius, float alpha, float uvParameter) {
        var v0 = -uvParameter;
        var v1 = v0 + length;

        var x0 = -radius;
        var x1 = radius;
        var z0 = -radius;
        var z1 = radius;
        var y0 = 0.0f;
        var y1 = length;

        var last = poseStack.last();
        var poseMat = last.pose();
        var normalMat = last.normal();

        addQuad(poseMat, normalMat, consumer,
                x1, y0, z0, 0f, v0,
                x1, y1, z0, 0f, v1,
                x1, y1, z1, 1f, v1,
                x1, y0, z1, 1f, v0,
                alpha,
                1f, 0f, 0f);

        addQuad(poseMat, normalMat, consumer,
                x0, y0, z1, 0f, v0,
                x0, y1, z1, 0f, v1,
                x0, y1, z0, 1f, v1,
                x0, y0, z0, 1f, v0,
                alpha,
                -1f, 0f, 0f);

        addQuad(poseMat, normalMat, consumer,
                x1, y0, z1, 0f, v0,
                x1, y1, z1, 0f, v1,
                x0, y1, z1, 1f, v1,
                x0, y0, z1, 1f, v0,
                alpha,
                0f, 0f, 1f);

        addQuad(poseMat, normalMat, consumer,
                x0, y0, z0, 0f, v0,
                x0, y1, z0, 0f, v1,
                x1, y1, z0, 1f, v1,
                x1, y0, z0, 1f, v0,
                alpha,
                0f, 0f, -1f);

        addQuad(poseMat, normalMat, consumer,
                x0, y0, z0, 0f, 0f,
                x0, y0, z1, 0f, 1f,
                x1, y0, z1, 1f, 1f,
                x1, y0, z0, 1f, 0f,
                alpha,
                0f, -1f, 0f);

        addQuad(poseMat, normalMat, consumer,
                x1, y1, z0, 0f, 0f,
                x1, y1, z1, 0f, 1f,
                x0, y1, z1, 1f, 1f,
                x0, y1, z0, 1f, 0f,
                alpha,
                0f, 1f, 0f);
    }

    private static void addQuad(Matrix4f poseMat, Matrix3f normalMat, VertexConsumer vertexConsumer,
                                float x0, float y0, float z0, float u0, float v0,
                                float x1, float y1, float z1, float u1, float v1,
                                float x2, float y2, float z2, float u2, float v2,
                                float x3, float y3, float z3, float u3, float v3,
                                float alpha,
                                float nx, float ny, float nz) {
        vertexConsumer.vertex(poseMat, x0, y0, z0).color(1.0f, 1.0f, 1.0f, alpha).uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT_LIGHT)
                .normal(normalMat, nx, ny, nz).endVertex();
        vertexConsumer.vertex(poseMat, x1, y1, z1).color(1.0f, 1.0f, 1.0f, alpha).uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT_LIGHT)
                .normal(normalMat, nx, ny, nz).endVertex();
        vertexConsumer.vertex(poseMat, x2, y2, z2).color(1.0f, 1.0f, 1.0f, alpha).uv(u2, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT_LIGHT)
                .normal(normalMat, nx, ny, nz).endVertex();
        vertexConsumer.vertex(poseMat, x3, y3, z3).color(1.0f, 1.0f, 1.0f, alpha).uv(u3, v3)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT_LIGHT)
                .normal(normalMat, nx, ny, nz).endVertex();
    }
}
