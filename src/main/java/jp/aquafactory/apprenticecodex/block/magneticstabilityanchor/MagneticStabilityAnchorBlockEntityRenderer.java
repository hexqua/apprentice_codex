package jp.aquafactory.apprenticecodex.block.magneticstabilityanchor;

import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.spell.magelight.FloatingLightRendererSupport;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.jetbrains.annotations.NotNull;

public class MagneticStabilityAnchorBlockEntityRenderer implements BlockEntityRenderer<MagneticStabilityAnchorBlockEntity> {
    public MagneticStabilityAnchorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // do nothing.
    }

    @Override
    public void render(@NotNull MagneticStabilityAnchorBlockEntity blockEntity, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        var level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        var offset = FloatingLightRendererSupport.getRenderOffset(blockEntity, partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0D, MagneticStabilityAnchorBlock.FLOATING_OFFSET + offset.yOffset(), 0.0D);
        FloatingLightRendererSupport.renderBlockModel(
                blockEntity.getBlockState(), level, blockEntity.getBlockPos(), poseStack, buffer, packedLight, packedOverlay
        );
        poseStack.popPose();
    }
}
