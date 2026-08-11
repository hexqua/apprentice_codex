package jp.aquafactory.apprenticecodex.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class WaterCubeRenderTools {
    public static final RenderType RENDER_TYPE = RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);
    private static final int WATER_TINT = 0x3F76E4;
    private static final float WATER_RED = ((WATER_TINT >> 16) & 0xFF) / 255.0f;
    private static final float WATER_GREEN = ((WATER_TINT >> 8) & 0xFF) / 255.0f;
    private static final float WATER_BLUE = (WATER_TINT & 0xFF) / 255.0f;

    private WaterCubeRenderTools() {
    }

    public static void renderCube(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite,
                                  Vec3 position, float diameter, float alpha,
                                  float rotateX, float rotateY, float rotateZ) {
        poseStack.pushPose();
        poseStack.translate(position.x, position.y, position.z);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotateX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotateY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotateZ));
        drawTexturedCube(poseStack, buffer, sprite, diameter * 0.5f, alpha);
        poseStack.popPose();
    }

    public static TextureAtlasSprite resolveWaterSprite() {
        var stillTexture = IClientFluidTypeExtensions.of(Fluids.WATER)
                .getStillTexture(new FluidStack(Fluids.WATER, 1000));
        if (stillTexture == null) {
            return null;
        }

        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        return sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? null : sprite;
    }

    public static Vec3 localToWorld(BlockPos blockPos, Direction facing, Vec3 localPoint) {
        var relativeX = localPoint.x - 0.5d;
        var relativeZ = localPoint.z - 0.5d;
        var rotated = rotateVector(facing, new Vec3(relativeX, localPoint.y, relativeZ));
        return new Vec3(
                blockPos.getX() + rotated.x + 0.5d,
                blockPos.getY() + rotated.y,
                blockPos.getZ() + rotated.z + 0.5d
        );
    }

    public static Vec3 rotateVector(Direction facing, Vec3 vector) {
        return switch (facing) {
            case EAST -> new Vec3(-vector.z, vector.y, vector.x);
            case SOUTH -> new Vec3(-vector.x, vector.y, -vector.z);
            case WEST -> new Vec3(vector.z, vector.y, -vector.x);
            default -> vector;
        };
    }

    private static void drawTexturedCube(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite,
                                         float half, float alpha) {
        var pose = poseStack.last();
        var poseMatrix = pose.pose();
        var normalMatrix = pose.normal();
        var u0 = sprite.getU0();
        var u1 = sprite.getU1();
        var v0 = sprite.getV0();
        var v1 = sprite.getV1();

        face(buffer, poseMatrix, normalMatrix, alpha,
                new Vec3(-half, -half, half), new Vec3(half, -half, half),
                new Vec3(half, half, half), new Vec3(-half, half, half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(0.0d, 0.0d, 1.0d));
        face(buffer, poseMatrix, normalMatrix, alpha,
                new Vec3(half, -half, -half), new Vec3(-half, -half, -half),
                new Vec3(-half, half, -half), new Vec3(half, half, -half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(0.0d, 0.0d, -1.0d));
        face(buffer, poseMatrix, normalMatrix, alpha,
                new Vec3(-half, -half, -half), new Vec3(-half, -half, half),
                new Vec3(-half, half, half), new Vec3(-half, half, -half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(-1.0d, 0.0d, 0.0d));
        face(buffer, poseMatrix, normalMatrix, alpha,
                new Vec3(half, -half, half), new Vec3(half, -half, -half),
                new Vec3(half, half, -half), new Vec3(half, half, half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(1.0d, 0.0d, 0.0d));
        face(buffer, poseMatrix, normalMatrix, alpha,
                new Vec3(-half, half, half), new Vec3(half, half, half),
                new Vec3(half, half, -half), new Vec3(-half, half, -half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(0.0d, 1.0d, 0.0d));
        face(buffer, poseMatrix, normalMatrix, alpha,
                new Vec3(-half, -half, -half), new Vec3(half, -half, -half),
                new Vec3(half, -half, half), new Vec3(-half, -half, half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(0.0d, -1.0d, 0.0d));
    }

    private static void face(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix, float alpha,
                             Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                             float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3,
                             Vec3 normal) {
        vertex(buffer, poseMatrix, normalMatrix, p0, u0, v0, normal, alpha);
        vertex(buffer, poseMatrix, normalMatrix, p1, u1, v1, normal, alpha);
        vertex(buffer, poseMatrix, normalMatrix, p2, u2, v2, normal, alpha);
        vertex(buffer, poseMatrix, normalMatrix, p3, u3, v3, normal, alpha);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix, Vec3 position,
                               float u, float v, Vec3 normal, float alpha) {
        var transformedNormal = new org.joml.Vector3f((float) normal.x, (float) normal.y, (float) normal.z)
                .mul(normalMatrix)
                .normalize();
        buffer.addVertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .setColor(WATER_RED, WATER_GREEN, WATER_BLUE, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
    }
}
