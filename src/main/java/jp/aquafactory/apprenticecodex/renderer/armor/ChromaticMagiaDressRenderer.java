package jp.aquafactory.apprenticecodex.renderer.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.armor.ChromaticMagiaDressItem;
import jp.aquafactory.apprenticecodex.model.ChromaticMagiaDressModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
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
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.RenderUtil;

public class ChromaticMagiaDressRenderer extends GeoArmorRenderer<ChromaticMagiaDressItem> {
    private static final String HAT_EMISSIVE_BONE = "hat_emissive";
    private static final String HAT_GEM_BONE = "hat_gem";
    private static final String LENS_TRANS_BONE = "lens_trans";
    private static final String CORE_MAIN_BONE = "core_main";
    private static final String RUNE_TINT_LEFT_BONE = "rune_tint_left";
    private static final String RUNE_TINT_RIGHT_BONE = "rune_tint_right";
    private static final String BOOT_CORE_LEFT_BONE = "boot_core_left";
    private static final String BOOT_CORE_RIGHT_BONE = "boot_core_right";

    private static final int SPECIAL_COLOR_COUNT = 21;
    private static final float SPECIAL_COLOR_HOLD_TICKS = 10.0F;
    private static final float SPECIAL_COLOR_TRANSITION_TICKS = 40.0F;
    private static final float SPECIAL_COLOR_STEP_TICKS = SPECIAL_COLOR_HOLD_TICKS + SPECIAL_COLOR_TRANSITION_TICKS;
    private static final int DEFAULT_COLOR = 0xFFFFFF;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/chromatic_magia_dress.png");
    private static final RenderType EMISSIVE_RENDER_TYPE =
            ApprenticeRenderTypes.entityTranslucentNoCull("chromatic_magia_dress_emissive", TEXTURE);
    private static final RenderType TRANSLUCENT_RENDER_TYPE =
            ApprenticeRenderTypes.entityTranslucentNoCull("chromatic_magia_dress_translucent", TEXTURE);

    private SpecialRenderPass specialRenderPass = SpecialRenderPass.NONE;

    public ChromaticMagiaDressRenderer() {
        super(new ChromaticMagiaDressModel());
    }

    @Override
    public void postRender(PoseStack poseStack, ChromaticMagiaDressItem animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                           int packedLight, int packedOverlay, int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);

        if (isReRender) {
            return;
        }

        float hatBrightness = resolveHatEmissiveBrightness(partialTick);
        renderSpecialPass(model, poseStack, bufferSource, animatable, SpecialRenderPass.HAT_EMISSIVE,
                EMISSIVE_RENDER_TYPE, partialTick, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                makeColor(hatBrightness, hatBrightness, hatBrightness, alpha(colour)));

        renderSpecialPass(model, poseStack, bufferSource, animatable, SpecialRenderPass.LENS,
                TRANSLUCENT_RENDER_TYPE, partialTick, packedLight, packedOverlay, makeColor(red(colour), green(colour), blue(colour), 1.0F));

        var specialColor = resolveSpecialColor(partialTick);
        renderSpecialPass(model, poseStack, bufferSource, animatable, SpecialRenderPass.SPECIAL_COLOR,
                EMISSIVE_RENDER_TYPE, partialTick, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                makeColor(specialColor.red(), specialColor.green(), specialColor.blue(), alpha(colour)));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ChromaticMagiaDressItem animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  int colour) {
        boolean hatEmissiveBone = isBoneOrChildOf(bone, HAT_EMISSIVE_BONE);
        boolean lensBone = isBoneOrChildOf(bone, LENS_TRANS_BONE);
        boolean specialColorBone = isSpecialColorBone(bone);

        if (this.specialRenderPass == SpecialRenderPass.NONE && (hatEmissiveBone || lensBone || specialColorBone)) {
            renderChildBonesOnly(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.specialRenderPass == SpecialRenderPass.HAT_EMISSIVE) {
            renderSpecialPassBone(
                    poseStack, animatable, bone, hatEmissiveBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.specialRenderPass == SpecialRenderPass.LENS) {
            renderSpecialPassBone(
                    poseStack, animatable, bone, lensBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.specialRenderPass == SpecialRenderPass.SPECIAL_COLOR) {
            renderSpecialPassBone(
                    poseStack, animatable, bone, specialColorBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, colour
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
        this.specialRenderPass = SpecialRenderPass.NONE;
    }

    private void renderSpecialPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                   ChromaticMagiaDressItem animatable, SpecialRenderPass pass, RenderType renderType,
                                   float partialTick, int packedLight, int packedOverlay, int colour) {
        this.specialRenderPass = pass;
        try {
            // 特殊ボーンは通常パスと glint から切り離し、対象ボーンだけを専用 RenderType で再描画する。
            this.reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    renderType,
                    bufferSource.getBuffer(renderType),
                    partialTick,
                    packedLight,
                    packedOverlay,
                    colour
            );
        } finally {
            this.specialRenderPass = SpecialRenderPass.NONE;
        }
    }

    private void renderSpecialPassBone(PoseStack poseStack, ChromaticMagiaDressItem animatable, GeoBone bone,
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

    private void renderChildBonesOnly(PoseStack poseStack, ChromaticMagiaDressItem animatable, GeoBone bone,
                                      RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                      boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                      int colour) {
        poseStack.pushPose();

        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            bone.setModelSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, this.entityRenderTranslations));
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

    private SpecialColor resolveSpecialColor(float partialTick) {
        float time = getClientRenderTick(partialTick);
        float cycleTicks = SPECIAL_COLOR_COUNT * SPECIAL_COLOR_STEP_TICKS;
        float cycleTime = time % cycleTicks;
        if (cycleTime < 0.0F) {
            cycleTime += cycleTicks;
        }

        int currentIndex = Mth.floor(cycleTime / SPECIAL_COLOR_STEP_TICKS);
        int nextIndex = (currentIndex + 1) % SPECIAL_COLOR_COUNT;
        float stepTime = cycleTime - currentIndex * SPECIAL_COLOR_STEP_TICKS;
        float progress = stepTime <= SPECIAL_COLOR_HOLD_TICKS
                ? 0.0F
                : Mth.clamp((stepTime - SPECIAL_COLOR_HOLD_TICKS) / SPECIAL_COLOR_TRANSITION_TICKS, 0.0F, 1.0F);

        int currentColor = resolveSpecialColorAt(currentIndex);
        int nextColor = resolveSpecialColorAt(nextIndex);
        return new SpecialColor(
                Mth.lerp(progress, red(currentColor), red(nextColor)),
                Mth.lerp(progress, green(currentColor), green(nextColor)),
                Mth.lerp(progress, blue(currentColor), blue(nextColor))
        );
    }

    private int resolveSpecialColorAt(int index) {
        if (index <= 0) {
            return DEFAULT_COLOR;
        }

        var schools = ChromaticMagiaDressItem.readSchoolHistory(getCurrentStack());
        int schoolIndex = index - 1;
        if (schoolIndex >= schools.size()) {
            return DEFAULT_COLOR;
        }

        return MagicTools.resolveSchoolTintColor(schools.get(schoolIndex));
    }

    private static float resolveHatEmissiveBrightness(float partialTick) {
        float time = getClientRenderTick(partialTick);
        return 0.90F + 0.10F * Mth.sin(time * 0.045F);
    }

    private static float getClientRenderTick(float partialTick) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return partialTick;
        }

        return level.getGameTime() + partialTick;
    }

    private static float red(int rgb) {
        return ((rgb >> 16) & 0xFF) / 255.0F;
    }

    private static float green(int rgb) {
        return ((rgb >> 8) & 0xFF) / 255.0F;
    }

    private static float blue(int rgb) {
        return (rgb & 0xFF) / 255.0F;
    }

    private static float alpha(int argb) {
        return ((argb >>> 24) & 0xFF) / 255.0F;
    }

    private static int makeColor(float red, float green, float blue, float alpha) {
        var safeAlpha = Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        var safeRed = Math.round(Mth.clamp(red, 0.0F, 1.0F) * 255.0F);
        var safeGreen = Math.round(Mth.clamp(green, 0.0F, 1.0F) * 255.0F);
        var safeBlue = Math.round(Mth.clamp(blue, 0.0F, 1.0F) * 255.0F);
        return (safeAlpha << 24) | (safeRed << 16) | (safeGreen << 8) | safeBlue;
    }

    private static boolean isSpecialColorBone(GeoBone bone) {
        return isBoneOrChildOf(bone, HAT_GEM_BONE)
                || isBoneOrChildOf(bone, CORE_MAIN_BONE)
                || isBoneOrChildOf(bone, RUNE_TINT_LEFT_BONE)
                || isBoneOrChildOf(bone, RUNE_TINT_RIGHT_BONE)
                || isBoneOrChildOf(bone, BOOT_CORE_LEFT_BONE)
                || isBoneOrChildOf(bone, BOOT_CORE_RIGHT_BONE);
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }

    private enum SpecialRenderPass {
        NONE,
        HAT_EMISSIVE,
        LENS,
        SPECIAL_COLOR
    }

    private record SpecialColor(float red, float green, float blue) {
    }
}
