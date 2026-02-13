package jp.aquafactory.apprenticecodex.spell.magelight;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;
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

        // LOD.
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var center = Vec3.atCenterOf(blockEntity.getBlockPos());
        var dist = cam.distanceTo(center);

        float wobbleMul;
        if (dist <= 16.0) wobbleMul = 1.0f;
        else if (dist >= 24.0) wobbleMul = 0.0f;
        else {
            wobbleMul = (float)((24.0 - dist) / (24.0 - 16.0));
        }

        // 浮く演出用.
        var t = (level.getGameTime() + partialTick) / 10.0;
        var h = blockEntity.getBlockPos().hashCode();
        var phase = (h & 1023) / 1023.0 * (Math.PI * 2.0);
        var y = (float)(Math.sin(t + phase) * 0.08f * wobbleMul);
        var baseLift = 0.25f;

        // 近距離だけ松明っぽいパーティクルをクライアントで炊く.
        if (dist <= 12.0) {
            var r = level.getRandom();
            var gt = level.getGameTime();
            if ((gt + (h & 31)) % 20 == 0) {
                // Yは松明分の高さ＋浮かせる高さ＋上下分の高さを考慮.
                var px = blockEntity.getBlockPos().getX() + 0.5;
                var py = blockEntity.getBlockPos().getY() + 0.5 + 0.25 + baseLift + y;
                var pz = blockEntity.getBlockPos().getZ() + 0.5;

                level.addParticle(ParticleTypes.FLAME, px, py, pz, 0.0, 0.002, 0.0);
                if (r.nextInt(5) == 0) {
                    level.addParticle(ParticleTypes.SMOKE, px, py, pz, 0.0, 0.001, 0.0);
                }
            }
        }

        poseStack.pushPose();
        poseStack.translate(0.5, baseLift + y, 0.5);

        // 原点から動かす.
        poseStack.translate(-0.5, 0.0, -0.5);
        var state = blockEntity.getBlockState();
        renderBlockModel(state, level, blockEntity.getBlockPos(), poseStack, buffer, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private void renderBlockModel(BlockState state, Level level, BlockPos pos, PoseStack poseStack,
                                  MultiBufferSource buffer, int packedLight, int packedOverlay) {

        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        var model = dispatcher.getBlockModel(state);
        var modelData = ModelData.EMPTY;
        var color = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        var random = RandomSource.create(42);

        for (var renderType : model.getRenderTypes(state, random, modelData)) {
            var entityRenderType = RenderTypeHelper.getEntityRenderType(renderType, false);
            dispatcher.getModelRenderer().renderModel(
                    poseStack.last(),
                    buffer.getBuffer(entityRenderType),
                    state,
                    model,
                    r,
                    g,
                    b,
                    packedLight,
                    packedOverlay,
                    modelData,
                    renderType
            );
        }
    }
}
