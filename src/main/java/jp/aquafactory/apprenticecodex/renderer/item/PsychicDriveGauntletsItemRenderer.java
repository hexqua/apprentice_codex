package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntlets;
import jp.aquafactory.apprenticecodex.model.PsychicDriveGauntletsModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PsychicDriveGauntletsItemRenderer extends GeoItemRenderer<PsychicDriveGauntlets> {
    public PsychicDriveGauntletsItemRenderer() {
        super(new PsychicDriveGauntletsModel<>());
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
