package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.MulticastEchoStaff;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffClientRenderState;
import jp.aquafactory.apprenticecodex.model.MulticastEchoStaffModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class MulticastEchoStaffRenderer extends GeoItemRenderer<MulticastEchoStaff> {
    private static final String STAR_BONE = "star";
    private static final String HEAD_CORE_BONE = "head_core";
    private static final String SHARD_BONE = "shard";
    private static final int STAR_MIN_BLOCK_LIGHT = 7;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/multicast_echo_staff.png");
    private static final RenderType DEFAULT_RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);
    private static final RenderType HEAD_CORE_RENDER_TYPE =
            ApprenticeRenderTypes.entityTranslucentNoCull("multicast_echo_staff_head_core_emissive", TEXTURE);
    private static final RenderType SHARD_RENDER_TYPE =
            ApprenticeRenderTypes.entityTranslucentNoCull("multicast_echo_staff_shard_translucent", TEXTURE);

    private SpecialPass specialPass = SpecialPass.NONE;

    public MulticastEchoStaffRenderer() {
        super(new MulticastEchoStaffModel());
    }

    @Override
    public RenderType getRenderType(MulticastEchoStaff animatable, ResourceLocation texture, MultiBufferSource bufferSource,
                                    float partialTick) {
        return DEFAULT_RENDER_TYPE;
    }

    @Override
    public void postRender(PoseStack poseStack, MulticastEchoStaff animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                           int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, alpha);

        if (isReRender) {
            return;
        }

        renderSpecialPass(model, poseStack, bufferSource, animatable, SpecialPass.HEAD_CORE, HEAD_CORE_RENDER_TYPE,
                partialTick, LightTexture.FULL_BRIGHT, 1.0F, 1.0F, 1.0F, alpha);
        renderSpecialPass(model, poseStack, bufferSource, animatable, SpecialPass.SHARD, SHARD_RENDER_TYPE,
                partialTick, packedLight, red, green, blue,
                alpha * MulticastEchoStaffClientRenderState.resolveShardAlpha(partialTick));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, MulticastEchoStaff animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        var starBone = isBoneOrChildOf(bone, STAR_BONE);
        var headCoreBone = isBoneOrChildOf(bone, HEAD_CORE_BONE);
        var shardBone = isBoneOrChildOf(bone, SHARD_BONE);

        if (this.specialPass == SpecialPass.NONE) {
            if (headCoreBone || shardBone) {
                return;
            }

            super.renderRecursively(
                    poseStack,
                    animatable,
                    bone,
                    renderType,
                    bufferSource,
                    buffer,
                    isReRender,
                    partialTick,
                    starBone ? raiseBlockLightFloor(packedLight, STAR_MIN_BLOCK_LIGHT) : packedLight,
                    packedOverlay,
                    red,
                    green,
                    blue,
                    alpha
            );
            return;
        }

        if (this.specialPass == SpecialPass.HEAD_CORE) {
            renderSpecialPassBone(
                    poseStack, animatable, bone, headCoreBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        if (this.specialPass == SpecialPass.SHARD) {
            renderSpecialPassBone(
                    poseStack, animatable, bone, shardBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha
            );
        }
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.specialPass = SpecialPass.NONE;
    }

    private void renderSpecialPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                   MulticastEchoStaff animatable, SpecialPass pass, RenderType renderType,
                                   float partialTick, int packedLight, float red, float green, float blue, float alpha) {
        this.specialPass = pass;
        try {
            this.reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    renderType,
                    bufferSource.getBuffer(renderType),
                    partialTick,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    red,
                    green,
                    blue,
                    alpha
            );
        } finally {
            this.specialPass = SpecialPass.NONE;
        }
    }

    private void renderSpecialPassBone(PoseStack poseStack, MulticastEchoStaff animatable, GeoBone bone,
                                       boolean targetBone, RenderType renderType, MultiBufferSource bufferSource,
                                       VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                                       int packedOverlay, float red, float green, float blue, float alpha) {
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

    private void renderChildBonesOnly(PoseStack poseStack, MulticastEchoStaff animatable, GeoBone bone,
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

    private static int raiseBlockLightFloor(int packedLight, int minBlockLight) {
        return LightTexture.pack(Math.max(LightTexture.block(packedLight), minBlockLight), LightTexture.sky(packedLight));
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }

    private enum SpecialPass {
        NONE,
        HEAD_CORE,
        SHARD
    }
}
