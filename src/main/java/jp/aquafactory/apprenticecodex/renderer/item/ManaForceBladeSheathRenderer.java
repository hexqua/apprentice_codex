package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeSheathItem;
import jp.aquafactory.apprenticecodex.model.ManaForceBladeSheathModel;
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

public class ManaForceBladeSheathRenderer extends GeoItemRenderer<ManaForceBladeSheathItem> {
    private static final String HOLDER_EMISSIVE_BONE = "holder_emissive";
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/mana_force_blade_sheath.png");
    private static final RenderType HOLDER_EMISSIVE_RENDER_TYPE = RenderType.entityTranslucent(TEXTURE);

    private boolean holderEmissivePass;

    public ManaForceBladeSheathRenderer() {
        super(new ManaForceBladeSheathModel());
    }

    @Override
    public void postRender(PoseStack poseStack, ManaForceBladeSheathItem animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                           float partialTick, int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, alpha);

        if (isReRender) {
            return;
        }

        var brightness = resolveHolderEmissiveBrightness(partialTick);
        this.holderEmissivePass = true;
        try {
            // holder_emissive は環境光に潰されない魔力部品として、専用フルブライトパスだけで描く。
            this.reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    HOLDER_EMISSIVE_RENDER_TYPE,
                    bufferSource.getBuffer(HOLDER_EMISSIVE_RENDER_TYPE),
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    brightness,
                    brightness,
                    brightness,
                    alpha
            );
        } finally {
            this.holderEmissivePass = false;
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ManaForceBladeSheathItem animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        var holderEmissiveBone = isBoneOrChildOf(bone, HOLDER_EMISSIVE_BONE);

        if (!this.holderEmissivePass && holderEmissiveBone) {
            return;
        }

        if (this.holderEmissivePass) {
            renderHolderEmissivePassBone(
                    poseStack, animatable, bone, holderEmissiveBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha
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
        this.holderEmissivePass = false;
    }

    private void renderHolderEmissivePassBone(PoseStack poseStack, ManaForceBladeSheathItem animatable, GeoBone bone,
                                              boolean targetBone, RenderType renderType, MultiBufferSource bufferSource,
                                              VertexConsumer buffer, boolean isReRender, float partialTick,
                                              int packedLight, int packedOverlay,
                                              float red, float green, float blue, float alpha) {
        if (targetBone) {
            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        renderChildBonesOnly(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, alpha
        );
    }

    private void renderChildBonesOnly(PoseStack poseStack, ManaForceBladeSheathItem animatable, GeoBone bone,
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

    private static float resolveHolderEmissiveBrightness(float partialTick) {
        var level = Minecraft.getInstance().level;
        float tick = level == null ? partialTick : level.getGameTime() + partialTick;
        return 0.95F + Mth.sin(tick / 20.0F * Mth.TWO_PI) * 0.05F;
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
