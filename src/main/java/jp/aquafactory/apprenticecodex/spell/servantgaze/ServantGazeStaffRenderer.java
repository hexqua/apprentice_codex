package jp.aquafactory.apprenticecodex.spell.servantgaze;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.model.ServantGazeStaffModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class ServantGazeStaffRenderer extends GeoEntityRenderer<ServantGazeStaffEntity> {
    private static final int STAR_MIN_BLOCK_LIGHT = 7;
    private static final net.minecraft.resources.ResourceLocation TEXTURE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID, "textures/geo/servant_gaze_staff.png");
    private static final RenderType CORE_RENDER_TYPE = ApprenticeRenderTypes.entityAdditiveGlowNoCull(
            "servant_gaze_staff_core_additive", TEXTURE);
    private boolean renderingCore;

    public ServantGazeStaffRenderer(EntityRendererProvider.Context context) {
        super(context, new ServantGazeStaffModel());
        shadowRadius = 0.0F;
    }

    @Override
    public void postRender(PoseStack poseStack, ServantGazeStaffEntity animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                           float partialTick, int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);
        if (isReRender) return;
        renderingCore = true;
        try {
            reRender(model, poseStack, bufferSource, animatable, CORE_RENDER_TYPE,
                    bufferSource.getBuffer(CORE_RENDER_TYPE), partialTick, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } finally {
            renderingCore = false;
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ServantGazeStaffEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        var core = isBoneOrChildOf(bone, "core");
        if (!renderingCore && core) {
            renderChildren(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }
        if (renderingCore && !core) {
            renderChildren(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }
        if (!renderingCore && isBoneOrChildOf(bone, "star")) {
            packedLight = LightTexture.pack(Math.max(LightTexture.block(packedLight), STAR_MIN_BLOCK_LIGHT),
                    LightTexture.sky(packedLight));
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private void renderChildren(PoseStack poseStack, ServantGazeStaffEntity animatable, GeoBone bone,
                                RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        if (bone.isTrackingMatrices()) {
            var poseState = new org.joml.Matrix4f(poseStack.last().pose());
            bone.setModelSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, entityRenderTranslations));
        }
        RenderUtils.prepMatrixForBone(poseStack, bone);
        renderChildBones(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String name) {
        for (var current = bone; current != null; current = current.getParent()) {
            if (name.equals(current.getName())) return true;
        }
        return false;
    }
}
