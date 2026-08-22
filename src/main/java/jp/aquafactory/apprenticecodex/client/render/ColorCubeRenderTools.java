package jp.aquafactory.apprenticecodex.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

public final class ColorCubeRenderTools {
    private ColorCubeRenderTools() {
    }

    public static void drawCube(PoseStack poseStack, VertexConsumer consumer, float size,
                                int red, int green, int blue, int alpha, boolean reverse) {
        var half = size * 0.5F;
        var pose = poseStack.last().pose();

        quad(consumer, pose, -half, -half, half, half, -half, half, half, half, half, -half, half, half,
                red, green, blue, alpha, reverse);
        quad(consumer, pose, half, -half, -half, -half, -half, -half, -half, half, -half, half, half, -half,
                red, green, blue, alpha, reverse);
        quad(consumer, pose, -half, -half, -half, -half, -half, half, -half, half, half, -half, half, -half,
                red, green, blue, alpha, reverse);
        quad(consumer, pose, half, -half, half, half, -half, -half, half, half, -half, half, half, half,
                red, green, blue, alpha, reverse);
        quad(consumer, pose, -half, half, half, half, half, half, half, half, -half, -half, half, -half,
                red, green, blue, alpha, reverse);
        quad(consumer, pose, -half, -half, -half, half, -half, -half, half, -half, half, -half, -half, half,
                red, green, blue, alpha, reverse);
    }

    private static void quad(VertexConsumer consumer, Matrix4f pose,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             int red, int green, int blue, int alpha, boolean reverse) {
        if (reverse) {
            vertex(consumer, pose, x4, y4, z4, red, green, blue, alpha);
            vertex(consumer, pose, x3, y3, z3, red, green, blue, alpha);
            vertex(consumer, pose, x2, y2, z2, red, green, blue, alpha);
            vertex(consumer, pose, x1, y1, z1, red, green, blue, alpha);
            return;
        }

        vertex(consumer, pose, x1, y1, z1, red, green, blue, alpha);
        vertex(consumer, pose, x2, y2, z2, red, green, blue, alpha);
        vertex(consumer, pose, x3, y3, z3, red, green, blue, alpha);
        vertex(consumer, pose, x4, y4, z4, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z,
                               int red, int green, int blue, int alpha) {
        consumer.vertex(pose, x, y, z).color(red, green, blue, alpha).endVertex();
    }
}
