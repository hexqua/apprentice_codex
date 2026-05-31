package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ZenithStaff;
import jp.aquafactory.apprenticecodex.item.zenithstaff.ZenithStaffClientRenderState;
import jp.aquafactory.apprenticecodex.model.ZenithStaffModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
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

public class ZenithStaffRenderer extends GeoItemRenderer<ZenithStaff> {
    private static final String TAIL_BONE = "tail";
    private static final String STONE_BONE = "stone";
    private static final int TAIL_MIN_BLOCK_LIGHT = 9;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/zenith_staff.png");
    private static final RenderType DEFAULT_RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);
    private static final RenderType STONE_ADDITIVE_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("zenith_staff_stone_additive", TEXTURE);

    private SpecialPass specialPass = SpecialPass.NONE;
    private ZenithStaffClientRenderState.StoneRenderState stoneState = ZenithStaffClientRenderState.StoneRenderState.hidden();

    public ZenithStaffRenderer() {
        super(new ZenithStaffModel());
    }

    @Override
    public RenderType getRenderType(ZenithStaff animatable, ResourceLocation texture, MultiBufferSource bufferSource,
                                    float partialTick) {
        return DEFAULT_RENDER_TYPE;
    }

    @Override
    public void preRender(PoseStack poseStack, ZenithStaff animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                          int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);

        if (!isReRender) {
            this.stoneState = ZenithStaffClientRenderState.resolveStone(
                    this.currentItemStack,
                    this.renderPerspective,
                    partialTick
            );
        }
    }

    @Override
    public void postRender(PoseStack poseStack, ZenithStaff animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                           int packedLight, int packedOverlay, int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);

        if (isReRender || !this.stoneState.visible()) {
            return;
        }

        renderStonePass(
                model,
                poseStack,
                bufferSource,
                animatable,
                partialTick,
                this.stoneState.red(),
                this.stoneState.green(),
                this.stoneState.blue(),
                this.stoneState.alpha()
        );
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ZenithStaff animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  int colour) {
        var tailBone = isBoneOrChildOf(bone, TAIL_BONE);
        var stoneBone = isBoneOrChildOf(bone, STONE_BONE);

        if (this.specialPass == SpecialPass.NONE) {
            if (stoneBone && this.stoneState.visible()) {
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
                    tailBone ? raiseBlockLightFloor(packedLight, TAIL_MIN_BLOCK_LIGHT) : packedLight,
                    packedOverlay,
                    colour
            );
            return;
        }

        if (this.specialPass == SpecialPass.STONE) {
            renderSpecialPassBone(
                    poseStack, animatable, bone, stoneBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, colour
            );
        }
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.specialPass = SpecialPass.NONE;
        this.stoneState = ZenithStaffClientRenderState.StoneRenderState.hidden();
    }

    private void renderStonePass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                 ZenithStaff animatable, float partialTick,
                                 float red, float green, float blue, float alpha) {
        this.specialPass = SpecialPass.STONE;
        try {
            this.reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    STONE_ADDITIVE_RENDER_TYPE,
                    bufferSource.getBuffer(STONE_ADDITIVE_RENDER_TYPE),
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    toColour(red, green, blue, alpha)
            );
        } finally {
            this.specialPass = SpecialPass.NONE;
        }
    }

    private void renderSpecialPassBone(PoseStack poseStack, ZenithStaff animatable, GeoBone bone,
                                       boolean targetBone, RenderType renderType, MultiBufferSource bufferSource,
                                       VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                                       int packedOverlay, int colour) {
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

    private void renderChildBonesOnly(PoseStack poseStack, ZenithStaff animatable, GeoBone bone, RenderType renderType,
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

    private static int raiseBlockLightFloor(int packedLight, int minBlockLight) {
        return LightTexture.pack(Math.max(LightTexture.block(packedLight), minBlockLight), LightTexture.sky(packedLight));
    }

    private static int toColour(float red, float green, float blue, float alpha) {
        return (Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24)
                | (Mth.clamp(Math.round(red * 255.0F), 0, 255) << 16)
                | (Mth.clamp(Math.round(green * 255.0F), 0, 255) << 8)
                | Mth.clamp(Math.round(blue * 255.0F), 0, 255);
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
        STONE
    }
}
