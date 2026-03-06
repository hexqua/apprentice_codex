package jp.aquafactory.apprenticecodex.block.arcanuminajar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ArcanumInAJarBlockEntityRenderer implements BlockEntityRenderer<ArcanumInAJarBlockEntity> {
    private static final float CENTER_XZ = 0.5f;
    private static final float CENTER_Y = 7.0f / 16.0f;
    private static final float INNER_CUBE_SIZE = 2.5f / 16.0f;
    private static final float OUTER_CUBE_SIZE = 4.0f / 16.0f;
    private static final double OUTER_CUBE_MAX_DISTANCE = 16.0;
    private static final double INNER_CUBE_MAX_DISTANCE = 48.0;
    private static final double OUTER_CUBE_MAX_DISTANCE_SQR = OUTER_CUBE_MAX_DISTANCE * OUTER_CUBE_MAX_DISTANCE;
    private static final double INNER_CUBE_MAX_DISTANCE_SQR = INNER_CUBE_MAX_DISTANCE * INNER_CUBE_MAX_DISTANCE;
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
        if (distanceSqr > INNER_CUBE_MAX_DISTANCE_SQR) {
            return;
        }

        // 演出専用なので、回転と色変化はクライアント時刻だけで進める.
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
    }

    @Override
    public boolean shouldRender(@NotNull ArcanumInAJarBlockEntity blockEntity, @NotNull Vec3 cameraPos) {
        return cameraPos.distanceToSqr(getRenderCenter(blockEntity)) <= INNER_CUBE_MAX_DISTANCE_SQR;
    }

    private static Vec3 getRenderCenter(ArcanumInAJarBlockEntity blockEntity) {
        return Vec3.atCenterOf(blockEntity.getBlockPos()).add(0.0, CENTER_Y - 0.5, 0.0);
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

    private static void quad(org.joml.Matrix4f pose, VertexConsumer consumer,
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

    private static void vertex(VertexConsumer consumer, org.joml.Matrix4f pose, float x, float y, float z,
                               int red, int green, int blue, int alpha) {
        consumer.vertex(pose, x, y, z).color(red, green, blue, alpha).endVertex();
    }
}
