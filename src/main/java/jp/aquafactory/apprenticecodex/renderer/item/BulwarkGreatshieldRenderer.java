package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.ImbuedSpellCoreClientEffectState;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshield;
import jp.aquafactory.apprenticecodex.model.BulwarkGreatshieldModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class BulwarkGreatshieldRenderer extends GeoItemRenderer<BulwarkGreatshield> {
    private static final String FRAME_BONE = "frame";
    private static final String CORE_UPPER_BONE = "core_upper";
    private static final int FRAME_MIN_BLOCK_LIGHT = 4;

    public BulwarkGreatshieldRenderer() {
        super(new BulwarkGreatshieldModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, BulwarkGreatshield animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        if (isBoneOrChildOf(bone, CORE_UPPER_BONE)) {
            var currentStack = this.currentItemStack != null ? this.currentItemStack : ItemStack.EMPTY;
            // WisdomShard の選択魔法ではなく、スタック内の Imbue 魔法のクールダウンだけを参照する。
            var coreState = ImbuedSpellCoreClientEffectState.resolve(currentStack, partialTick);
            var emissiveRenderType = RenderType.entityTranslucent(getTextureLocation(animatable));
            super.renderRecursively(
                    poseStack, animatable, bone, emissiveRenderType, bufferSource, bufferSource.getBuffer(emissiveRenderType),
                    isReRender, partialTick, LightTexture.FULL_BRIGHT, packedOverlay,
                    packColour(coreState.red(), coreState.green(), coreState.blue(), coreState.alpha())
            );
            return;
        }

        var adjustedLight = isBoneOrChildOf(bone, FRAME_BONE)
                ? raiseBlockLightFloor(packedLight, FRAME_MIN_BLOCK_LIGHT)
                : packedLight;
        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                adjustedLight, packedOverlay, colour
        );
    }

    private static int raiseBlockLightFloor(int packedLight, int minBlockLight) {
        return LightTexture.pack(Math.max(LightTexture.block(packedLight), minBlockLight), LightTexture.sky(packedLight));
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }

    private static int packColour(float red, float green, float blue, float alpha) {
        var a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        var r = Mth.clamp(Math.round(red * 255.0F), 0, 255);
        var g = Mth.clamp(Math.round(green * 255.0F), 0, 255);
        var b = Mth.clamp(Math.round(blue * 255.0F), 0, 255);
        return a << 24 | r << 16 | g << 8 | b;
    }
}
