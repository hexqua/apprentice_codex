package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldClientEffectState;
import jp.aquafactory.apprenticecodex.model.ReflectcastShieldModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ReflectcastShieldRenderer extends GeoItemRenderer<ReflectcastShield> {
    private static final String PLATE_FLASH_BONE = "plate_flash";
    private static final int FULL_BRIGHT_LIGHT = 0x00F000F0;

    public ReflectcastShieldRenderer() {
        super(new ReflectcastShieldModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ReflectcastShield animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        if (PLATE_FLASH_BONE.equals(bone.getName())) {
            var currentStack = this.currentItemStack != null ? this.currentItemStack : ItemStack.EMPTY;
            var effectState = ReflectcastShieldClientEffectState.resolve(currentStack, this.renderPerspective, partialTick);
            if (!effectState.isVisible()) {
                return;
            }

            var flashRenderType = ApprenticeRenderTypes.entityAdditiveGlowNoCull(
                    "reflectcast_shield_plate_flash_additive",
                    getTextureLocation(animatable)
            );
            var flashBuffer = ItemRenderer.getFoilBufferDirect(
                    bufferSource,
                    flashRenderType,
                    this.renderPerspective == ItemDisplayContext.GUI,
                    currentStack.hasFoil()
            );
            var flashColour = packColour(
                    effectState.red(),
                    effectState.green(),
                    effectState.blue(),
                    effectState.alpha()
            );
            super.renderRecursively(
                    poseStack, animatable, bone, flashRenderType, bufferSource, flashBuffer, isReRender, partialTick,
                    FULL_BRIGHT_LIGHT, packedOverlay, flashColour
            );
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour
        );
    }

    private static int packColour(float red, float green, float blue, float alpha) {
        var a = Mth.clamp(Math.round(alpha * 255.0f), 0, 255);
        var r = Mth.clamp(Math.round(red * 255.0f), 0, 255);
        var g = Mth.clamp(Math.round(green * 255.0f), 0, 255);
        var b = Mth.clamp(Math.round(blue * 255.0f), 0, 255);
        return a << 24 | r << 16 | g << 8 | b;
    }
}
