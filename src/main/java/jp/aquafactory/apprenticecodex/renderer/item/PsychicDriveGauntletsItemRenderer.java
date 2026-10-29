package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntlets;
import jp.aquafactory.apprenticecodex.model.PsychicDriveGauntletsModel;
import jp.aquafactory.apprenticecodex.renderer.PsychicDriveGauntletsGlowLayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PsychicDriveGauntletsItemRenderer extends GeoItemRenderer<PsychicDriveGauntlets> {
    public PsychicDriveGauntletsItemRenderer() {
        super(new PsychicDriveGauntletsModel<>());
        addRenderLayer(new PsychicDriveGauntletsGlowLayer<>(this));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, PsychicDriveGauntlets item, GeoBone bone, RenderType type,
                                  MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        boolean glow = PsychicDriveGauntletsGlowLayer.glowing(bone);
        boolean dim = glow && (bone.getParent() == null || !PsychicDriveGauntletsGlowLayer.glowing(bone.getParent()));
        float brightness = dim ? 0.65F : 1;
        super.renderRecursively(poseStack, item, bone, type, buffers, buffer, isReRender, partialTick,
                glow ? LightTexture.FULL_BRIGHT : packedLight, packedOverlay,
                red * brightness, green * brightness, blue * brightness, alpha);
    }

    @Override
    public void renderByItem(@NotNull ItemStack stack, @NotNull ItemDisplayContext context,
                             @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {
        // 手持ち抑止をこのrendererに限定し、専用の浮遊描画には適用しない。
        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return;
        }
        super.renderByItem(stack, context, poseStack, buffer, packedLight, packedOverlay);
    }
}
