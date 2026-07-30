package jp.aquafactory.apprenticecodex.spell.rifthole;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class RiftHoleBlockEntityRenderer implements BlockEntityRenderer<RiftHoleBlockEntity> {
    private static final float EPSILON = 0.001f;

    public RiftHoleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull RiftHoleBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        var level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        var vertexConsumer = buffer.getBuffer(RenderType.endPortal());
        for (var direction : Direction.values()) {
            if (!shouldRenderFace(level, blockEntity, direction)) {
                continue;
            }

            drawFace(poseStack.last().pose(), vertexConsumer, direction);
        }
    }

    private static boolean shouldRenderFace(Level level, RiftHoleBlockEntity blockEntity, Direction direction) {
        var neighborPos = blockEntity.getBlockPos().relative(direction);
        var neighborState = level.getBlockState(neighborPos);
        if (neighborState.is(BlockRegistry.RIFT_HOLE.get())) {
            return false;
        }

        return neighborState.canOcclude() && neighborState.isFaceSturdy(level, neighborPos, direction.getOpposite());
    }

    private static void drawFace(Matrix4f matrix, VertexConsumer vertexConsumer, Direction direction) {
        switch (direction) {
            case DOWN -> addDoubleSidedQuad(matrix, vertexConsumer,
                    0.0f, EPSILON, 0.0f,
                    0.0f, EPSILON, 1.0f,
                    1.0f, EPSILON, 1.0f,
                    1.0f, EPSILON, 0.0f);
            case UP -> addDoubleSidedQuad(matrix, vertexConsumer,
                    0.0f, 1.0f - EPSILON, 0.0f,
                    1.0f, 1.0f - EPSILON, 0.0f,
                    1.0f, 1.0f - EPSILON, 1.0f,
                    0.0f, 1.0f - EPSILON, 1.0f);
            case NORTH -> addDoubleSidedQuad(matrix, vertexConsumer,
                    0.0f, 0.0f, EPSILON,
                    1.0f, 0.0f, EPSILON,
                    1.0f, 1.0f, EPSILON,
                    0.0f, 1.0f, EPSILON);
            case SOUTH -> addDoubleSidedQuad(matrix, vertexConsumer,
                    0.0f, 0.0f, 1.0f - EPSILON,
                    0.0f, 1.0f, 1.0f - EPSILON,
                    1.0f, 1.0f, 1.0f - EPSILON,
                    1.0f, 0.0f, 1.0f - EPSILON);
            case WEST -> addDoubleSidedQuad(matrix, vertexConsumer,
                    EPSILON, 0.0f, 0.0f,
                    EPSILON, 1.0f, 0.0f,
                    EPSILON, 1.0f, 1.0f,
                    EPSILON, 0.0f, 1.0f);
            case EAST -> addDoubleSidedQuad(matrix, vertexConsumer,
                    1.0f - EPSILON, 0.0f, 0.0f,
                    1.0f - EPSILON, 0.0f, 1.0f,
                    1.0f - EPSILON, 1.0f, 1.0f,
                    1.0f - EPSILON, 1.0f, 0.0f);
        }
    }

    private static void addDoubleSidedQuad(Matrix4f matrix, VertexConsumer vertexConsumer,
                                           float x0, float y0, float z0,
                                           float x1, float y1, float z1,
                                           float x2, float y2, float z2,
                                           float x3, float y3, float z3) {
        addQuad(matrix, vertexConsumer, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
        addQuad(matrix, vertexConsumer, x3, y3, z3, x2, y2, z2, x1, y1, z1, x0, y0, z0);
    }

    private static void addQuad(Matrix4f matrix, VertexConsumer vertexConsumer,
                                float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3) {
        vertexConsumer.addVertex(matrix, x0, y0, z0).setColor(255, 255, 255, 255);
        vertexConsumer.addVertex(matrix, x1, y1, z1).setColor(255, 255, 255, 255);
        vertexConsumer.addVertex(matrix, x2, y2, z2).setColor(255, 255, 255, 255);
        vertexConsumer.addVertex(matrix, x3, y3, z3).setColor(255, 255, 255, 255);
    }
}
