package jp.aquafactory.apprenticecodex.spell.heavenlyfist;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.model.HeavenlyFistFistModel;
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
import software.bernie.geckolib.util.RenderUtils;

public class HeavenlyFistFistRenderer extends GeoEntityRenderer<HeavenlyFistFistEntity> {
    private static final String FIST_BONE = "fist";
    private static final String CORE_BONE = "core";
    private static final int FIST_MIN_BLOCK_LIGHT = 7;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/heavenly_fist_fist.png");
    private static final RenderType CORE_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("heavenly_fist_core_additive", TEXTURE);

    private boolean renderingCore;

    public HeavenlyFistFistRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new HeavenlyFistFistModel());
        shadowRadius = 0.0F;
    }

    @Override
    public void postRender(PoseStack poseStack, HeavenlyFistFistEntity animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                           int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, alpha);

        if (isReRender) {
            return;
        }

        var progress = animatable.getCoreRedProgress(partialTick);
        var coreRed = Mth.lerp(progress, 0.72F, 1.0F);
        var coreGreen = Mth.lerp(progress, 0.26F, 0.05F);
        var coreBlue = Mth.lerp(progress, 1.0F, 0.04F);
        renderCorePass(model, poseStack, bufferSource, animatable, partialTick, coreRed, coreGreen, coreBlue, alpha);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, HeavenlyFistFistEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        var coreBone = isBoneOrChildOf(bone, CORE_BONE);
        if (!renderingCore && coreBone) {
            renderChildBonesOnly(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        if (renderingCore) {
            if (coreBone) {
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
            return;
        }

        if (isBoneOrChildOf(bone, FIST_BONE)) {
            packedLight = withMinimumBlockLight(packedLight, FIST_MIN_BLOCK_LIGHT);
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        renderingCore = false;
    }

    private void renderCorePass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                HeavenlyFistFistEntity animatable, float partialTick,
                                float red, float green, float blue, float alpha) {
        renderingCore = true;
        try {
            this.reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    CORE_RENDER_TYPE,
                    bufferSource.getBuffer(CORE_RENDER_TYPE),
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    red,
                    green,
                    blue,
                    alpha
            );
        } finally {
            renderingCore = false;
        }
    }

    private void renderChildBonesOnly(PoseStack poseStack, HeavenlyFistFistEntity animatable, GeoBone bone,
                                      RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                      boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                      float red, float green, float blue, float alpha) {
        poseStack.pushPose();

        if (bone.isTrackingMatrices()) {
            org.joml.Matrix4f poseState = new org.joml.Matrix4f(poseStack.last().pose());
            bone.setModelSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.entityRenderTranslations));
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

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }
        return false;
    }

    private static int withMinimumBlockLight(int packedLight, int minimumBlockLight) {
        var blockLight = Math.max(LightTexture.block(packedLight), minimumBlockLight);
        return LightTexture.pack(blockLight, LightTexture.sky(packedLight));
    }
}
