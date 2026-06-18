package jp.aquafactory.apprenticecodex.spell.automagnet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.model.AutoMagnetFamiliarModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class AutoMagnetFamiliarRenderer extends GeoEntityRenderer<AutoMagnetFamiliarEntity> {
    private static final String GEM_BONE = "gem";
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/auto_magnet_familiar.png");
    private static final RenderType GEM_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly("auto_magnet_gem_emissive", TEXTURE);

    private boolean renderingGem;

    public AutoMagnetFamiliarRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new AutoMagnetFamiliarModel<>());
    }

    @Override
    public void postRender(PoseStack poseStack, AutoMagnetFamiliarEntity animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                           int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, alpha);

        if (isReRender) {
            return;
        }

        if (animatable.isCollectionBlocked()) {
            renderGemPass(model, poseStack, bufferSource, animatable, partialTick, 1.0F, 0.08F, 0.04F, alpha);
        } else {
            renderGemPass(model, poseStack, bufferSource, animatable, partialTick, red, green, blue, alpha);
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, AutoMagnetFamiliarEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        var gemBone = isBoneOrChildOf(bone, GEM_BONE);
        if (!renderingGem && gemBone) {
            renderChildBonesOnly(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        if (renderingGem) {
            if (gemBone) {
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

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        renderingGem = false;
    }

    private void renderGemPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                               AutoMagnetFamiliarEntity animatable, float partialTick,
                               float red, float green, float blue, float alpha) {
        renderingGem = true;
        try {
            this.reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    GEM_RENDER_TYPE,
                    bufferSource.getBuffer(GEM_RENDER_TYPE),
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    red,
                    green,
                    blue,
                    alpha
            );
        } finally {
            renderingGem = false;
        }
    }

    private void renderChildBonesOnly(PoseStack poseStack, AutoMagnetFamiliarEntity animatable, GeoBone bone,
                                      RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                      boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                      float red, float green, float blue, float alpha) {
        poseStack.pushPose();

        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
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
}
