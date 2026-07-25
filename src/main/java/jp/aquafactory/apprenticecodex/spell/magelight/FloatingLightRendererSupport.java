package jp.aquafactory.apprenticecodex.spell.magelight;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;

public final class FloatingLightRendererSupport {
    public static final float BASE_LIFT = 0.25F;

    private FloatingLightRendererSupport() {
    }

    public static RenderOffset getRenderOffset(BlockEntity blockEntity, float partialTick) {
        var level = blockEntity.getLevel();
        if (level == null) {
            return new RenderOffset(Double.POSITIVE_INFINITY, 0.0F);
        }

        var cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var center = Vec3.atCenterOf(blockEntity.getBlockPos());
        var distance = cameraPosition.distanceTo(center);
        float wobbleMultiplier;
        if (distance <= 16.0) {
            wobbleMultiplier = 1.0F;
        } else if (distance >= 24.0) {
            wobbleMultiplier = 0.0F;
        } else {
            wobbleMultiplier = (float) ((24.0 - distance) / 8.0);
        }

        var time = (level.getGameTime() + partialTick) / 10.0;
        var hash = blockEntity.getBlockPos().hashCode();
        var phase = (hash & 1023) / 1023.0 * (Math.PI * 2.0);
        var yOffset = (float) (Math.sin(time + phase) * 0.08F * wobbleMultiplier);
        return new RenderOffset(distance, yOffset);
    }

    public static void renderBlockModel(BlockState state, Level level, BlockPos pos, PoseStack poseStack,
                                        MultiBufferSource buffer, int packedLight, int packedOverlay) {
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        var model = dispatcher.getBlockModel(state);
        var modelData = ModelData.EMPTY;
        var color = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        var random = RandomSource.create(42);

        for (var renderType : model.getRenderTypes(state, random, modelData)) {
            var entityRenderType = RenderTypeHelper.getEntityRenderType(renderType, false);
            dispatcher.getModelRenderer().renderModel(
                    poseStack.last(),
                    buffer.getBuffer(entityRenderType),
                    state,
                    model,
                    red,
                    green,
                    blue,
                    packedLight,
                    packedOverlay,
                    modelData,
                    renderType
            );
        }
    }

    public record RenderOffset(double distance, float yOffset) {
    }
}
