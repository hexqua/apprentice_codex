package jp.aquafactory.apprenticecodex.renderer.extrudedsprite;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class ExtrudedSpriteRenderer {
    public static void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ResourceLocation texture) {
        ExtrudedSpriteMesh mesh = ExtrudedSpriteManager.get(texture);
        if (mesh.quads.isEmpty()) {
            return;
        }

        var vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        var pose = poseStack.last();
        for (var q : mesh.quads) {
            for (var i = 0; i < 4; ++i) {
                vc.vertex(pose.pose(), q.x[i], q.y[i], q.z[i])
                        .color(255, 255, 255, 255)
                        .uv(q.u[i], q.v[i])
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(pose.normal(), q.nx, q.ny, q.nz)
                        .endVertex();
            }
        }
    }
}
