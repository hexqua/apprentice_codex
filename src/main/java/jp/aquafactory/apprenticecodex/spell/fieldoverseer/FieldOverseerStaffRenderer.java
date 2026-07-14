package jp.aquafactory.apprenticecodex.spell.fieldoverseer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.model.FieldOverseerStaffModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtil;

public class FieldOverseerStaffRenderer extends GeoEntityRenderer<FieldOverseerStaffEntity> {
    private static final String STAR_BONE = "star";
    private static final String CORE_BONE = "core";
    private static final int STAR_MIN_BLOCK_LIGHT = 7;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "textures/geo/field_overseer_staff.png");
    private static final RenderType CORE_RENDER_TYPE = ApprenticeRenderTypes.entityAdditiveGlowNoCull(
            "field_overseer_staff_core_additive", TEXTURE);
    private boolean renderingCore;

    public FieldOverseerStaffRenderer(EntityRendererProvider.Context context) {
        super(context, new FieldOverseerStaffModel());
        shadowRadius = 0.2F;
    }

    @Override
    public void postRender(PoseStack poseStack, FieldOverseerStaffEntity animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                           float partialTick, int packedLight, int packedOverlay,
                           int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
        if (isReRender) return;

        var insufficient = !animatable.hasEnoughManaToAttack();
        var warningAlpha = insufficient
                ? 0.95F + 0.05F * Mth.sin((animatable.tickCount + partialTick) * 0.25F)
                : 1.0F;
        var coreColour = (Mth.clamp(Math.round(warningAlpha * 255.0F), 0, 255) << 24)
                | (insufficient ? 0x00FF0000 : 0x00FFFFFF);
        renderCorePass(model, poseStack, bufferSource, animatable, partialTick, coreColour);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, FieldOverseerStaffEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  int colour) {
        var coreBone = isBoneOrChildOf(bone, CORE_BONE);
        if (!renderingCore && coreBone) {
            renderChildBonesOnly(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, colour);
            return;
        }
        if (renderingCore) {
            if (coreBone) {
                super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
                        isReRender, partialTick, packedLight, packedOverlay, colour);
            } else {
                renderChildBonesOnly(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                        partialTick, packedLight, packedOverlay, colour);
            }
            return;
        }
        if (isBoneOrChildOf(bone, STAR_BONE)) {
            packedLight = LightTexture.pack(Math.max(LightTexture.block(packedLight), STAR_MIN_BLOCK_LIGHT),
                    LightTexture.sky(packedLight));
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    private void renderCorePass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                FieldOverseerStaffEntity animatable, float partialTick,
                                int colour) {
        renderingCore = true;
        try {
            reRender(model, poseStack, bufferSource, animatable, CORE_RENDER_TYPE,
                    bufferSource.getBuffer(CORE_RENDER_TYPE), partialTick, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, colour);
        } finally {
            renderingCore = false;
        }
    }

    private void renderChildBonesOnly(PoseStack poseStack, FieldOverseerStaffEntity animatable, GeoBone bone,
                                      RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                      boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                      int colour) {
        poseStack.pushPose();
        if (bone.isTrackingMatrices()) {
            var poseState = new org.joml.Matrix4f(poseStack.last().pose());
            bone.setModelSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, entityRenderTranslations));
        }
        RenderUtil.prepMatrixForBone(poseStack, bone);
        renderChildBones(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
        poseStack.popPose();
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootName) {
        for (var current = bone; current != null; current = current.getParent()) {
            if (rootName.equals(current.getName())) return true;
        }
        return false;
    }
}
