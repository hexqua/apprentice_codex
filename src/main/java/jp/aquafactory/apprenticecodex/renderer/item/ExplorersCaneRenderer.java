package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.offhand.ExplorersCane;
import jp.aquafactory.apprenticecodex.model.ExplorersCaneModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ExplorersCaneRenderer extends GeoItemRenderer<ExplorersCane> {
    private static final String COMPASS_CUBE_BONE = "compass_cube";

    public ExplorersCaneRenderer() {
        super(new ExplorersCaneModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ExplorersCane animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (isBoneOrChildOf(bone, COMPASS_CUBE_BONE)) {
            // compass_cube は常時発光表示し、アイテム glint を乗せない。
            var cubeBuffer = bufferSource.getBuffer(renderType);
            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, cubeBuffer, isReRender, partialTick,
                    LightTexture.FULL_BRIGHT, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }
}
