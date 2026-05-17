package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.model.ScrollcasterGauntletModel;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class ScrollcasterGauntletRenderer extends GeoItemRenderer<ScrollcasterGauntlet> {
    private static final String SCROLL_SOCKET_BONE = "scroll_socket";
    private static final String CORE_MAIN_BONE = "core_main";
    private static final float SELECTED_CORE_PERIOD_TICKS = 20.0F;
    private static final float SELECTED_CORE_MIN_BRIGHTNESS = 0.9F;
    private static final float SELECTED_CORE_MAX_BRIGHTNESS = 1.0F;

    public ScrollcasterGauntletRenderer() {
        super(new ScrollcasterGauntletModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ScrollcasterGauntlet animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        var stack = getCurrentItemStack();
        if (SCROLL_SOCKET_BONE.equals(bone.getName()) && !hasVisibleScrollSocket(stack)) {
            return;
        }

        if (CORE_MAIN_BONE.equals(bone.getName())) {
            renderCoreMain(
                    poseStack,
                    animatable,
                    bone,
                    renderType,
                    bufferSource,
                    partialTick,
                    packedLight,
                    packedOverlay,
                    red,
                    green,
                    blue,
                    alpha,
                    stack
            );
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    private void renderCoreMain(PoseStack poseStack, ScrollcasterGauntlet animatable, GeoBone bone,
                                RenderType renderType, MultiBufferSource bufferSource, float partialTick,
                                int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                                ItemStack stack) {
        var school = hasVisibleScrollSocket(stack) ? MagicTools.getImbuedSpellSchool(stack) : null;
        if (school == null) {
            var normalBuffer = bufferSource.getBuffer(renderType);
            super.renderRecursively(
                    poseStack,
                    animatable,
                    bone,
                    renderType,
                    bufferSource,
                    normalBuffer,
                    true,
                    partialTick,
                    packedLight,
                    packedOverlay,
                    red,
                    green,
                    blue,
                    alpha
            );
            return;
        }

        var tintColor = MagicTools.resolveSchoolTintColor(school);
        var brightness = pulse(partialTick, SELECTED_CORE_PERIOD_TICKS, SELECTED_CORE_MIN_BRIGHTNESS, SELECTED_CORE_MAX_BRIGHTNESS);
        var coreRenderType = RenderType.entityCutoutNoCull(getTextureLocation(animatable));
        var coreBuffer = bufferSource.getBuffer(coreRenderType);

        super.renderRecursively(
                poseStack,
                animatable,
                bone,
                coreRenderType,
                bufferSource,
                coreBuffer,
                true,
                partialTick,
                LightTexture.FULL_BRIGHT,
                packedOverlay,
                red(tintColor) * brightness,
                green(tintColor) * brightness,
                blue(tintColor) * brightness,
                alpha
        );
    }

    private static boolean hasVisibleScrollSocket(ItemStack stack) {
        return stack != null && !stack.isEmpty() && ScrollcasterGauntlet.hasAnyCalibrationScroll(stack);
    }

    private static float pulse(float partialTick, float periodTicks, float minBrightness, float maxBrightness) {
        var phase = (resolveRenderTick(partialTick) % periodTicks) / periodTicks * Mth.TWO_PI;
        var progress = (Mth.sin(phase) + 1.0F) * 0.5F;
        return Mth.lerp(progress, minBrightness, maxBrightness);
    }

    private static float resolveRenderTick(float partialTick) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            return level.getGameTime() + partialTick;
        }
        return Util.getMillis() / 50.0F;
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
