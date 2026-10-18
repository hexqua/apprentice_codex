package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.manaforcereleasedevice.ManaForceReleaseDevice;
import jp.aquafactory.apprenticecodex.model.ManaForceReleaseDeviceModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

public final class ManaForceReleaseDeviceRenderer extends GeoItemRenderer<ManaForceReleaseDevice> {
    private int glowPass;

    public ManaForceReleaseDeviceRenderer() { super(new ManaForceReleaseDeviceModel()); }

    @Override
    public void postRender(PoseStack poseStack, ManaForceReleaseDevice item, BakedGeoModel model,
                           MultiBufferSource source, VertexConsumer buffer, boolean isReRender, float partialTick,
                           int light, int overlay, int colour) {
        super.postRender(poseStack, item, model, source, buffer, isReRender, partialTick, light, overlay, colour);
        if (isReRender) return;
        var texture = getGeoModel().getTextureResource(item);
        var types = new RenderType[]{
                ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly(item.tier().id + "_crystal", texture),
                RenderType.entityTranslucentEmissive(texture)
        };
        var level = Minecraft.getInstance().level;
        float time = level == null ? 0 : level.getGameTime() + partialTick;
        float brightness = 0.8F + 0.2F * Mth.sin(time * Mth.TWO_PI / 40);
        int channel = (int) (255 * brightness);
        int rgb = 0xFF000000 | channel << 16 | channel << 8 | channel;
        try {
            for (int i = 0; i < types.length; i++) {
                glowPass = i + 1;
                reRender(model, poseStack, source, item, types[i], source.getBuffer(types[i]), partialTick,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, rgb);
            }
        } finally {
            glowPass = 0;
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ManaForceReleaseDevice item, GeoBone bone,
                                  RenderType renderType, MultiBufferSource source, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int light, int overlay, int colour) {
        int category = category(bone);
        if ((glowPass == 0 && category == 0) || (glowPass != 0 && category == glowPass)) {
            super.renderRecursively(poseStack, item, bone, renderType, source, buffer, isReRender,
                    partialTick, light, overlay, colour);
        } else {
            renderChildBonesOnly(poseStack, item, bone, renderType, source, buffer, isReRender,
                    partialTick, light, overlay, colour);
        }
    }

    private static int category(GeoBone bone) {
        for (var current = bone; current != null; current = current.getParent()) {
            if (current.getName().equals("glow_crystal")) return 1;
            if (current.getName().startsWith("glow_")) return 2;
        }
        return 0;
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        glowPass = 0;
    }
    private void renderChildBonesOnly(PoseStack poseStack, ManaForceReleaseDevice animatable, GeoBone bone,
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

}
