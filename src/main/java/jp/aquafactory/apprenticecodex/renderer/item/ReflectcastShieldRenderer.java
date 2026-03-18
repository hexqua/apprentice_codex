package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import jp.aquafactory.apprenticecodex.model.ReflectcastShieldModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ReflectcastShieldRenderer extends GeoItemRenderer<ReflectcastShield> {
    private static final String PLATE_FLASH_BONE = "plate_flash";

    public ReflectcastShieldRenderer() {
        super(new ReflectcastShieldModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ReflectcastShield animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (PLATE_FLASH_BONE.equals(bone.getName())) {
            // 将来は別 RenderType に流すが、現段階では描画しない。
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }
}
