package jp.aquafactory.apprenticecodex.renderer.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.armor.SoulcollectorRobeItem;
import jp.aquafactory.apprenticecodex.model.SoulcollectorRobeModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.RenderUtils;

public final class SoulcollectorRobeRenderer extends GeoArmorRenderer<SoulcollectorRobeItem> {
    private static final String RUNE_TINT_RIGHT_BONE = "rune_tint_right";
    private static final String RUNE_TINT_LEFT_BONE = "rune_tint_left";
    private static final int FIRST_RUNE_COLOR = 0xA42FB6;
    private static final int SECOND_RUNE_COLOR = 0x5F1BAF;
    private static final float RUNE_CYCLE_TICKS = 40.0F;

    private float runeRed = 1.0F;
    private float runeGreen = 1.0F;
    private float runeBlue = 1.0F;

    public SoulcollectorRobeRenderer() {
        super(new SoulcollectorRobeModel());
    }

    @Override
    public void preRender(PoseStack poseStack, SoulcollectorRobeItem animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                          int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, alpha);
        updateRuneColor(partialTick);
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        runeRed = 1.0F;
        runeGreen = 1.0F;
        runeBlue = 1.0F;
    }

    @Override
    public void renderRecursively(PoseStack poseStack, SoulcollectorRobeItem animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            bone.setModelSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.entityRenderTranslations));
        }

        if (!isRuneBone(bone)) {
            super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }

        poseStack.pushPose();
        RenderUtils.prepMatrixForBone(poseStack, bone);
        renderCubesOfBone(poseStack, bone, buffer, LightTexture.FULL_BRIGHT, packedOverlay,
                runeRed, runeGreen, runeBlue, alpha);
        renderChildBones(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }

    private void updateRuneColor(float partialTick) {
        var level = Minecraft.getInstance().level;
        float tick = (level == null ? 0.0F : level.getGameTime()) + partialTick;
        float blend = (Mth.sin(Mth.TWO_PI * tick / RUNE_CYCLE_TICKS) + 1.0F) * 0.5F;
        runeRed = Mth.lerp(blend, red(FIRST_RUNE_COLOR), red(SECOND_RUNE_COLOR));
        runeGreen = Mth.lerp(blend, green(FIRST_RUNE_COLOR), green(SECOND_RUNE_COLOR));
        runeBlue = Mth.lerp(blend, blue(FIRST_RUNE_COLOR), blue(SECOND_RUNE_COLOR));
    }

    private static boolean isRuneBone(GeoBone bone) {
        return RUNE_TINT_RIGHT_BONE.equals(bone.getName()) || RUNE_TINT_LEFT_BONE.equals(bone.getName());
    }

    private static float red(int color) {
        return ((color >> 16) & 0xFF) / 255.0F;
    }

    private static float green(int color) {
        return ((color >> 8) & 0xFF) / 255.0F;
    }

    private static float blue(int color) {
        return (color & 0xFF) / 255.0F;
    }
}
