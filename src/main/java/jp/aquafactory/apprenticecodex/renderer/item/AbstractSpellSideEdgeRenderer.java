package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellsideedge.AbstractSpellSideEdgeItem;
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
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtils;

abstract class AbstractSpellSideEdgeRenderer<T extends AbstractSpellSideEdgeItem> extends GeoItemRenderer<T> {
    private static final String BLADE_BONE = "blade";
    private static final String CORE_BONE = "core";
    private static final int BLADE_MIN_BLOCK_LIGHT = 7;
    private static final float CORE_PULSE_PERIOD_TICKS = 100.0F;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/spell_side_edge.png");
    private static final RenderType DEFAULT_RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);
    private static final RenderType CORE_RENDER_TYPE =
            ApprenticeRenderTypes.entityTranslucentNoCull("spell_side_edge_core_emissive", TEXTURE);

    private SpecialPass specialPass = SpecialPass.NONE;

    protected AbstractSpellSideEdgeRenderer(GeoModel<T> model) {
        super(model);
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource bufferSource,
                                    float partialTick) {
        return DEFAULT_RENDER_TYPE;
    }

    @Override
    public void postRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                           VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                           int packedOverlay, float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, alpha);

        if (isReRender) {
            return;
        }

        float brightness = resolveCoreBrightness(partialTick);
        renderCorePass(model, poseStack, bufferSource, animatable, partialTick,
                brightness, brightness, brightness, alpha);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, float red, float green,
                                  float blue, float alpha) {
        boolean bladeBone = isBoneOrChildOf(bone, BLADE_BONE);
        boolean coreBone = isBoneOrChildOf(bone, CORE_BONE);

        if (this.specialPass == SpecialPass.NONE) {
            if (coreBone) {
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
                    bladeBone ? raiseBlockLightFloor(packedLight, BLADE_MIN_BLOCK_LIGHT) : packedLight,
                    packedOverlay,
                    red,
                    green,
                    blue,
                    alpha
            );
            return;
        }

        if (this.specialPass == SpecialPass.CORE) {
            renderSpecialPassBone(
                    poseStack, animatable, bone, coreBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha
            );
        }
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.specialPass = SpecialPass.NONE;
    }

    private void renderCorePass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                T animatable, float partialTick, float red, float green, float blue, float alpha) {
        this.specialPass = SpecialPass.CORE;
        try {
            this.reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    CORE_RENDER_TYPE,
                    bufferSource.getBuffer(CORE_RENDER_TYPE),
                    partialTick,
                    LightTexture.FULL_BRIGHT,
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

    private void renderSpecialPassBone(PoseStack poseStack, T animatable, GeoBone bone, boolean targetBone,
                                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                       boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                       float red, float green, float blue, float alpha) {
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

    private void renderChildBonesOnly(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                                      MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                      float partialTick, int packedLight, int packedOverlay, float red, float green,
                                      float blue, float alpha) {
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

    private static float resolveCoreBrightness(float partialTick) {
        float time = resolveRenderTime(partialTick);
        return 0.95F + 0.05F * Mth.sin(time * Mth.TWO_PI / CORE_PULSE_PERIOD_TICKS);
    }

    private static float resolveRenderTime(float partialTick) {
        var level = Minecraft.getInstance().level;
        return level == null ? partialTick : level.getGameTime() + partialTick;
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
        CORE
    }
}
