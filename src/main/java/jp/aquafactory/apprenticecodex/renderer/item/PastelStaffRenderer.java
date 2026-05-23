package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.PastelStaff;
import jp.aquafactory.apprenticecodex.model.PastelStaffModel;
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

public class PastelStaffRenderer extends GeoItemRenderer<PastelStaff> {
    private static final String STONE_TINT_BONE = "stone_tint";
    private static final String STAR_BONE = "star";
    private static final String ORB_PROJECT_BONE = "orb_project";
    private static final int STAR_MIN_BLOCK_LIGHT = 7;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/pastel_staff.png");
    private static final RenderType DEFAULT_RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);
    private static final RenderType ORB_PROJECT_RENDER_TYPE =
            ApprenticeRenderTypes.entityTranslucentNoCull("pastel_staff_orb_project_emissive", TEXTURE);

    private SpecialPass specialPass = SpecialPass.NONE;

    private float stoneRed = 1.0f;
    private float stoneGreen = 1.0f;
    private float stoneBlue = 1.0f;

    public PastelStaffRenderer() {
        super(new PastelStaffModel());
    }

    @Override
    public RenderType getRenderType(PastelStaff animatable, ResourceLocation texture, MultiBufferSource bufferSource,
                                    float partialTick) {
        return DEFAULT_RENDER_TYPE;
    }

    @Override
    public void preRender(PoseStack poseStack, PastelStaff animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        int stoneColor = animatable.getStoneTintColor(getCurrentItemStack());
        this.stoneRed = ((stoneColor >> 16) & 0xFF) / 255.0f;
        this.stoneGreen = ((stoneColor >> 8) & 0xFF) / 255.0f;
        this.stoneBlue = (stoneColor & 0xFF) / 255.0f;
    }

    @Override
    public void postRender(PoseStack poseStack, PastelStaff animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                           int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, alpha);

        if (isReRender) {
            return;
        }

        renderSpecialPass(model, poseStack, bufferSource, animatable, SpecialPass.ORB_PROJECT,
                ORB_PROJECT_RENDER_TYPE, partialTick, LightTexture.FULL_BRIGHT, red, green, blue, alpha);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, PastelStaff animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        var starBone = isBoneOrChildOf(bone, STAR_BONE);
        var orbProjectBone = isBoneOrChildOf(bone, ORB_PROJECT_BONE);

        if (this.specialPass == SpecialPass.NONE) {
            if (orbProjectBone) {
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

        if (this.specialPass == SpecialPass.ORB_PROJECT) {
            renderSpecialPassBone(
                    poseStack, animatable, bone, orbProjectBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha
            );
        }
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight,
                                  int packedOverlay, int colour) {
        if (STONE_TINT_BONE.equals(bone.getName())) {
            var alpha = (colour >>> 24) & 0xFF;
            var red = (colour >>> 16) & 0xFF;
            var green = (colour >>> 8) & 0xFF;
            var blue = colour & 0xFF;
            red = Math.round(red * this.stoneRed);
            green = Math.round(green * this.stoneGreen);
            blue = Math.round(blue * this.stoneBlue);
            colour = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }

        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.specialPass = SpecialPass.NONE;
        this.stoneRed = 1.0f;
        this.stoneGreen = 1.0f;
        this.stoneBlue = 1.0f;
    }

    private void renderSpecialPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                   PastelStaff animatable, SpecialPass pass, RenderType renderType,
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

    private void renderSpecialPassBone(PoseStack poseStack, PastelStaff animatable, GeoBone bone,
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

    private void renderChildBonesOnly(PoseStack poseStack, PastelStaff animatable, GeoBone bone,
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
        ORB_PROJECT
    }
}
