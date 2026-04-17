package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ElementalBow;
import jp.aquafactory.apprenticecodex.item.ElementalBowClientRenderState;
import jp.aquafactory.apprenticecodex.model.ElementalBowModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

public class ElementalBowRenderer extends GeoItemRenderer<ElementalBow> {
    private static final String ORB_FOCUS_BONE = "orb_focus";
    private static final ResourceLocation ORB_MASK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/elemental_bow_orb_mask.png");
    private static final RenderType ORB_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("elemental_bow_orb_focus_additive", ORB_MASK_TEXTURE);
    private static final float WARNING_RIM_ALPHA_MULTIPLIER = 0.95F;
    private static final float WARNING_CORE_ALPHA_MULTIPLIER = 0.42F;
    private static final float WARNING_CORE_EXTRA_SCALE = 0.02F;

    private ElementalBowClientRenderState.OrbRenderState orbState =
            new ElementalBowClientRenderState.OrbRenderState(false, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F);
    private boolean orbGlowPassActive = false;
    private float orbGlowPassScale = 1.0F;

    public ElementalBowRenderer() {
        super(new ElementalBowModel());
    }

    @Override
    public void preRender(PoseStack poseStack, ElementalBow animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        if (!isReRender) {
            this.orbState = ElementalBowClientRenderState.resolveOrbState(this.currentItemStack, this.renderPerspective, partialTick);
        }
    }

    @Override
    public void postRender(PoseStack poseStack, ElementalBow animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                           VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                           int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        if (isReRender || !this.orbState.visible()) {
            return;
        }

        renderOrbPass(
                poseStack,
                animatable,
                model,
                bufferSource,
                partialTick,
                makeColor(this.orbState.red(), this.orbState.green(), this.orbState.blue(), this.orbState.alpha()),
                this.orbState.baseScale()
        );

        if (this.orbState.warningAlpha() > 0.0F) {
            // school 色の中心は残しつつ、赤い外縁の膨張で警告感を優先して見せる。
            renderOrbPass(
                    poseStack,
                    animatable,
                    model,
                    bufferSource,
                    partialTick,
                    makeColor(1.0F, 0.0F, 0.0F, this.orbState.warningAlpha() * WARNING_RIM_ALPHA_MULTIPLIER),
                    this.orbState.warningScale()
            );
            renderOrbPass(
                    poseStack,
                    animatable,
                    model,
                    bufferSource,
                    partialTick,
                    makeColor(1.0F, 0.0F, 0.0F, this.orbState.warningAlpha() * WARNING_CORE_ALPHA_MULTIPLIER),
                    1.0F + this.orbState.warningPulse() * WARNING_CORE_EXTRA_SCALE
            );
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ElementalBow animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        if (isBoneOrChildOf(bone, ORB_FOCUS_BONE)) {
            if (!this.orbGlowPassActive) {
                return;
            }

            renderScaledOrbRecursively(
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
        this.orbState = new ElementalBowClientRenderState.OrbRenderState(false, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F);
        this.orbGlowPassActive = false;
        this.orbGlowPassScale = 1.0F;
    }

    private void renderOrbPass(PoseStack poseStack, ElementalBow animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                               float partialTick, int colour, float scale) {
        if (((colour >>> 24) & 0xFF) <= 0) {
            return;
        }

        this.orbGlowPassActive = true;
        this.orbGlowPassScale = scale;
        try {
            // orb_focus は glint や通常ライティングを通さず、加算発光専用パスでのみ描画する。
            this.reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    ORB_RENDER_TYPE,
                    getFoilAwareBuffer(bufferSource, ORB_RENDER_TYPE),
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    colour
            );
        } finally {
            this.orbGlowPassActive = false;
            this.orbGlowPassScale = 1.0F;
        }
    }

    private void renderScaledOrbRecursively(PoseStack poseStack, ElementalBow animatable, GeoBone bone, RenderType renderType,
                                            MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                            int packedLight, int packedOverlay, int colour) {
        poseStack.pushPose();

        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            bone.setModelSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, this.itemRenderTranslations));
        }

        RenderUtil.prepMatrixForBone(poseStack, bone);

        if (this.orbGlowPassScale != 1.0F) {
            RenderUtil.translateToPivotPoint(poseStack, bone);
            poseStack.scale(this.orbGlowPassScale, this.orbGlowPassScale, this.orbGlowPassScale);
            RenderUtil.translateAwayFromPivotPoint(poseStack, bone);
        }

        renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
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

    private VertexConsumer getFoilAwareBuffer(MultiBufferSource bufferSource, RenderType renderType) {
        return ItemRenderer.getFoilBufferDirect(
                bufferSource,
                renderType,
                this.renderPerspective == ItemDisplayContext.GUI,
                this.currentItemStack != null && this.currentItemStack.hasFoil()
        );
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }

    private static int makeColor(float red, float green, float blue, float alpha) {
        var safeAlpha = Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        var safeRed = Math.round(Mth.clamp(red, 0.0F, 1.0F) * 255.0F);
        var safeGreen = Math.round(Mth.clamp(green, 0.0F, 1.0F) * 255.0F);
        var safeBlue = Math.round(Mth.clamp(blue, 0.0F, 1.0F) * 255.0F);
        return (safeAlpha << 24) | (safeRed << 16) | (safeGreen << 8) | safeBlue;
    }
}
