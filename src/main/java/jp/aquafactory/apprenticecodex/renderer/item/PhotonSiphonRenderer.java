package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphon;
import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphonClientRenderState;
import jp.aquafactory.apprenticecodex.model.PhotonSiphonModel;
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
import software.bernie.geckolib.util.RenderUtil;

public class PhotonSiphonRenderer extends GeoItemRenderer<PhotonSiphon> {
    private static final String HANDLE_GLOW_BONE = "handle_glow";
    private static final String SIPHON_RING_1_BONE = "siphon_ring_1";
    private static final String SIPHON_RING_2_BONE = "siphon_ring_2";
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/photon_siphon.png");
    private static final RenderType HANDLE_GLOW_RENDER_TYPE = RenderType.entityTranslucent(TEXTURE);
    private static final RenderType RING_1_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly("photon_siphon_ring_1_additive", TEXTURE);
    private static final RenderType RING_2_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly("photon_siphon_ring_2_additive", TEXTURE);

    private GlowPass glowPass = GlowPass.NONE;

    public PhotonSiphonRenderer() {
        super(new PhotonSiphonModel());
    }

    @Override
    public void postRender(PoseStack poseStack, PhotonSiphon animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                           VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                           int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay,
                colour);

        if (isReRender) {
            return;
        }

        float handleBrightness = resolveHandleGlowBrightness(partialTick);
        renderGlowPass(model, poseStack, bufferSource, animatable, GlowPass.HANDLE_GLOW, HANDLE_GLOW_RENDER_TYPE,
                partialTick, multiplyRgb(colour, handleBrightness));

        var combatRing = PhotonSiphonClientRenderState.resolveCombatRing(this.currentItemStack, this.renderPerspective, partialTick);
        if (combatRing.visible()) {
            renderGlowPass(model, poseStack, bufferSource, animatable, GlowPass.SIPHON_RING_1, RING_1_RENDER_TYPE,
                    partialTick, rgba(combatRing.red(), combatRing.green(), combatRing.blue(), combatRing.alpha()));
        }

        var manaRing = PhotonSiphonClientRenderState.resolveManaRing(this.currentItemStack, this.renderPerspective, partialTick);
        if (manaRing.visible()) {
            renderGlowPass(model, poseStack, bufferSource, animatable, GlowPass.SIPHON_RING_2, RING_2_RENDER_TYPE,
                    partialTick, rgba(manaRing.red(), manaRing.green(), manaRing.blue(), manaRing.alpha()));
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, PhotonSiphon animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        var handleGlowBone = isBoneOrChildOf(bone, HANDLE_GLOW_BONE);
        var ring1Bone = isBoneOrChildOf(bone, SIPHON_RING_1_BONE);
        var ring2Bone = isBoneOrChildOf(bone, SIPHON_RING_2_BONE);
        var specialBone = handleGlowBone || ring1Bone || ring2Bone;

        if (this.glowPass == GlowPass.NONE && specialBone) {
            renderChildBonesOnly(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.glowPass == GlowPass.HANDLE_GLOW) {
            renderGlowPassBone(
                    poseStack, animatable, bone, handleGlowBone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.glowPass == GlowPass.SIPHON_RING_1) {
            renderGlowPassBone(
                    poseStack, animatable, bone, ring1Bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.glowPass == GlowPass.SIPHON_RING_2) {
            renderGlowPassBone(
                    poseStack, animatable, bone, ring2Bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
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
        this.glowPass = GlowPass.NONE;
    }

    private void renderGlowPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                PhotonSiphon animatable, GlowPass pass, RenderType renderType, float partialTick,
                                int colour) {
        this.glowPass = pass;
        try {
            // 特殊ボーンは glint や通常ライティングから切り離し、意図した発光パスだけを重ねる。
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
                    colour
            );
        } finally {
            this.glowPass = GlowPass.NONE;
        }
    }

    private void renderGlowPassBone(PoseStack poseStack, PhotonSiphon animatable, GeoBone bone, boolean targetBone,
                                    RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                    boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                    int colour) {
        if (targetBone) {
            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        renderChildBonesOnly(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour
        );
    }

    private void renderChildBonesOnly(PoseStack poseStack, PhotonSiphon animatable, GeoBone bone, RenderType renderType,
                                      MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                      float partialTick, int packedLight, int packedOverlay,
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

    private static int multiplyRgb(int colour, float multiplier) {
        int alpha = colour & 0xFF000000;
        int red = Math.round(((colour >> 16) & 0xFF) * multiplier);
        int green = Math.round(((colour >> 8) & 0xFF) * multiplier);
        int blue = Math.round((colour & 0xFF) * multiplier);
        return alpha | (Mth.clamp(red, 0, 255) << 16) | (Mth.clamp(green, 0, 255) << 8) | Mth.clamp(blue, 0, 255);
    }

    private static int rgba(float red, float green, float blue, float alpha) {
        return (Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24)
                | (Mth.clamp(Math.round(red * 255.0F), 0, 255) << 16)
                | (Mth.clamp(Math.round(green * 255.0F), 0, 255) << 8)
                | Mth.clamp(Math.round(blue * 255.0F), 0, 255);
    }

    private static float resolveHandleGlowBrightness(float partialTick) {
        var level = Minecraft.getInstance().level;
        float time = level == null ? partialTick : level.getGameTime() + partialTick;
        return 0.95F + 0.05F * Mth.sin(time * Mth.TWO_PI / 48.0F);
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }

    private enum GlowPass {
        NONE,
        HANDLE_GLOW,
        SIPHON_RING_1,
        SIPHON_RING_2
    }
}
