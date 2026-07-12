package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.ImbuedSpellCoreClientEffectState;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastStaffClientEffectState;
import jp.aquafactory.apprenticecodex.model.SwingcastStaffModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SwingcastStaffRenderer extends GeoItemRenderer<AbstractSwingcastStaffItem> {
    private static final String STAFF_CORE_BONE = "staff_core";
    private static final String ORB_NONE_BONE = "orb_none";
    private static final String ORB_CONTAIN_BONE = "orb_contain";

    public SwingcastStaffRenderer() {
        super(new SwingcastStaffModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, AbstractSwingcastStaffItem animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        var currentStack = this.currentItemStack != null ? this.currentItemStack : ItemStack.EMPTY;

        if (isBoneOrChildOf(bone, STAFF_CORE_BONE)) {
            var coreState = ImbuedSpellCoreClientEffectState.resolve(currentStack, partialTick);
            var emissiveRenderType = RenderType.entityTranslucent(getTextureLocation(animatable));
            super.renderRecursively(
                    poseStack, animatable, bone, emissiveRenderType, bufferSource, bufferSource.getBuffer(emissiveRenderType),
                    isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay,
                    composeColour(coreState.red(), coreState.green(), coreState.blue(), coreState.alpha())
            );
            return;
        }

        if (isBoneOrChildOf(bone, ORB_NONE_BONE)) {
            if (SwingcastStaffClientEffectState.shouldRenderContainedOrb(currentStack)) {
                return;
            }

            var translucentRenderType = RenderType.entityTranslucent(getTextureLocation(animatable));
            super.renderRecursively(
                    poseStack, animatable, bone, translucentRenderType, bufferSource, bufferSource.getBuffer(translucentRenderType),
                    isReRender, partialTick, packedLight, packedOverlay,
                    scaleColour(colour, 1.0f, SwingcastStaffClientEffectState.getEmptyOrbAlpha())
            );
            return;
        }

        if (isBoneOrChildOf(bone, ORB_CONTAIN_BONE)) {
            if (!SwingcastStaffClientEffectState.shouldRenderContainedOrb(currentStack)) {
                return;
            }

            var orbState = SwingcastStaffClientEffectState.resolveOrb(currentStack);
            var additiveRenderType = ApprenticeRenderTypes.entityAdditiveGlowNoCull(
                    "swingcast_staff_orb_contain_additive",
                    getTextureLocation(animatable)
            );
            super.renderRecursively(
                    poseStack, animatable, bone, additiveRenderType, bufferSource, bufferSource.getBuffer(additiveRenderType),
                    isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay,
                    composeColour(orbState.red(), orbState.green(), orbState.blue(), orbState.alpha())
            );
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour
        );
    }

    private static int composeColour(float red, float green, float blue, float alpha) {
        return (Mth.clamp(Math.round(alpha * 255.0f), 0, 255) << 24)
                | (Mth.clamp(Math.round(red * 255.0f), 0, 255) << 16)
                | (Mth.clamp(Math.round(green * 255.0f), 0, 255) << 8)
                | Mth.clamp(Math.round(blue * 255.0f), 0, 255);
    }

    private static int scaleColour(int colour, float brightness, float alphaMultiplier) {
        var safeBrightness = Math.max(0.0f, brightness);
        var alpha = Math.round(((colour >>> 24) & 0xFF) * Mth.clamp(alphaMultiplier, 0.0f, 1.0f));
        var red = Math.round(((colour >>> 16) & 0xFF) * safeBrightness);
        var green = Math.round(((colour >>> 8) & 0xFF) * safeBrightness);
        var blue = Math.round((colour & 0xFF) * safeBrightness);
        return (Mth.clamp(alpha, 0, 255) << 24)
                | (Mth.clamp(red, 0, 255) << 16)
                | (Mth.clamp(green, 0, 255) << 8)
                | Mth.clamp(blue, 0, 255);
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }
}
