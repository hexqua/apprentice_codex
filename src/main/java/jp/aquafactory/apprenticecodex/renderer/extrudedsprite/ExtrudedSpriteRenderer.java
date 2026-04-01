package jp.aquafactory.apprenticecodex.renderer.extrudedsprite;

import com.mojang.blaze3d.vertex.PoseStack;
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
        // emissive は FULL_BRIGHT のみを保証し、glow 用 additive pass とは分けて扱う。
        var vc = buffer.getBuffer(renderMode == RenderMode.EMISSIVE
                ? RenderType.entityTranslucent(texture)
                : RenderType.entityCutoutNoCull(texture));
        var resolvedLight = renderMode == RenderMode.EMISSIVE ? LightTexture.FULL_BRIGHT : packedLight;
        for (var q : mesh.quads) {
            for (var i = 0; i < 4; ++i) {
                vc.vertex(poseMatrix, q.x[i], q.y[i], q.z[i])
                        .color(255, 255, 255, 255)
                        .uv(q.u[i], q.v[i])
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(resolvedLight)
                        .normal(normalMatrix, q.nx, q.ny, q.nz)
                        .endVertex();
            }
        }
    }

    public enum RenderMode {
        DEFAULT,
        EMISSIVE
    }
}
