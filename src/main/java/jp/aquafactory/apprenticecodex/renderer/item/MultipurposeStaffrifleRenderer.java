package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.model.MultipurposeStaffrifleModel;
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
import software.bernie.geckolib.util.RenderUtil;

public final class MultipurposeStaffrifleRenderer extends GeoItemRenderer<MultipurposeStaffrifle> {
    private static final String RUNE_BARREL_BONE = "rune_barrel";
    private static final String EMITTER_BONE = "emitter";
    private static final String CHAMBER_BONE = "chamber";
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/multipurpose_staffrifle.png");
    private static final RenderType RUNE_BARREL_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly("multipurpose_staffrifle_rune_barrel_additive", TEXTURE);
    private static final RenderType EMITTER_RENDER_TYPE = RenderType.entityTranslucent(TEXTURE);
    private static final RenderType CHAMBER_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly("multipurpose_staffrifle_chamber_additive", TEXTURE);

    private GlowPass glowPass = GlowPass.NONE;

    public MultipurposeStaffrifleRenderer() {
        super(new MultipurposeStaffrifleModel());
    }

    @Override
    public void postRender(PoseStack poseStack, MultipurposeStaffrifle animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                           int packedLight, int packedOverlay, int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);

        if (isReRender) {
            return;
        }

        float runeBrightness = resolveRuneBarrelBrightness(partialTick);
        var runeColor = resolveRuneBarrelColor();
        renderGlowPass(model, poseStack, bufferSource, animatable, GlowPass.RUNE_BARREL, RUNE_BARREL_RENDER_TYPE,
                partialTick, rgba(runeColor.red() * runeBrightness, runeColor.green() * runeBrightness,
                        runeColor.blue() * runeBrightness, 1.0F));

        float emitterBrightness = resolveEmitterBrightness(partialTick);
        renderGlowPass(model, poseStack, bufferSource, animatable, GlowPass.EMITTER, EMITTER_RENDER_TYPE,
                partialTick, multiplyRgb(colour, emitterBrightness));

        float chamberBrightness = resolveChamberBrightness(partialTick);
        renderGlowPass(model, poseStack, bufferSource, animatable, GlowPass.CHAMBER, CHAMBER_RENDER_TYPE,
                partialTick, rgba(chamberBrightness, chamberBrightness, chamberBrightness, 1.0F));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, MultipurposeStaffrifle animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  int colour) {
        var runeBarrelBone = isBoneOrChildOf(bone, RUNE_BARREL_BONE);
        var emitterBone = isBoneOrChildOf(bone, EMITTER_BONE);
        var chamberBone = isBoneOrChildOf(bone, CHAMBER_BONE);
        var specialBone = runeBarrelBone || emitterBone || chamberBone;

        if (this.glowPass == GlowPass.NONE && specialBone) {
            renderChildBonesOnly(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.glowPass == GlowPass.RUNE_BARREL) {
            renderGlowPassBone(
                    poseStack, animatable, bone, runeBarrelBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.glowPass == GlowPass.EMITTER) {
            renderGlowPassBone(
                    poseStack, animatable, bone, emitterBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.glowPass == GlowPass.CHAMBER) {
            renderGlowPassBone(
                    poseStack, animatable, bone, chamberBone, renderType, bufferSource, buffer, isReRender,
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
        this.glowPass = GlowPass.NONE;
    }

    private void renderGlowPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                MultipurposeStaffrifle animatable, GlowPass pass, RenderType renderType,
                                float partialTick, int colour) {
        this.glowPass = pass;
        try {
            // 特殊ボーンは通常パスと glint から切り離し、発光用パスだけで描画する。
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

    private void renderGlowPassBone(PoseStack poseStack, MultipurposeStaffrifle animatable, GeoBone bone,
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

    private void renderChildBonesOnly(PoseStack poseStack, MultipurposeStaffrifle animatable, GeoBone bone,
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

    private static float resolveRuneBarrelBrightness(float partialTick) {
        float time = resolveRenderTime(partialTick);
        return 0.9F + 0.1F * Mth.sin(time * Mth.TWO_PI / 40.0F);
    }

    private static GlowColor resolveRuneBarrelColor() {
        return new GlowColor(1.0F, 1.0F, 1.0F);
    }

    private static float resolveEmitterBrightness(float partialTick) {
        float time = resolveRenderTime(partialTick);
        return 0.95F + 0.05F * Mth.sin(time * Mth.TWO_PI / 10.0F);
    }

    private static float resolveChamberBrightness(float partialTick) {
        float manaRatio = resolvePlayerManaRatio();
        float baseBrightness = 0.5F + manaRatio * 0.5F;
        float time = resolveRenderTime(partialTick);
        return Mth.clamp(baseBrightness + 0.25F * Mth.sin(time * Mth.TWO_PI / 30.0F), 0.0F, 1.0F);
    }

    private static float resolvePlayerManaRatio() {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        if (level == null || player == null) {
            return 1.0F;
        }

        float maxMana = (float) player.getAttributeValue(AttributeRegistry.MAX_MANA);
        if (maxMana <= 0.0F) {
            return 1.0F;
        }

        return Mth.clamp(ClientMagicData.getPlayerMana() / maxMana, 0.0F, 1.0F);
    }

    private static float resolveRenderTime(float partialTick) {
        var level = Minecraft.getInstance().level;
        return level == null ? partialTick : level.getGameTime() + partialTick;
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }

    private static int multiplyRgb(int colour, float multiplier) {
        int alpha = colour & 0xFF000000;
        int red = Math.round(((colour >> 16) & 0xFF) * multiplier);
        int green = Math.round(((colour >> 8) & 0xFF) * multiplier);
        int blue = Math.round((colour & 0xFF) * multiplier);
        return alpha | (Mth.clamp(red, 0, 255) << 16) | (Mth.clamp(green, 0, 255) << 8) | Mth.clamp(blue, 0, 255);
    }

    private static int rgba(float red, float green, float blue, float alpha) {
        return (Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24)
                | (Mth.clamp(Math.round(red * 255.0F), 0, 255) << 16)
                | (Mth.clamp(Math.round(green * 255.0F), 0, 255) << 8)
                | Mth.clamp(Math.round(blue * 255.0F), 0, 255);
    }

    private record GlowColor(float red, float green, float blue) {
    }

    private enum GlowPass {
        NONE,
        RUNE_BARREL,
        EMITTER,
        CHAMBER
    }
}
