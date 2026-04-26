package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ManaForceBlade;
import jp.aquafactory.apprenticecodex.model.ManaForceBladeModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
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

public class ManaForceBladeRenderer extends GeoItemRenderer<ManaForceBlade> {
    private static final String ORB_BONE = "orb";
    private static final String RUNE_BONE = "rune";
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/mana_force_blade.png");
    private static final RenderType ORB_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("mana_force_blade_orb_additive", TEXTURE);
    private static final RenderType RUNE_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("mana_force_blade_rune_additive", TEXTURE);

    private GlowPass glowPass = GlowPass.NONE;

    public ManaForceBladeRenderer() {
        super(new ManaForceBladeModel());
    }

    @Override
    public void postRender(PoseStack poseStack, ManaForceBlade animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                           VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);

        if (isReRender) {
            return;
        }

        renderGlowPass(model, poseStack, bufferSource, animatable, GlowPass.ORB, ORB_RENDER_TYPE, partialTick,
                red, green, blue, alpha);

        var school = MagicTools.getImbuedSpellSchool(this.currentItemStack);
        if (school == null) {
            return;
        }

        var tint = MagicTools.resolveSchoolTintColor(school);
        renderGlowPass(model, poseStack, bufferSource, animatable, GlowPass.RUNE, RUNE_RENDER_TYPE, partialTick,
                ((tint >> 16) & 0xFF) / 255.0F,
                ((tint >> 8) & 0xFF) / 255.0F,
                (tint & 0xFF) / 255.0F,
                alpha);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ManaForceBlade animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        var orbBone = isBoneOrChildOf(bone, ORB_BONE);
        var runeBone = isBoneOrChildOf(bone, RUNE_BONE);

        if (this.glowPass == GlowPass.NONE && (orbBone || runeBone)) {
            return;
        }

        if (this.glowPass == GlowPass.ORB) {
            renderGlowPassBone(
                    poseStack, animatable, bone, orbBone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        if (this.glowPass == GlowPass.RUNE) {
            renderGlowPassBone(
                    poseStack, animatable, bone, runeBone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.glowPass = GlowPass.NONE;
    }

    private void renderGlowPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                ManaForceBlade animatable, GlowPass pass, RenderType renderType, float partialTick,
                                float red, float green, float blue, float alpha) {
        this.glowPass = pass;
        try {
            // orb/rune は通常パスと glint から切り離し、専用の加算発光パスだけで描画する。
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
                    red,
                    green,
                    blue,
                    alpha
            );
        } finally {
            this.glowPass = GlowPass.NONE;
        }
    }

    private void renderGlowPassBone(PoseStack poseStack, ManaForceBlade animatable, GeoBone bone, boolean targetBone,
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

    private void renderChildBonesOnly(PoseStack poseStack, ManaForceBlade animatable, GeoBone bone, RenderType renderType,
                                      MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                      float partialTick, int packedLight, int packedOverlay,
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
        ORB,
        RUNE
    }
}
