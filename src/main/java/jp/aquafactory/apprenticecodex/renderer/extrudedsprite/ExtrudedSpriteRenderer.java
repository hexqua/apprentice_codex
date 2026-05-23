package jp.aquafactory.apprenticecodex.renderer.extrudedsprite;

import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class ExtrudedSpriteRenderer {
    public static void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ResourceLocation texture) {
        render(poseStack, buffer, packedLight, texture, RenderMode.DEFAULT);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ResourceLocation texture, RenderMode renderMode) {
        var pose = poseStack.last();
        render(pose.pose(), pose.normal(), buffer, packedLight, texture, renderMode);
    }

    public static void renderCenteredWithIndependentRotation(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ResourceLocation texture) {
        renderCenteredWithIndependentRotation(poseStack, buffer, packedLight, texture, RenderMode.DEFAULT);
    }

    public static void renderCenteredWithIndependentRotation(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                             ResourceLocation texture, RenderMode renderMode) {
        var pose = poseStack.last();
        renderCenteredWithIndependentRotation(pose.pose(), pose.normal(), buffer, packedLight, texture, renderMode);
    }

    public static void render(Matrix4f poseMatrix, Matrix3f normalMatrix, MultiBufferSource buffer, int packedLight, ResourceLocation texture) {
        render(poseMatrix, normalMatrix, buffer, packedLight, texture, RenderMode.DEFAULT);
    }

    public static void render(Matrix4f poseMatrix, Matrix3f normalMatrix, MultiBufferSource buffer, int packedLight,
                              ResourceLocation texture, RenderMode renderMode) {
        ExtrudedSpriteMesh mesh = ExtrudedSpriteManager.get(texture);
        if (mesh.quads.isEmpty()) {
            return;
        }

        render(mesh, poseMatrix, normalMatrix, buffer, packedLight, texture, renderMode);
    }

    public static void renderCenteredWithIndependentRotation(Matrix4f poseMatrix, Matrix3f normalMatrix, MultiBufferSource buffer, int packedLight, ResourceLocation texture) {
        renderCenteredWithIndependentRotation(poseMatrix, normalMatrix, buffer, packedLight, texture, RenderMode.DEFAULT);
    }

    public static void renderCenteredWithIndependentRotation(Matrix4f poseMatrix, Matrix3f normalMatrix, MultiBufferSource buffer,
                                                             int packedLight, ResourceLocation texture, RenderMode renderMode) {
        ExtrudedSpriteMesh mesh = ExtrudedSpriteManager.get(texture);
        if (mesh.quads.isEmpty()) {
            return;
        }

        // 呼び出し側が独立に組んだ回転を崩さないよう、原点寄せだけここで行う.
        Matrix4f centeredPoseMatrix = new Matrix4f(poseMatrix).translate(-mesh.centerX, -mesh.centerY, -mesh.centerZ);
        render(mesh, centeredPoseMatrix, normalMatrix, buffer, packedLight, texture, renderMode);
    }

    private static void render(ExtrudedSpriteMesh mesh, Matrix4f poseMatrix, Matrix3f normalMatrix, MultiBufferSource buffer, int packedLight,
                               ResourceLocation texture, RenderMode renderMode) {
        var vc = buffer.getBuffer(resolveRenderType(texture, renderMode));
        var resolvedLight = renderMode == RenderMode.DEFAULT ? packedLight : LightTexture.FULL_BRIGHT;
        for (var q : mesh.quads) {
            for (var i = 0; i < 4; ++i) {
                var transformedNormal = new org.joml.Vector3f(q.nx, q.ny, q.nz)
                        .mul(normalMatrix)
                        .normalize();
                vc.addVertex(poseMatrix, q.x[i], q.y[i], q.z[i])
                        .setColor(255, 255, 255, 255)
                        .setUv(q.u[i], q.v[i])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(resolvedLight)
                        .setNormal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
            }
        }
    }

    private static RenderType resolveRenderType(ResourceLocation texture, RenderMode renderMode) {
        return switch (renderMode) {
            case DEFAULT -> RenderType.entityCutoutNoCull(texture);
            case EMISSIVE -> RenderType.entityTranslucent(texture);
            case ADDITIVE_COLOR_ONLY -> ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly("extruded_sprite_additive_color_only", texture);
        };
    }

    public enum RenderMode {
        DEFAULT,
        EMISSIVE,
        ADDITIVE_COLOR_ONLY
    }
}
