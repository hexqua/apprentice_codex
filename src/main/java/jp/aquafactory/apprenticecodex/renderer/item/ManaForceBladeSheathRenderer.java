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
import software.bernie.geckolib.util.RenderUtil;

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
                           int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);

        if (isReRender) {
            return;
        }

        var emissiveColour = resolveHolderEmissiveColour(partialTick, colour);
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
                    emissiveColour
            );
        } finally {
            this.holderEmissivePass = false;
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ManaForceBladeSheathItem animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  int colour) {
        var holderEmissiveBone = isBoneOrChildOf(bone, HOLDER_EMISSIVE_BONE);

        if (!this.holderEmissivePass && holderEmissiveBone) {
            return;
        }

        if (this.holderEmissivePass) {
            renderHolderEmissivePassBone(
                    poseStack, animatable, bone, holderEmissiveBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, colour
            );
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour
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
                                              int colour) {
        if (targetBone) {
            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        renderChildBonesOnly(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour
        );
    }

    private void renderChildBonesOnly(PoseStack poseStack, ManaForceBladeSheathItem animatable, GeoBone bone,
                                      RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                      boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                      int colour) {
        poseStack.pushPose();

        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            bone.setModelSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, this.itemRenderTranslations));
        }

        RenderUtil.prepMatrixForBone(poseStack, bone);
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
                colour
        );
        poseStack.popPose();
    }

    private static int resolveHolderEmissiveColour(float partialTick, int colour) {
        var level = Minecraft.getInstance().level;
        float tick = level == null ? partialTick : level.getGameTime() + partialTick;
        var brightness = Mth.clamp(Math.round((0.95F + Mth.sin(tick / 20.0F * Mth.TWO_PI) * 0.05F) * 255.0F), 0, 255);
        return (colour & 0xFF000000) | (brightness << 16) | (brightness << 8) | brightness;
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
