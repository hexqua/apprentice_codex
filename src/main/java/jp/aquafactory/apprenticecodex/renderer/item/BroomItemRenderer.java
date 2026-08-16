package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.broom.AbstractBroomItem;
import jp.aquafactory.apprenticecodex.model.BroomModel;
import jp.aquafactory.apprenticecodex.renderer.BroomRenderSupport;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class BroomItemRenderer<T extends AbstractBroomItem> extends GeoItemRenderer<T> {
    protected BroomItemRenderer(BroomModel<T> model) {
        super(model);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        if (BroomRenderSupport.isBoneOrChildOf(bone, BroomRenderSupport.STAR_BONE)) {
            renderEmissiveBone(poseStack, animatable, bone, bufferSource, isReRender, partialTick, packedOverlay,
                    BroomRenderSupport.resolveStarColour(partialTick));
            return;
        }

        if (BroomRenderSupport.isBoneOrChildOf(bone, BroomRenderSupport.CORE_BONE)) {
            renderEmissiveBone(poseStack, animatable, bone, bufferSource, isReRender, partialTick, packedOverlay,
                    BroomRenderSupport.resolveItemCoreColour(partialTick));
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour
        );
    }

    private void renderEmissiveBone(PoseStack poseStack, T animatable, GeoBone bone,
                                    MultiBufferSource bufferSource, boolean isReRender, float partialTick,
                                    int packedOverlay, int colour) {
        var emissiveRenderType = RenderType.entityTranslucent(getTextureLocation(animatable));
        super.renderRecursively(
                poseStack, animatable, bone, emissiveRenderType, bufferSource, bufferSource.getBuffer(emissiveRenderType),
                isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay, colour
        );
    }
}
