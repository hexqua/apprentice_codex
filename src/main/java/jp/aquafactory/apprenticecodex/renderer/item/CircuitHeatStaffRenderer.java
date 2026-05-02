package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.CircuitHeatStaff;
import jp.aquafactory.apprenticecodex.item.CircuitHeatStaffClientRenderState;
import jp.aquafactory.apprenticecodex.model.CircuitHeatStaffModel;
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
import software.bernie.geckolib.util.RenderUtil;

public class CircuitHeatStaffRenderer extends GeoItemRenderer<CircuitHeatStaff> {
    private static final String CIRCUIT_BONE = "circuit";
    private static final String CORE_BONE = "core";
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/circuit_heat_staff.png");
    private static final RenderType CIRCUIT_GLOW_RENDER_TYPE =
            ApprenticeRenderTypes.entityTranslucentNoCull("circuit_heat_staff_circuit_emissive", TEXTURE);
    private static final RenderType CORE_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("circuit_heat_staff_core_additive", TEXTURE);

    private GlowPass glowPass = GlowPass.NONE;

    public CircuitHeatStaffRenderer() {
        super(new CircuitHeatStaffModel());
    }

    @Override
    public void postRender(PoseStack poseStack, CircuitHeatStaff animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                           VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                           int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay,
                colour);

        if (isReRender) {
            return;
        }

        var circuitState = CircuitHeatStaffClientRenderState.resolveCircuit(this.currentItemStack, this.renderPerspective, partialTick);
        if (circuitState.glow() && circuitState.alpha() > 0.0F) {
            renderGlowPass(model, poseStack, bufferSource, animatable, GlowPass.CIRCUIT, CIRCUIT_GLOW_RENDER_TYPE, partialTick,
                    toArgb(circuitState.red(), circuitState.green(), circuitState.blue(), circuitState.alpha()));
        }

        var coreState = CircuitHeatStaffClientRenderState.resolveCore(this.currentItemStack, this.renderPerspective, partialTick);
        renderGlowPass(model, poseStack, bufferSource, animatable, GlowPass.CORE, CORE_RENDER_TYPE, partialTick,
                toArgb(coreState.red(), coreState.green(), coreState.blue(), coreState.alpha()));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, CircuitHeatStaff animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        var circuitBone = isBoneOrChildOf(bone, CIRCUIT_BONE);
        var coreBone = isBoneOrChildOf(bone, CORE_BONE);

        if (this.glowPass == GlowPass.NONE && coreBone) {
            renderChildBonesOnly(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.glowPass == GlowPass.CIRCUIT) {
            renderGlowPassBone(
                    poseStack, animatable, bone, circuitBone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.glowPass == GlowPass.CORE) {
            renderGlowPassBone(
                    poseStack, animatable, bone, coreBone, renderType, bufferSource, buffer, isReRender, partialTick,
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
                                CircuitHeatStaff animatable, GlowPass pass, RenderType renderType, float partialTick,
                                int colour) {
        this.glowPass = pass;
        try {
            // circuit/core の追加パスは glint から切り離し、発光表現だけを安定して重ねる。
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

    private void renderGlowPassBone(PoseStack poseStack, CircuitHeatStaff animatable, GeoBone bone, boolean targetBone,
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

    private void renderChildBonesOnly(PoseStack poseStack, CircuitHeatStaff animatable, GeoBone bone, RenderType renderType,
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

    private static int toArgb(float red, float green, float blue, float alpha) {
        return (clampChannel(alpha) << 24)
                | (clampChannel(red) << 16)
                | (clampChannel(green) << 8)
                | clampChannel(blue);
    }

    private static int clampChannel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
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
        CIRCUIT,
        CORE
    }
}
