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
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtils;

public final class SpellchargedGreatswordRenderer extends GeoItemRenderer<SpellchargedGreatsword> {
    private static final String STAR_BONE = "star";
    private static final String CORE2_BONE = "core2";
    private static final String AURA_BONE = "aura";
    private static final String NORMAL_AURA_BONE = "normal_aura";
    private static final String EXTENDED_AURA_BONE = "extended_aura";
    private static final int STAR_MIN_BLOCK_LIGHT = 7;
    private static final float CORE_PULSE_PERIOD_TICKS = 20.0F;
    private static final float NORMAL_AURA_GLINT_INTENSITY_MULTIPLIER = 0.55F;
    private static final float NORMAL_AURA_GLINT_SCROLL_U_PER_TICK = -0.008F;
    private static final float NORMAL_AURA_GLINT_SCROLL_V_PER_TICK = 0.016F;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/spellcharged_greatsword.png");
    private static final ResourceLocation GLINT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/spellcharged_greatsword_glint.png");
    private static final RenderType DEFAULT_RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);
    private static final RenderType CORE2_RENDER_TYPE =
            ApprenticeRenderTypes.entityTranslucentNoCull("spellcharged_greatsword_core2_emissive", TEXTURE);
    private static final RenderType NORMAL_AURA_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly("spellcharged_greatsword_normal_aura", TEXTURE);
    private static final RenderType NORMAL_AURA_GLINT_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly("spellcharged_greatsword_normal_aura_glint", GLINT_TEXTURE);

    private SpecialPass specialPass = SpecialPass.NONE;
    private float normalAuraGlintUOffset = 0.0F;
    private float normalAuraGlintVOffset = 0.0F;

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

        var renderState = resolveChargeRenderState(partialTick);
        float brightness = resolveCore2Brightness(partialTick, renderState.core2MaxBrightness());
        var color = renderState.core2Color();
        renderCore2Pass(model, poseStack, bufferSource, animatable, partialTick,
                red * color.red() * brightness, green * color.green() * brightness,
                blue * color.blue() * brightness, alpha);
        if (renderState.normalAuraIntensity() > 0.0F) {
            renderNormalAuraPass(model, poseStack, bufferSource, animatable, partialTick,
                    red, green, blue, alpha, renderState.normalAuraIntensity());
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, SpellchargedGreatsword animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        if (isBoneOrChildOf(bone, EXTENDED_AURA_BONE)) {
            return;
        }

        boolean core2Bone = isBoneOrChildOf(bone, CORE2_BONE);
        boolean auraBone = isBoneOrChildOf(bone, AURA_BONE);
        boolean normalAuraBone = isBoneOrChildOf(bone, NORMAL_AURA_BONE);
        if (this.specialPass == SpecialPass.NONE) {
            if (core2Bone || auraBone) {
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

        boolean targetBone = switch (this.specialPass) {
            case CORE2 -> core2Bone;
            case NORMAL_AURA, NORMAL_AURA_GLINT -> normalAuraBone;
            case NONE -> false;
        };
        renderSpecialPassBone(
                poseStack, animatable, bone, targetBone, renderType, bufferSource, buffer, isReRender,
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

    private void renderNormalAuraPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                      SpellchargedGreatsword animatable, float partialTick,
                                      float red, float green, float blue, float alpha, float intensity) {
        this.specialPass = SpecialPass.NORMAL_AURA;
        try {
            this.reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    NORMAL_AURA_RENDER_TYPE,
                    bufferSource.getBuffer(NORMAL_AURA_RENDER_TYPE),
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    red * intensity,
                    green * intensity,
                    blue * intensity,
                    alpha
            );
            float glintTime = resolveRenderTime(partialTick);
            this.specialPass = SpecialPass.NORMAL_AURA_GLINT;
            this.normalAuraGlintUOffset = wrapUnit(glintTime * NORMAL_AURA_GLINT_SCROLL_U_PER_TICK);
            this.normalAuraGlintVOffset = wrapUnit(glintTime * NORMAL_AURA_GLINT_SCROLL_V_PER_TICK);
            this.reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    NORMAL_AURA_GLINT_RENDER_TYPE,
                    bufferSource.getBuffer(NORMAL_AURA_GLINT_RENDER_TYPE),
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    red * intensity * NORMAL_AURA_GLINT_INTENSITY_MULTIPLIER,
                    green * intensity * NORMAL_AURA_GLINT_INTENSITY_MULTIPLIER,
                    blue * intensity * NORMAL_AURA_GLINT_INTENSITY_MULTIPLIER,
                    alpha
            );
        } finally {
            this.normalAuraGlintUOffset = 0.0F;
            this.normalAuraGlintVOffset = 0.0F;
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

    @Override
    public void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer,
                                     int packedLight, int packedOverlay, float red, float green, float blue,
                                     float alpha) {
        if (this.specialPass != SpecialPass.NORMAL_AURA_GLINT) {
            super.createVerticesOfQuad(
                    quad, poseState, normal, buffer, packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        for (GeoVertex vertex : quad.vertices()) {
            Vector3f position = vertex.position();
            Vector4f transformedPosition = poseState.transform(
                    new Vector4f(position.x(), position.y(), position.z(), 1.0F)
            );
            buffer.vertex(
                    transformedPosition.x(),
                    transformedPosition.y(),
                    transformedPosition.z(),
                    red,
                    green,
                    blue,
                    alpha,
                    vertex.texU() + this.normalAuraGlintUOffset,
                    vertex.texV() + this.normalAuraGlintVOffset,
                    packedOverlay,
                    packedLight,
                    normal.x(),
                    normal.y(),
                    normal.z()
            );
        }
    }

    private ChargeRenderState resolveChargeRenderState(float partialTick) {
        if (!isHandheldPerspective(this.renderPerspective)) {
            return new ChargeRenderState(1.0F, GlowColor.WHITE, 0.0F);
        }

        var stack = this.currentItemStack != null ? this.currentItemStack : net.minecraft.world.item.ItemStack.EMPTY;
        var chargeProgress = Mth.clamp(
                SpellchargedGreatsword.getEffectiveChargeTicks(stack, resolveRenderTime(partialTick))
                        / SpellchargedGreatsword.MAX_CHARGE_TICKS,
                0.0D,
                1.0D
        );
        var core2MaxBrightness = (float) Mth.lerp(chargeProgress, 0.1D, 1.0D);
        var chargeLevel = SpellchargedGreatsword.getChargeLevel(stack);
        return new ChargeRenderState(
                core2MaxBrightness,
                resolveCore2Color(chargeLevel),
                resolveNormalAuraIntensity(chargeLevel)
        );
    }

    private static float resolveCore2Brightness(float partialTick, float maxBrightness) {
        float minBrightness = Math.max(0.0F, maxBrightness - 0.1F);
        float time = resolveRenderTime(partialTick);
        float progress = (Mth.sin(time * Mth.TWO_PI / CORE_PULSE_PERIOD_TICKS) + 1.0F) * 0.5F;
        return Mth.lerp(progress, minBrightness, maxBrightness);
    }

    private static float resolveRenderTime(float partialTick) {
        var level = Minecraft.getInstance().level;
        return level == null ? partialTick : level.getGameTime() + partialTick;
    }

    private static int raiseBlockLightFloor(int packedLight, int minBlockLight) {
        return LightTexture.pack(Math.max(LightTexture.block(packedLight), minBlockLight), LightTexture.sky(packedLight));
    }

    private static float wrapUnit(float value) {
        return value - Mth.floor(value);
    }

    private static GlowColor resolveCore2Color(int chargeLevel) {
        return chargeLevel > 0 ? GlowColor.fromRgb(0xFFFF88) : GlowColor.WHITE;
    }

    private static float resolveNormalAuraIntensity(int chargeLevel) {
        return switch (chargeLevel) {
            case 2 -> 0.25F;
            case 3 -> 0.75F;
            default -> 0.0F;
        };
    }

    private static boolean isHandheldPerspective(ItemDisplayContext renderPerspective) {
        return renderPerspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || renderPerspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || renderPerspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
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
        CORE2,
        NORMAL_AURA,
        NORMAL_AURA_GLINT
    }

    private record ChargeRenderState(float core2MaxBrightness, GlowColor core2Color, float normalAuraIntensity) {
    }

    private record GlowColor(float red, float green, float blue) {
        private static final GlowColor WHITE = new GlowColor(1.0F, 1.0F, 1.0F);

        private static GlowColor fromRgb(int color) {
            return new GlowColor(
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F
            );
        }
    }
}
