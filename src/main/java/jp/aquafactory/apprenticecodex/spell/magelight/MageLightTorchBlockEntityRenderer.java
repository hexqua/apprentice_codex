package jp.aquafactory.apprenticecodex.spell.magelight;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.particles.ParticleTypes;
import org.jetbrains.annotations.NotNull;

public class MageLightTorchBlockEntityRenderer implements BlockEntityRenderer<MageLightTorchBlockEntity> {
    public MageLightTorchBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // do nothing.
    }

    @Override
    public void render(@NotNull MageLightTorchBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {

        var level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        var offset = FloatingLightRendererSupport.getRenderOffset(blockEntity, partialTick);
        var h = blockEntity.getBlockPos().hashCode();

        // 近距離だけ松明っぽいパーティクルをクライアントで炊く.
        if (offset.distance() <= 12.0) {
            var r = level.getRandom();
            var gt = level.getGameTime();
            if ((gt + (h & 31)) % 20 == 0) {
                // Yは松明分の高さ＋浮かせる高さ＋上下分の高さを考慮.
                var px = blockEntity.getBlockPos().getX() + 0.5;
                var py = blockEntity.getBlockPos().getY() + 0.5 + 0.25
                        + FloatingLightRendererSupport.BASE_LIFT + offset.yOffset();
                var pz = blockEntity.getBlockPos().getZ() + 0.5;

                level.addParticle(ParticleTypes.FLAME, px, py, pz, 0.0, 0.002, 0.0);
                if (r.nextInt(5) == 0) {
                    level.addParticle(ParticleTypes.SMOKE, px, py, pz, 0.0, 0.001, 0.0);
                }
            }
        }

        poseStack.pushPose();
        poseStack.translate(0.5, FloatingLightRendererSupport.BASE_LIFT + offset.yOffset(), 0.5);

        // 原点から動かす.
        poseStack.translate(-0.5, 0.0, -0.5);
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
