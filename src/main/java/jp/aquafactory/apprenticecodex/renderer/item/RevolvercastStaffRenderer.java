package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.RevolvercastStaff;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaffClientEffectState;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastStaffClientEffectState;
import jp.aquafactory.apprenticecodex.model.RevolvercastStaffModel;
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

import java.util.Arrays;

public final class RevolvercastStaffRenderer extends GeoItemRenderer<RevolvercastStaff> {
    private static final String STAR_BONE = "star";
    private static final String STAFF_CORE_BONE = "staff_core";
    private static final String ORB_BONE = "orb";
    private static final String CYLINDER_TRANS_PREFIX = "cylinder_trans";
    private static final int CYLINDER_COUNT = 4;

    private SpecialPass specialPass = SpecialPass.NONE;
    private String specialPassBoneName;
    private RevolvercastStaffClientEffectState.ColorRenderState starState =
            RevolvercastStaffClientEffectState.ColorRenderState.hidden();
    private SwingcastStaffClientEffectState.CoreRenderState coreState =
            new SwingcastStaffClientEffectState.CoreRenderState(0.0F, 0.0F, 0.0F, 0.0F);
    private RevolvercastStaffClientEffectState.ColorRenderState orbState =
            RevolvercastStaffClientEffectState.ColorRenderState.hidden();
    private final RevolvercastStaffClientEffectState.ColorRenderState[] cylinderStates =
            new RevolvercastStaffClientEffectState.ColorRenderState[CYLINDER_COUNT];

    public RevolvercastStaffRenderer() {
        super(new RevolvercastStaffModel());
        Arrays.fill(cylinderStates, RevolvercastStaffClientEffectState.ColorRenderState.hidden());
    }

    @Override
    public RenderType getRenderType(RevolvercastStaff animatable, net.minecraft.resources.ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }

    @Override
    public void preRender(PoseStack poseStack, RevolvercastStaff animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                          int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);

        if (isReRender) {
            return;
        }

        var currentStack = this.currentItemStack != null ? this.currentItemStack : ItemStack.EMPTY;
        this.starState = RevolvercastStaffClientEffectState.resolveStar(partialTick);
        this.coreState = SwingcastStaffClientEffectState.resolveCore(currentStack, partialTick);
        this.orbState = RevolvercastStaffClientEffectState.resolveOrb(currentStack, partialTick);
        for (var index = 0; index < cylinderStates.length; ++index) {
            this.cylinderStates[index] = RevolvercastStaffClientEffectState.resolveCylinder(
                    currentStack,
                    index + 1
            );
        }
    }

    @Override
    public void postRender(PoseStack poseStack, RevolvercastStaff animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                           int packedLight, int packedOverlay, int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);

        if (isReRender) {
            return;
        }

        var texture = getTextureLocation(animatable);
        var starRenderType = ApprenticeRenderTypes.entityTranslucentNoCull("revolvercast_staff_star_emissive", texture);
        renderSpecialPass(model, poseStack, bufferSource, animatable, SpecialPass.STAR, STAR_BONE, starRenderType,
                partialTick, starState);

        var coreRenderType = ApprenticeRenderTypes.entityTranslucentNoCull("revolvercast_staff_core_emissive", texture);
        renderSpecialPass(model, poseStack, bufferSource, animatable, SpecialPass.STAFF_CORE, STAFF_CORE_BONE,
                coreRenderType, partialTick, coreState.red(), coreState.green(), coreState.blue(), coreState.alpha());

        if (orbState.visible()) {
            var orbRenderType = ApprenticeRenderTypes.entityAdditiveGlowNoCull(
                    "revolvercast_staff_orb_additive",
                    texture
            );
            renderSpecialPass(model, poseStack, bufferSource, animatable, SpecialPass.ORB, ORB_BONE, orbRenderType,
                    partialTick, orbState);
        }

        var cylinderRenderType = ApprenticeRenderTypes.entityAdditiveGlowNoCull(
                "revolvercast_staff_cylinder_additive",
                texture
        );
        for (var index = 0; index < cylinderStates.length; ++index) {
            var cylinderState = cylinderStates[index];
            if (!cylinderState.visible()) {
                continue;
            }
            renderSpecialPass(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    SpecialPass.CYLINDER,
                    CYLINDER_TRANS_PREFIX + (index + 1),
                    cylinderRenderType,
                    partialTick,
                    cylinderState
            );
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, RevolvercastStaff animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, int colour) {
        if (this.specialPass != SpecialPass.NONE) {
            renderSpecialPassBone(
                    poseStack, animatable, bone, isBoneOrChildOf(bone, this.specialPassBoneName), renderType,
                    bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour
            );
            return;
        }

        if (isBoneOrChildOf(bone, STAR_BONE)
                || isBoneOrChildOf(bone, STAFF_CORE_BONE)
                || (orbState.visible() && isBoneOrChildOf(bone, ORB_BONE))
                || isCylinderTransBone(bone)) {
            renderChildBonesOnly(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
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
        this.specialPass = SpecialPass.NONE;
        this.specialPassBoneName = null;
        this.starState = RevolvercastStaffClientEffectState.ColorRenderState.hidden();
        this.coreState = new SwingcastStaffClientEffectState.CoreRenderState(0.0F, 0.0F, 0.0F, 0.0F);
        this.orbState = RevolvercastStaffClientEffectState.ColorRenderState.hidden();
        Arrays.fill(cylinderStates, RevolvercastStaffClientEffectState.ColorRenderState.hidden());
    }

    private void renderSpecialPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                   RevolvercastStaff animatable, SpecialPass pass, String targetBoneName,
                                   RenderType renderType, float partialTick,
                                   RevolvercastStaffClientEffectState.ColorRenderState state) {
        renderSpecialPass(
                model,
                poseStack,
                bufferSource,
                animatable,
                pass,
                targetBoneName,
                renderType,
                partialTick,
                state.red(),
                state.green(),
                state.blue(),
                state.alpha()
        );
    }

    private void renderSpecialPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                   RevolvercastStaff animatable, SpecialPass pass, String targetBoneName,
                                   RenderType renderType, float partialTick,
                                   float red, float green, float blue, float alpha) {
        this.specialPass = pass;
        this.specialPassBoneName = targetBoneName;
        try {
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
                    toColour(red, green, blue, alpha)
            );
        } finally {
            this.specialPass = SpecialPass.NONE;
            this.specialPassBoneName = null;
        }
    }

    private void renderSpecialPassBone(PoseStack poseStack, RevolvercastStaff animatable, GeoBone bone,
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

    private void renderChildBonesOnly(PoseStack poseStack, RevolvercastStaff animatable, GeoBone bone,
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

    private static int toColour(float red, float green, float blue, float alpha) {
        var safeAlpha = Math.round(clamp01(alpha) * 255.0F);
        var safeRed = Math.round(clamp01(red) * 255.0F);
        var safeGreen = Math.round(clamp01(green) * 255.0F);
        var safeBlue = Math.round(clamp01(blue) * 255.0F);
        return (safeAlpha << 24) | (safeRed << 16) | (safeGreen << 8) | safeBlue;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        if (rootBoneName == null) {
            return false;
        }

        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }

    private static boolean isCylinderTransBone(GeoBone bone) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (current.getName().startsWith(CYLINDER_TRANS_PREFIX)) {
                return true;
            }
        }

        return false;
    }

    private enum SpecialPass {
        NONE,
        STAR,
        STAFF_CORE,
        ORB,
        CYLINDER
    }
}
