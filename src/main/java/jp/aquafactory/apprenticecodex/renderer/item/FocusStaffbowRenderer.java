package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeEffectState;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientRenderState;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowItemRenderContext;
import jp.aquafactory.apprenticecodex.model.FocusStaffbowModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtils;

public final class FocusStaffbowRenderer extends GeoItemRenderer<FocusStaffbow> {
    private static final String CORE_BONE = "core";
    private FocusStaffbowChargeEffectState chargeEffectState = FocusStaffbowChargeEffectState.HIDDEN;

    public FocusStaffbowRenderer() {
        super(new FocusStaffbowModel());
    }

    @Override
    public void preRender(PoseStack poseStack, FocusStaffbow animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);

        if (!isReRender) {
            this.chargeEffectState = FocusStaffbowClientRenderState.resolveChargeEffectState(
                    this.currentItemStack,
                    this.renderPerspective,
                    FocusStaffbowItemRenderContext.getRenderingEntity(),
                    partialTick
            );
        }
    }

    @Override
    public RenderType getRenderType(FocusStaffbow animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, FocusStaffbow animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        if (!CORE_BONE.equals(bone.getName())) {
            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        renderCoreBoneWithChargeEffect(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.chargeEffectState = FocusStaffbowChargeEffectState.HIDDEN;
    }

    private void renderCoreBoneWithChargeEffect(PoseStack poseStack, FocusStaffbow animatable, GeoBone bone,
                                                RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                                boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                                float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            bone.setModelSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.itemRenderTranslations));
        }

        RenderUtils.prepMatrixForBone(poseStack, bone);
        renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        if (!isReRender) {
            applyRenderLayersForBone(poseStack, animatable, bone, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
            renderCoreChargeEffect(poseStack, bufferSource, bone, partialTick);
        }
        renderChildBones(
                poseStack,
                animatable,
                bone,
                renderType,
                bufferSource,
                buffer,
                isReRender,
                partialTick,
                packedLight,
                packedOverlay,
                red,
                green,
                blue,
                alpha
        );
        poseStack.popPose();
    }

    private void renderCoreChargeEffect(PoseStack poseStack, MultiBufferSource bufferSource, GeoBone bone, float partialTick) {
        if (!this.chargeEffectState.visible()) {
            return;
        }

        poseStack.pushPose();
        // core ボーンの pivot を魔力チャージの基準点として扱い、モデル側の持ち替え/一人称変形へ追従させる。
        RenderUtils.translateToPivotPoint(poseStack, bone);
        FocusStaffbowChargeEffectRenderer.render(poseStack, bufferSource, this.chargeEffectState, partialTick);
        poseStack.popPose();
    }
}
