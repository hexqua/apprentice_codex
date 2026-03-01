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
                vc.addVertex(pose.pose(), q.x[i], q.y[i], q.z[i])
                        .setColor(255, 255, 255, 255)
                        .setUv(q.u[i], q.v[i])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight)
                        .setNormal(pose, q.nx, q.ny, q.nz);
            }
        }
    }
}
