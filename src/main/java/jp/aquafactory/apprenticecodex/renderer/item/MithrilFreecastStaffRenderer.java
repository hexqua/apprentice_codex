package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffClientRenderState;
import jp.aquafactory.apprenticecodex.model.MithrilFreecastStaffModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

public class MithrilFreecastStaffRenderer extends GeoItemRenderer<MithrilFreecastStaff> {
    private static final String HANDLE_STAR_BONE = "handle_star";
    private static final String STAFF_CORE_BONE = "staff_core";
    private static final String ORB_BONE = "orb";
    private static final int HANDLE_STAR_MIN_BLOCK_LIGHT = 7;

    private SpecialPass specialPass = SpecialPass.NONE;

    public MithrilFreecastStaffRenderer() {
        super(new MithrilFreecastStaffModel());
    }

    @Override
    public RenderType getRenderType(MithrilFreecastStaff animatable, net.minecraft.resources.ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }

    @Override
    public void postRender(PoseStack poseStack, MithrilFreecastStaff animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                           int packedLight, int packedOverlay, int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);

        if (isReRender) {
            return;
        }

        var currentStack = this.currentItemStack != null ? this.currentItemStack : ItemStack.EMPTY;
        var coreState = MithrilFreecastStaffClientRenderState.resolveCore(currentStack, partialTick);
        var coreRenderType = ApprenticeRenderTypes.entityTranslucentNoCull(
                "mithril_freecast_staff_core_translucent",
                getTextureLocation(animatable)
        );
        renderSpecialPass(model, poseStack, bufferSource, animatable, SpecialPass.STAFF_CORE, coreRenderType,
                partialTick, LightTexture.FULL_BRIGHT,
                toColour(coreState.red(), coreState.green(), coreState.blue(), coreState.alpha()));

        var orbState = MithrilFreecastStaffClientRenderState.resolveOrb(partialTick);
        var orbRenderType = ApprenticeRenderTypes.entityTranslucentNoCull(
                "mithril_freecast_staff_orb_translucent",
                getTextureLocation(animatable)
        );
        renderSpecialPass(model, poseStack, bufferSource, animatable, SpecialPass.ORB, orbRenderType,
                partialTick, packedLight,
                toColour(orbState.red(), orbState.green(), orbState.blue(), orbState.alpha()));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, MithrilFreecastStaff animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  int colour) {
        var handleStarBone = isBoneOrChildOf(bone, HANDLE_STAR_BONE);
        var staffCoreBone = isBoneOrChildOf(bone, STAFF_CORE_BONE);
        var orbBone = isBoneOrChildOf(bone, ORB_BONE);

        if (this.specialPass == SpecialPass.NONE) {
            if (staffCoreBone || orbBone) {
                return;
            }

            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    handleStarBone ? raiseBlockLightFloor(packedLight, HANDLE_STAR_MIN_BLOCK_LIGHT) : packedLight,
                    packedOverlay, colour
            );
            return;
        }

        if (this.specialPass == SpecialPass.STAFF_CORE) {
            renderSpecialPassBone(
                    poseStack, animatable, bone, staffCoreBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.specialPass == SpecialPass.ORB) {
            renderSpecialPassBone(
                    poseStack, animatable, bone, orbBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, colour
            );
        }
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.specialPass = SpecialPass.NONE;
    }

    private void renderSpecialPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                   MithrilFreecastStaff animatable, SpecialPass pass, RenderType renderType,
                                   float partialTick, int packedLight, int colour) {
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
                    colour
            );
        } finally {
            this.specialPass = SpecialPass.NONE;
        }
    }

    private void renderSpecialPassBone(PoseStack poseStack, MithrilFreecastStaff animatable, GeoBone bone,
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

    private void renderChildBonesOnly(PoseStack poseStack, MithrilFreecastStaff animatable, GeoBone bone,
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

    private static int raiseBlockLightFloor(int packedLight, int minBlockLight) {
        return LightTexture.pack(Math.max(LightTexture.block(packedLight), minBlockLight), LightTexture.sky(packedLight));
    }

    private static int toColour(float red, float green, float blue, float alpha) {
        var a = Math.round(clamp01(alpha) * 255.0F);
        var r = Math.round(clamp01(red) * 255.0F);
        var g = Math.round(clamp01(green) * 255.0F);
        var b = Math.round(clamp01(blue) * 255.0F);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
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
        STAFF_CORE,
        ORB
    }
}
