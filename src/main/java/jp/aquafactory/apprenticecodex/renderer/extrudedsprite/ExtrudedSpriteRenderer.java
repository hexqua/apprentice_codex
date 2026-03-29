package jp.aquafactory.apprenticecodex.renderer.extrudedsprite;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class ExtrudedSpriteRenderer {
    public static void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ResourceLocation texture) {
        var pose = poseStack.last();
        render(pose.pose(), pose.normal(), buffer, packedLight, texture);
    }

    public static void renderCenteredWithIndependentRotation(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ResourceLocation texture) {
        var pose = poseStack.last();
        renderCenteredWithIndependentRotation(pose.pose(), pose.normal(), buffer, packedLight, texture);
    }

    public static void render(Matrix4f poseMatrix, Matrix3f normalMatrix, MultiBufferSource buffer, int packedLight, ResourceLocation texture) {
        ExtrudedSpriteMesh mesh = ExtrudedSpriteManager.get(texture);
        if (mesh.quads.isEmpty()) {
            return;
        }

        render(mesh, poseMatrix, normalMatrix, buffer, packedLight, texture);
    }

    public static void renderCenteredWithIndependentRotation(Matrix4f poseMatrix, Matrix3f normalMatrix, MultiBufferSource buffer, int packedLight, ResourceLocation texture) {
        ExtrudedSpriteMesh mesh = ExtrudedSpriteManager.get(texture);
        if (mesh.quads.isEmpty()) {
            return;
        }

        // 呼び出し側が独立に組んだ回転を崩さないよう、原点寄せだけここで行う.
        Matrix4f centeredPoseMatrix = new Matrix4f(poseMatrix).translate(-mesh.centerX, -mesh.centerY, -mesh.centerZ);
        render(mesh, centeredPoseMatrix, normalMatrix, buffer, packedLight, texture);
    }

    private static void render(ExtrudedSpriteMesh mesh, Matrix4f poseMatrix, Matrix3f normalMatrix, MultiBufferSource buffer, int packedLight, ResourceLocation texture) {

        var vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        for (var q : mesh.quads) {
            for (var i = 0; i < 4; ++i) {
                vc.vertex(poseMatrix, q.x[i], q.y[i], q.z[i])
                        .color(255, 255, 255, 255)
                        .uv(q.u[i], q.v[i])
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(normalMatrix, q.nx, q.ny, q.nz)
                        .endVertex();
            }
        }
    }
}
