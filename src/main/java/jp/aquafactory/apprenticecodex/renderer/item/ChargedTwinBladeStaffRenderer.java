package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.model.ChargedTwinBladeStaffModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtils;

public final class ChargedTwinBladeStaffRenderer extends GeoItemRenderer<ChargedTwinBladeStaff> {
    private static final String MAIN_CORE_BONE = "main_core";
    private static final String SUB_CORE_BONE = "sub_core";
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/charged_twin_blade_staff.png");
    private static final RenderType MAIN_CORE_RENDER_TYPE = RenderType.entityTranslucent(TEXTURE);
    private static final RenderType SUB_CORE_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("charged_twin_blade_staff_sub_core_additive", TEXTURE);

    private CoreRenderPass coreRenderPass = CoreRenderPass.NONE;

    public ChargedTwinBladeStaffRenderer() {
        super(new ChargedTwinBladeStaffModel());
    }

    @Override
    public void postRender(PoseStack poseStack, ChargedTwinBladeStaff animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                           VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);

        if (isReRender) {
            return;
        }

        var mainCoreBrightness = resolveMainCoreBrightness(partialTick);
        renderCorePass(model, poseStack, bufferSource, animatable, CoreRenderPass.MAIN_CORE, MAIN_CORE_RENDER_TYPE, partialTick,
                red * mainCoreBrightness, green * mainCoreBrightness, blue * mainCoreBrightness, alpha);
        renderCorePass(model, poseStack, bufferSource, animatable, CoreRenderPass.SUB_CORE, SUB_CORE_RENDER_TYPE, partialTick,
                1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ChargedTwinBladeStaff animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        var mainCoreBone = isBoneOrChildOf(bone, MAIN_CORE_BONE);
        var subCoreBone = isBoneOrChildOf(bone, SUB_CORE_BONE);

        if (this.coreRenderPass == CoreRenderPass.NONE && (mainCoreBone || subCoreBone)) {
            return;
        }

        if (this.coreRenderPass == CoreRenderPass.MAIN_CORE) {
            renderCorePassBone(
                    poseStack, animatable, bone, mainCoreBone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        if (this.coreRenderPass == CoreRenderPass.SUB_CORE) {
            renderCorePassBone(
                    poseStack, animatable, bone, subCoreBone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.coreRenderPass = CoreRenderPass.NONE;
    }

    private void renderCorePass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                ChargedTwinBladeStaff animatable, CoreRenderPass pass, RenderType renderType, float partialTick,
                                float red, float green, float blue, float alpha) {
        this.coreRenderPass = pass;
        try {
            // core は glint から切り離し、専用パスで発光表現だけを再描画する。
            this.reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    renderType,
                    bufferSource.getBuffer(renderType),
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    red,
                    green,
                    blue,
                    alpha
            );
        } finally {
            this.coreRenderPass = CoreRenderPass.NONE;
        }
    }

    private void renderCorePassBone(PoseStack poseStack, ChargedTwinBladeStaff animatable, GeoBone bone, boolean targetBone,
                                    RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                    boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                    float red, float green, float blue, float alpha) {
        if (targetBone) {
            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        renderChildBonesOnly(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    private void renderChildBonesOnly(PoseStack poseStack, ChargedTwinBladeStaff animatable, GeoBone bone, RenderType renderType,
                                      MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                      float partialTick, int packedLight, int packedOverlay,
                                      float red, float green, float blue, float alpha) {
        poseStack.pushPose();

        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            bone.setModelSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.itemRenderTranslations));
        }

        RenderUtils.prepMatrixForBone(poseStack, bone);
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

    private static float resolveMainCoreBrightness(float partialTick) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return 0.95F;
        }

        float time = level.getGameTime() + partialTick;
        return 0.90F + 0.10F * (0.5F + 0.5F * Mth.sin(time * 0.16F));
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }

    private enum CoreRenderPass {
        NONE,
        MAIN_CORE,
        SUB_CORE
    }
}
