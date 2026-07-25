package jp.aquafactory.apprenticecodex.spell.wizardlamp;

import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.spell.magelight.FloatingLightRendererSupport;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.jetbrains.annotations.NotNull;

public class WizardlampLanternBlockEntityRenderer implements BlockEntityRenderer<WizardlampLanternBlockEntity> {
    public WizardlampLanternBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // do nothing.
    }

    @Override
    public void render(@NotNull WizardlampLanternBlockEntity blockEntity, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        var level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        var offset = FloatingLightRendererSupport.getRenderOffset(blockEntity, partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0, WizardlampLanternBlock.FLOATING_OFFSET + offset.yOffset(), 0.0);
        FloatingLightRendererSupport.renderBlockModel(
                blockEntity.getBlockState(),
                level,
                blockEntity.getBlockPos(),
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }
}
