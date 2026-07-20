package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookClientRenderState;
import jp.aquafactory.apprenticecodex.model.ChargecastCatalystbookModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

public final class ChargecastCatalystbookRenderer extends GeoItemRenderer<ChargecastCatalystbook> {
    private static final String LEFT_RUNE_BONE = "left_cover_rune";
    private static final String RIGHT_RUNE_BONE = "right_cover_rune";
    private static final String SPELL_ICON_BONE = "spell_icon";
    private static final String COOLDOWN_LEFT_BONE = "cooldown_left";
    private static final String COOLDOWN_RIGHT_BONE = "cooldown_right";
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "textures/geo/chargecast_catalystbook.png"
    );
    private static final RenderType RUNE_RENDER_TYPE = ApprenticeRenderTypes.entityTranslucentNoCull(
            "chargecast_catalystbook_rune_emissive",
            TEXTURE
    );
    private static final RenderType COOLDOWN_RENDER_TYPE = ApprenticeRenderTypes.entityAdditiveGlowNoCull(
            "chargecast_catalystbook_cooldown_additive",
            TEXTURE
    );

    private final GeoModel<ChargecastCatalystbook> openModel = new ChargecastCatalystbookModel(true);
    private final GeoModel<ChargecastCatalystbook> closeModel = new ChargecastCatalystbookModel(false);
    private SpecialPass specialPass = SpecialPass.NONE;
    private ChargecastCatalystbookClientRenderState.RenderState renderState;

    public ChargecastCatalystbookRenderer() {
        super(new ChargecastCatalystbookModel(false));
    }

    @Override
    public GeoModel<ChargecastCatalystbook> getGeoModel() {
        return isHandheldPerspective(renderPerspective) ? openModel : closeModel;
    }

    @Override
    public void preRender(PoseStack poseStack, ChargecastCatalystbook animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
        if (!isReRender) {
            var stack = currentItemStack != null ? currentItemStack : ItemStack.EMPTY;
            renderState = ChargecastCatalystbookClientRenderState.resolve(stack, partialTick);
        }
    }

    @Override
    public void postRender(PoseStack poseStack, ChargecastCatalystbook animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                           float partialTick, int packedLight, int packedOverlay,
                           int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
        if (isReRender || renderState == null) {
            return;
        }

        var runeColor = renderState.runeColor();
        renderSpecialPass(
                model, poseStack, bufferSource, animatable, SpecialPass.RUNE, RUNE_RENDER_TYPE,
                partialTick, LightTexture.FULL_BRIGHT,
                runeColor.red(), runeColor.green(), runeColor.blue(), 1.0F
        );

        if (isHandheldPerspective(renderPerspective) && renderState.spellIcon() != null) {
            var iconRenderType = RenderType.entityCutoutNoCull(renderState.spellIcon());
            renderSpecialPass(
                    model, poseStack, bufferSource, animatable, SpecialPass.SPELL_ICON, iconRenderType,
                    partialTick, packedLight, 1.0F, 1.0F, 1.0F, 1.0F
            );
        }

        if (isHandheldPerspective(renderPerspective) && renderState.cooldown().visible()) {
            var brightness = renderState.cooldown().brightness();
            renderSpecialPass(
                    model, poseStack, bufferSource, animatable, SpecialPass.COOLDOWN, COOLDOWN_RENDER_TYPE,
                    partialTick, LightTexture.FULL_BRIGHT,
                    brightness, brightness, brightness, 1.0F
            );
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ChargecastCatalystbook animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  int colour) {
        if (specialPass != SpecialPass.NONE) {
            if (isSpecialPassTarget(bone, specialPass)) {
                super.renderRecursively(
                        poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                        packedLight, packedOverlay, colour
                );
            } else {
                renderChildBonesOnly(
                        poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                        packedLight, packedOverlay, colour
                );
            }
            return;
        }

        if (isAnySpecialBone(bone)) {
            // 通常パスから特殊ボーンを外し、通常モデルにだけ ItemRenderer の Glint を残す。
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
        specialPass = SpecialPass.NONE;
        renderState = null;
    }

    private void renderSpecialPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                   ChargecastCatalystbook animatable, SpecialPass pass, RenderType renderType,
                                   float partialTick, int packedLight,
                                   float red, float green, float blue, float alpha) {
        specialPass = pass;
        try {
            reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    renderType,
                    bufferSource.getBuffer(renderType),
                    partialTick,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    toColour(red, green, blue, alpha)
            );
        } finally {
            specialPass = SpecialPass.NONE;
        }
    }

    private void renderChildBonesOnly(PoseStack poseStack, ChargecastCatalystbook animatable, GeoBone bone,
                                      RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                      boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                      int colour) {
        poseStack.pushPose();
        if (bone.isTrackingMatrices()) {
            var poseState = new Matrix4f(poseStack.last().pose());
            bone.setModelSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, itemRenderTranslations));
        }
        RenderUtil.prepMatrixForBone(poseStack, bone);
        renderChildBones(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour
        );
        poseStack.popPose();
    }

    private static int toColour(float red, float green, float blue, float alpha) {
        return (Math.round(clamp01(alpha) * 255.0F) << 24)
                | (Math.round(clamp01(red) * 255.0F) << 16)
                | (Math.round(clamp01(green) * 255.0F) << 8)
                | Math.round(clamp01(blue) * 255.0F);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static boolean isSpecialPassTarget(GeoBone bone, SpecialPass pass) {
        return switch (pass) {
            case RUNE -> isBoneOrChildOf(bone, LEFT_RUNE_BONE) || isBoneOrChildOf(bone, RIGHT_RUNE_BONE);
            case SPELL_ICON -> isBoneOrChildOf(bone, SPELL_ICON_BONE);
            case COOLDOWN -> isBoneOrChildOf(bone, COOLDOWN_LEFT_BONE)
                    || isBoneOrChildOf(bone, COOLDOWN_RIGHT_BONE);
            case NONE -> false;
        };
    }

    private static boolean isAnySpecialBone(GeoBone bone) {
        return isBoneOrChildOf(bone, LEFT_RUNE_BONE)
                || isBoneOrChildOf(bone, RIGHT_RUNE_BONE)
                || isBoneOrChildOf(bone, SPELL_ICON_BONE)
                || isBoneOrChildOf(bone, COOLDOWN_LEFT_BONE)
                || isBoneOrChildOf(bone, COOLDOWN_RIGHT_BONE);
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (var current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHandheldPerspective(ItemDisplayContext perspective) {
        return perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    private enum SpecialPass {
        NONE,
        RUNE,
        SPELL_ICON,
        COOLDOWN
    }
}
