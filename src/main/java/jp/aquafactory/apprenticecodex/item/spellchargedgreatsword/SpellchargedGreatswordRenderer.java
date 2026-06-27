package jp.aquafactory.apprenticecodex.item.spellchargedgreatsword;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
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
import software.bernie.geckolib.util.RenderUtils;

public final class SpellchargedGreatswordRenderer extends GeoItemRenderer<SpellchargedGreatsword> {
    private static final String STAR_BONE = "star";
    private static final String CORE2_BONE = "core2";
    private static final String AURA_BONE = "aura";
    private static final int STAR_MIN_BLOCK_LIGHT = 7;
    private static final float CORE_PULSE_PERIOD_TICKS = 20.0F;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/spellcharged_greatsword.png");
    private static final RenderType DEFAULT_RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);
    private static final RenderType CORE2_RENDER_TYPE =
            ApprenticeRenderTypes.entityTranslucentNoCull("spellcharged_greatsword_core2_emissive", TEXTURE);

    private SpecialPass specialPass = SpecialPass.NONE;

    public SpellchargedGreatswordRenderer() {
        super(new SpellchargedGreatswordModel());
    }

    @Override
    public RenderType getRenderType(SpellchargedGreatsword animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return DEFAULT_RENDER_TYPE;
    }

    @Override
    public void postRender(PoseStack poseStack, SpellchargedGreatsword animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                           float partialTick, int packedLight, int packedOverlay, float red, float green,
                           float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);

        if (isReRender) {
            return;
        }

        float brightness = resolveCore2Brightness(partialTick);
        renderCore2Pass(model, poseStack, bufferSource, animatable, partialTick,
                red * brightness, green * brightness, blue * brightness, alpha);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, SpellchargedGreatsword animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        if (isBoneOrChildOf(bone, AURA_BONE)) {
            return;
        }

        boolean core2Bone = isBoneOrChildOf(bone, CORE2_BONE);
        if (this.specialPass == SpecialPass.NONE) {
            if (core2Bone) {
                return;
            }

            boolean starBone = isBoneOrChildOf(bone, STAR_BONE);
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

        renderSpecialPassBone(
                poseStack, animatable, bone, core2Bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.specialPass = SpecialPass.NONE;
    }

    private void renderCore2Pass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                 SpellchargedGreatsword animatable, float partialTick,
                                 float red, float green, float blue, float alpha) {
        this.specialPass = SpecialPass.CORE2;
        try {
            this.reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    CORE2_RENDER_TYPE,
                    bufferSource.getBuffer(CORE2_RENDER_TYPE),
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

    private void renderSpecialPassBone(PoseStack poseStack, SpellchargedGreatsword animatable, GeoBone bone,
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

    private void renderChildBonesOnly(PoseStack poseStack, SpellchargedGreatsword animatable, GeoBone bone,
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

    private static float resolveCore2Brightness(float partialTick) {
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
        CORE2
    }
}
