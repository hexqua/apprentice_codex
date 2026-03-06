package jp.aquafactory.apprenticecodex.block.arcanuminajar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class ArcanumInAJarBlockEntityRenderer implements BlockEntityRenderer<ArcanumInAJarBlockEntity> {
    private static final ResourceLocation DUST_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/block/arcanum_in_a_jar_dust.png");
    private static final float ATLAS_TILE_SIZE = 0.5f;
    private static final float TOP_MIN_U = 0.0f;
    private static final float TOP_MIN_V = 0.0f;
    private static final float SIDE_MIN_U = 0.5f;
    private static final float SIDE_MIN_V = 0.0f;
    private static final float BOTTOM_MIN_U = 0.0f;
    private static final float BOTTOM_MIN_V = 0.5f;
    private static final float INNER_MIN_X = 4.0f / 16.0f;
    private static final float INNER_MAX_X = 12.0f / 16.0f;
    private static final float INNER_MIN_Z = 4.0f / 16.0f;
    private static final float INNER_MAX_Z = 12.0f / 16.0f;
    private static final float INNER_MIN_Y = 1.0f / 16.0f;
    private static final float INNER_MAX_Y = 11.0f / 16.0f;
    private static final float CENTER_XZ = 0.5f;
    private static final float CENTER_Y = (INNER_MIN_Y + INNER_MAX_Y) * 0.5f;
    private static final float INNER_CUBE_SIZE = 2.5f / 16.0f;
    private static final float OUTER_CUBE_SIZE = 4.0f / 16.0f;
    private static final double OUTER_CUBE_MAX_DISTANCE = 16.0;
    private static final double OUTER_CUBE_MAX_DISTANCE_SQR = OUTER_CUBE_MAX_DISTANCE * OUTER_CUBE_MAX_DISTANCE;
    private static final double MAX_RENDER_DISTANCE = 48.0;
    private static final double MAX_RENDER_DISTANCE_SQR = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;
    private static final RenderType SOLID_RENDER_TYPE =
            ApprenticeRenderTypes.color("arcanum_in_a_jar_cube_solid");

    public ArcanumInAJarBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // do nothing.
    }

    @Override
    public void render(@NotNull ArcanumInAJarBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        var level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var center = getRenderCenter(blockEntity);
        var distanceSqr = cameraPos.distanceToSqr(center);
        if (distanceSqr > MAX_RENDER_DISTANCE_SQR) {
            return;
        }

        // 既存のキューブ演出の上に粉の充填を重ねる.
        var time = level.getGameTime() + partialTick + (blockEntity.getBlockPos().asLong() & 31L);
        var fade = 0.5f + 0.5f * Mth.sin((float)(time * 0.045f));

        poseStack.pushPose();
        poseStack.translate(CENTER_XZ, CENTER_Y, CENTER_XZ);
        poseStack.mulPose(Axis.XP.rotationDegrees((float)(time * 0.55f)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float)(time * 0.85f)));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float)(time * 0.70f)));

        drawCube(
                poseStack,
                buffer.getBuffer(SOLID_RENDER_TYPE),
                INNER_CUBE_SIZE,
                255, 255, 255, 255,
                false
        );

        if (distanceSqr <= OUTER_CUBE_MAX_DISTANCE_SQR) {
            drawCube(
                    poseStack,
                    buffer.getBuffer(SOLID_RENDER_TYPE),
                    OUTER_CUBE_SIZE,
                    toChannel(Mth.lerp(fade, 0.45f, 0.78f)),
                    toChannel(Mth.lerp(fade, 0.88f, 0.44f)),
                    255,
                    255,
                    true
            );
        }

        poseStack.popPose();

        var fillRatio = blockEntity.getFillRatio();
        if (fillRatio <= 0.0f) {
            return;
        }

        var fillTopY = INNER_MIN_Y + (INNER_MAX_Y - INNER_MIN_Y) * fillRatio;
        var dustConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(DUST_TEXTURE));
        drawFillVolume(
                poseStack,
                dustConsumer,
                packedLight,
                packedOverlay,
                INNER_MIN_X,
                INNER_MIN_Y,
                INNER_MIN_Z,
                INNER_MAX_X,
                fillTopY,
                INNER_MAX_Z
        );
    }

    @Override
    public boolean shouldRender(@NotNull ArcanumInAJarBlockEntity blockEntity, @NotNull Vec3 cameraPos) {
        return cameraPos.distanceToSqr(getRenderCenter(blockEntity)) <= MAX_RENDER_DISTANCE_SQR;
    }

    private static Vec3 getRenderCenter(ArcanumInAJarBlockEntity blockEntity) {
        return Vec3.atCenterOf(blockEntity.getBlockPos()).add(0.0, CENTER_Y - 0.5, 0.0);
    }

    private static void drawFillVolume(PoseStack poseStack, VertexConsumer dustConsumer, int packedLight, int packedOverlay,
                                       float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        var pose = poseStack.last();
        var poseMat = pose.pose();
        var normalMat = pose.normal();
        var width = maxX - minX;
        var depth = maxZ - minZ;
        var height = maxY - minY;

        // 32x32 の統合テクスチャを 16x16 タイル相当で扱い、象限ごとに面を割り当てる.
        addQuad(
                poseMat, normalMat, dustConsumer,
                minX, minY, minZ, atlasU(SIDE_MIN_U, 0.0f), atlasV(SIDE_MIN_V, height),
                maxX, minY, minZ, atlasU(SIDE_MIN_U, width), atlasV(SIDE_MIN_V, height),
                maxX, maxY, minZ, atlasU(SIDE_MIN_U, width), atlasV(SIDE_MIN_V, 0.0f),
                minX, maxY, minZ, atlasU(SIDE_MIN_U, 0.0f), atlasV(SIDE_MIN_V, 0.0f),
                255, 255, 255, 255,
                packedLight, packedOverlay,
                0.0f, 0.0f, -1.0f
        );
        addQuad(
                poseMat, normalMat, dustConsumer,
                maxX, minY, maxZ, atlasU(SIDE_MIN_U, 0.0f), atlasV(SIDE_MIN_V, height),
                minX, minY, maxZ, atlasU(SIDE_MIN_U, width), atlasV(SIDE_MIN_V, height),
                minX, maxY, maxZ, atlasU(SIDE_MIN_U, width), atlasV(SIDE_MIN_V, 0.0f),
                maxX, maxY, maxZ, atlasU(SIDE_MIN_U, 0.0f), atlasV(SIDE_MIN_V, 0.0f),
                255, 255, 255, 255,
                packedLight, packedOverlay,
                0.0f, 0.0f, 1.0f
        );
        addQuad(
                poseMat, normalMat, dustConsumer,
                maxX, minY, minZ, atlasU(SIDE_MIN_U, 0.0f), atlasV(SIDE_MIN_V, height),
                maxX, minY, maxZ, atlasU(SIDE_MIN_U, depth), atlasV(SIDE_MIN_V, height),
                maxX, maxY, maxZ, atlasU(SIDE_MIN_U, depth), atlasV(SIDE_MIN_V, 0.0f),
                maxX, maxY, minZ, atlasU(SIDE_MIN_U, 0.0f), atlasV(SIDE_MIN_V, 0.0f),
                255, 255, 255, 255,
                packedLight, packedOverlay,
                1.0f, 0.0f, 0.0f
        );
        addQuad(
                poseMat, normalMat, dustConsumer,
                minX, minY, maxZ, atlasU(SIDE_MIN_U, 0.0f), atlasV(SIDE_MIN_V, height),
                minX, minY, minZ, atlasU(SIDE_MIN_U, depth), atlasV(SIDE_MIN_V, height),
                minX, maxY, minZ, atlasU(SIDE_MIN_U, depth), atlasV(SIDE_MIN_V, 0.0f),
                minX, maxY, maxZ, atlasU(SIDE_MIN_U, 0.0f), atlasV(SIDE_MIN_V, 0.0f),
                255, 255, 255, 255,
                packedLight, packedOverlay,
                -1.0f, 0.0f, 0.0f
        );
        addQuad(
                poseMat, normalMat, dustConsumer,
                minX, minY, maxZ, atlasU(BOTTOM_MIN_U, 0.0f), atlasV(BOTTOM_MIN_V, 0.0f),
                maxX, minY, maxZ, atlasU(BOTTOM_MIN_U, width), atlasV(BOTTOM_MIN_V, 0.0f),
                maxX, minY, minZ, atlasU(BOTTOM_MIN_U, width), atlasV(BOTTOM_MIN_V, depth),
                minX, minY, minZ, atlasU(BOTTOM_MIN_U, 0.0f), atlasV(BOTTOM_MIN_V, depth),
                255, 255, 255, 255,
                packedLight, packedOverlay,
                0.0f, -1.0f, 0.0f
        );
        addQuad(
                poseMat, normalMat, dustConsumer,
                minX, maxY, minZ, atlasU(TOP_MIN_U, 0.0f), atlasV(TOP_MIN_V, 0.0f),
                maxX, maxY, minZ, atlasU(TOP_MIN_U, width), atlasV(TOP_MIN_V, 0.0f),
                maxX, maxY, maxZ, atlasU(TOP_MIN_U, width), atlasV(TOP_MIN_V, depth),
                minX, maxY, maxZ, atlasU(TOP_MIN_U, 0.0f), atlasV(TOP_MIN_V, depth),
                255, 255, 255, 255,
                packedLight, packedOverlay,
                0.0f, 1.0f, 0.0f
        );
    }

    private static int toChannel(float value) {
        return Mth.clamp((int)(value * 255.0f), 0, 255);
    }

    private static void drawCube(PoseStack poseStack, VertexConsumer consumer, float size,
                                 int red, int green, int blue, int alpha, boolean innerOnly) {
        var half = size * 0.5f;
        var pose = poseStack.last().pose();

        // 外側キューブは内面向きの頂点順で組み、瓶越しに殻の内側が見える見た目に寄せる.
        quad(pose, consumer, -half, -half, half, half, -half, half, half, half, half, -half, half, half,
                red, green, blue, alpha, innerOnly);
        quad(pose, consumer, half, -half, -half, -half, -half, -half, -half, half, -half, half, half, -half,
                red, green, blue, alpha, innerOnly);
        quad(pose, consumer, -half, -half, -half, -half, -half, half, -half, half, half, -half, half, -half,
                red, green, blue, alpha, innerOnly);
        quad(pose, consumer, half, -half, half, half, -half, -half, half, half, -half, half, half, half,
                red, green, blue, alpha, innerOnly);
        quad(pose, consumer, -half, half, half, half, half, half, half, half, -half, -half, half, -half,
                red, green, blue, alpha, innerOnly);
        quad(pose, consumer, -half, -half, -half, half, -half, -half, half, -half, half, -half, -half, half,
                red, green, blue, alpha, innerOnly);
    }

    private static void quad(Matrix4f pose, VertexConsumer consumer,
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

    private static void addQuad(Matrix4f poseMat, Matrix3f normalMat, VertexConsumer consumer,
                                float x0, float y0, float z0, float u0, float v0,
                                float x1, float y1, float z1, float u1, float v1,
                                float x2, float y2, float z2, float u2, float v2,
                                float x3, float y3, float z3, float u3, float v3,
                                int red, int green, int blue, int alpha,
                                int packedLight, int packedOverlay,
                                float nx, float ny, float nz) {
        consumer.vertex(poseMat, x0, y0, z0).color(red, green, blue, alpha).uv(u0, v0)
                .overlayCoords(resolveOverlay(packedOverlay)).uv2(packedLight)
                .normal(normalMat, nx, ny, nz).endVertex();
        consumer.vertex(poseMat, x1, y1, z1).color(red, green, blue, alpha).uv(u1, v1)
                .overlayCoords(resolveOverlay(packedOverlay)).uv2(packedLight)
                .normal(normalMat, nx, ny, nz).endVertex();
        consumer.vertex(poseMat, x2, y2, z2).color(red, green, blue, alpha).uv(u2, v2)
                .overlayCoords(resolveOverlay(packedOverlay)).uv2(packedLight)
                .normal(normalMat, nx, ny, nz).endVertex();
        consumer.vertex(poseMat, x3, y3, z3).color(red, green, blue, alpha).uv(u3, v3)
                .overlayCoords(resolveOverlay(packedOverlay)).uv2(packedLight)
                .normal(normalMat, nx, ny, nz).endVertex();
    }

    private static int resolveOverlay(int packedOverlay) {
        return packedOverlay == 0 ? OverlayTexture.NO_OVERLAY : packedOverlay;
    }

    private static float atlasU(float tileMinU, float localU) {
        return tileMinU + (localU * ATLAS_TILE_SIZE);
    }

    private static float atlasV(float tileMinV, float localV) {
        return tileMinV + (localV * ATLAS_TILE_SIZE);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z,
                               int red, int green, int blue, int alpha) {
        consumer.vertex(pose, x, y, z).color(red, green, blue, alpha).endVertex();
    }
}
