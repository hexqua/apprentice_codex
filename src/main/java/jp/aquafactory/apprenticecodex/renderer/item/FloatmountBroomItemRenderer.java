package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.FloatmountBroomItem;
import jp.aquafactory.apprenticecodex.model.FloatmountBroomModel;
import jp.aquafactory.apprenticecodex.renderer.FloatmountBroomRenderSupport;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class FloatmountBroomItemRenderer extends GeoItemRenderer<FloatmountBroomItem> {
    public FloatmountBroomItemRenderer() {
        super(new FloatmountBroomModel<>());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, FloatmountBroomItem animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        if (FloatmountBroomRenderSupport.isBoneOrChildOf(bone, FloatmountBroomRenderSupport.STAR_BONE)) {
            renderEmissiveBone(poseStack, animatable, bone, bufferSource, isReRender, partialTick, packedOverlay,
                    FloatmountBroomRenderSupport.resolveStarColour(partialTick));
            return;
        }

        if (FloatmountBroomRenderSupport.isBoneOrChildOf(bone, FloatmountBroomRenderSupport.CORE_BONE)) {
            renderEmissiveBone(poseStack, animatable, bone, bufferSource, isReRender, partialTick, packedOverlay,
                    FloatmountBroomRenderSupport.resolveItemCoreColour(partialTick));
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour
        );
    }

    private void renderEmissiveBone(PoseStack poseStack, FloatmountBroomItem animatable, GeoBone bone,
                                    MultiBufferSource bufferSource, boolean isReRender, float partialTick,
                                    int packedOverlay, int colour) {
        var emissiveRenderType = RenderType.entityTranslucent(getTextureLocation(animatable));
        super.renderRecursively(
                poseStack, animatable, bone, emissiveRenderType, bufferSource, bufferSource.getBuffer(emissiveRenderType),
                isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay, colour
        );
    }
}
