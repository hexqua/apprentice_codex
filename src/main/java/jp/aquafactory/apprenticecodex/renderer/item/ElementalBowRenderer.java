package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowClientRenderState;
import jp.aquafactory.apprenticecodex.model.ElementalBowModel;
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
                          float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);

        if (!isReRender) {
            this.orbState = ElementalBowClientRenderState.resolveOrbState(this.currentItemStack, this.renderPerspective, partialTick);
        }
    }

    @Override
    public void postRender(PoseStack poseStack, ElementalBow animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                           VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);

        if (isReRender || !this.orbState.visible()) {
            return;
        }

        renderOrbPass(
                poseStack,
                animatable,
                model,
                bufferSource,
                partialTick,
                this.orbState.red(),
                this.orbState.green(),
                this.orbState.blue(),
                this.orbState.alpha(),
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
                    1.0F,
                    0.0F,
                    0.0F,
                    this.orbState.warningAlpha() * WARNING_RIM_ALPHA_MULTIPLIER,
                    this.orbState.warningScale()
            );
            renderOrbPass(
                    poseStack,
                    animatable,
                    model,
                    bufferSource,
                    partialTick,
                    1.0F,
                    0.0F,
                    0.0F,
                    this.orbState.warningAlpha() * WARNING_CORE_ALPHA_MULTIPLIER,
                    1.0F + this.orbState.warningPulse() * WARNING_CORE_EXTRA_SCALE
            );
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ElementalBow animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (isBoneOrChildOf(bone, ORB_FOCUS_BONE)) {
            if (!this.orbGlowPassActive) {
                return;
            }

            renderScaledOrbRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
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

    private void renderOrbPass(PoseStack poseStack, ElementalBow animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                               float partialTick, float red, float green, float blue, float alpha, float scale) {
        if (alpha <= 0.0F) {
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
                    bufferSource.getBuffer(ORB_RENDER_TYPE),
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    red,
                    green,
                    blue,
                    alpha
            );
        } finally {
            this.orbGlowPassActive = false;
            this.orbGlowPassScale = 1.0F;
        }
    }

    private void renderScaledOrbRecursively(PoseStack poseStack, ElementalBow animatable, GeoBone bone, RenderType renderType,
                                            MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                            int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();

        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            bone.setModelSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.itemRenderTranslations));
        }

        RenderUtils.prepMatrixForBone(poseStack, bone);

        if (this.orbGlowPassScale != 1.0F) {
            RenderUtils.translateToPivotPoint(poseStack, bone);
            poseStack.scale(this.orbGlowPassScale, this.orbGlowPassScale, this.orbGlowPassScale);
            RenderUtils.translateAwayFromPivotPoint(poseStack, bone);
        }

        renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
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

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.orbState = new ElementalBowClientRenderState.OrbRenderState(false, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F);
        this.orbGlowPassActive = false;
        this.orbGlowPassScale = 1.0F;
    }
}
