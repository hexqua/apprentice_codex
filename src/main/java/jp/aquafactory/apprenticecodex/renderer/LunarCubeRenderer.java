package jp.aquafactory.apprenticecodex.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class LunarCubeRenderer {
    private LunarCubeRenderer() {}

    public static void drawCube(PoseStack poseStack, VertexConsumer buffer, float size, float alpha,
                                 float red, float green, float blue, boolean scaleRgbByAlpha) {
        var half = size * 0.5f;
        var pose = poseStack.last();
        var poseMatrix = pose.pose();
        var normalMatrix = pose.normal();
        var resolvedRed = scaleRgbByAlpha ? red * alpha : red;
        var resolvedGreen = scaleRgbByAlpha ? green * alpha : green;
        var resolvedBlue = scaleRgbByAlpha ? blue * alpha : blue;

        addQuad(buffer, poseMatrix, normalMatrix,
                -half, -half, half,
                half, -half, half,
                half, half, half,
                -half, half, half,
                0.0f, 0.0f, 1.0f,
                resolvedRed, resolvedGreen, resolvedBlue, alpha);
        addQuad(buffer, poseMatrix, normalMatrix,
                half, -half, -half,
                -half, -half, -half,
                -half, half, -half,
                half, half, -half,
                0.0f, 0.0f, -1.0f,
                resolvedRed, resolvedGreen, resolvedBlue, alpha);
        addQuad(buffer, poseMatrix, normalMatrix,
                -half, -half, -half,
                -half, -half, half,
                -half, half, half,
                -half, half, -half,
                -1.0f, 0.0f, 0.0f,
                resolvedRed, resolvedGreen, resolvedBlue, alpha);
        addQuad(buffer, poseMatrix, normalMatrix,
                half, -half, half,
                half, -half, -half,
                half, half, -half,
                half, half, half,
                1.0f, 0.0f, 0.0f,
                resolvedRed, resolvedGreen, resolvedBlue, alpha);
        addQuad(buffer, poseMatrix, normalMatrix,
                -half, half, -half,
                half, half, -half,
                half, half, half,
                -half, half, half,
                0.0f, 1.0f, 0.0f,
                resolvedRed, resolvedGreen, resolvedBlue, alpha);
        addQuad(buffer, poseMatrix, normalMatrix,
                -half, -half, half,
                half, -half, half,
                half, -half, -half,
                -half, -half, -half,
                0.0f, -1.0f, 0.0f,
                resolvedRed, resolvedGreen, resolvedBlue, alpha);
    }

    private static void addQuad(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float normalX, float normalY, float normalZ,
                                float red, float green, float blue, float alpha) {
        addVertex(buffer, poseMatrix, normalMatrix, x1, y1, z1, 0.0f, 1.0f, normalX, normalY, normalZ, red, green, blue, alpha);
        addVertex(buffer, poseMatrix, normalMatrix, x2, y2, z2, 1.0f, 1.0f, normalX, normalY, normalZ, red, green, blue, alpha);
        addVertex(buffer, poseMatrix, normalMatrix, x3, y3, z3, 1.0f, 0.0f, normalX, normalY, normalZ, red, green, blue, alpha);
        addVertex(buffer, poseMatrix, normalMatrix, x4, y4, z4, 0.0f, 0.0f, normalX, normalY, normalZ, red, green, blue, alpha);
    }

    private static void addVertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                                  float x, float y, float z, float u, float v,
                                  float normalX, float normalY, float normalZ,
                                  float red, float green, float blue, float alpha) {
        var transformedNormal = normalMatrix.transform(new org.joml.Vector3f(normalX, normalY, normalZ));
        buffer.addVertex(poseMatrix, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
    }
}
