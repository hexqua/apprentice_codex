package jp.aquafactory.apprenticecodex.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientPlacementPreviewRenderEvent {
    private static final RenderType RENDER_TYPE = ApprenticeRenderTypes.translucentColorNoCull("placement_preview_translucent");
    private static final int SEGMENT_COUNT = 8;
    private static final float BASE_ALPHA = 0.55f;

    private ClientPlacementPreviewRenderEvent() {
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        var activePreview = ClientPlacementPreviewManager.getActivePreview();
        if (activePreview == null) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            return;
        }

        var poseStack = event.getPoseStack();
        var cameraPosition = event.getCamera().getPosition();
        var buffers = minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        renderPreview(poseStack, buffers, activePreview);
        poseStack.popPose();

        buffers.endBatch(RENDER_TYPE);
    }

    private static void renderPreview(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                      ClientPlacementPreviewManager.ActivePreview activePreview) {
        var previewData = activePreview.previewData();
        renderRing(
                poseStack,
                buffers,
                previewData.baseCenter(),
                previewData.radius(),
                previewData.height(),
                previewData.normal(),
                activePreview.color(),
                SEGMENT_COUNT,
                BASE_ALPHA
        );
    }

    static void renderRing(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                           Vec3 baseCenter, float radius, float height, Direction normal,
                           int color, int segmentCount, float alpha) {
        var axis = Vec3.atLowerCornerOf(normal.getNormal()).scale(height);
        var tangentA = tangentA(normal);
        var tangentB = tangentB(normal, tangentA);
        var red = ((color >> 16) & 0xFF) / 255.0f;
        var green = ((color >> 8) & 0xFF) / 255.0f;
        var blue = (color & 0xFF) / 255.0f;
        var buffer = buffers.getBuffer(RENDER_TYPE);
        var poseMatrix = poseStack.last().pose();

        for (int index = 0; index < segmentCount; index++) {
            var current = unitVertex(index, segmentCount);
            var next = unitVertex(index + 1, segmentCount);
            var bottomA = ringPoint(baseCenter, tangentA, tangentB, current, radius);
            var bottomB = ringPoint(baseCenter, tangentA, tangentB, next, radius);
            var topB = bottomB.add(axis);
            var topA = bottomA.add(axis);
            addVertex(buffer, poseMatrix, bottomA, red, green, blue, alpha);
            addVertex(buffer, poseMatrix, bottomB, red, green, blue, alpha);
            addVertex(buffer, poseMatrix, topB, red, green, blue, 0.0f);
            addVertex(buffer, poseMatrix, topA, red, green, blue, 0.0f);
        }
    }

    static void endBatch(MultiBufferSource.BufferSource buffers) {
        buffers.endBatch(RENDER_TYPE);
    }

    private static Vec3 ringPoint(Vec3 center, Vec3 tangentA, Vec3 tangentB, UnitVertex unitVertex, float radius) {
        return center
                .add(tangentA.scale(unitVertex.x() * radius))
                .add(tangentB.scale(unitVertex.z() * radius));
    }

    private static Vec3 tangentA(Direction normal) {
        return switch (normal.getAxis()) {
            case Y -> new Vec3(1.0, 0.0, 0.0);
            case X -> new Vec3(0.0, 1.0, 0.0);
            case Z -> new Vec3(1.0, 0.0, 0.0);
        };
    }

    private static Vec3 tangentB(Direction normal, Vec3 tangentA) {
        var axis = Vec3.atLowerCornerOf(normal.getNormal());
        return axis.cross(tangentA).normalize();
    }

    private static void addVertex(VertexConsumer buffer, Matrix4f poseMatrix, Vec3 point,
                                  float red, float green, float blue, float alpha) {
        addVertex(buffer, poseMatrix, (float) point.x, (float) point.y, (float) point.z, red, green, blue, alpha);
    }

    private static void addVertex(VertexConsumer buffer, Matrix4f poseMatrix, float x, float y, float z,
                                  float red, float green, float blue, float alpha) {
        buffer.vertex(poseMatrix, x, y, z)
                .color(red, green, blue, alpha)
                .endVertex();
    }

    private static UnitVertex unitVertex(int index, int segmentCount) {
        var angle = Math.toRadians(index * (360.0 / segmentCount));
        return new UnitVertex((float) Math.cos(angle), (float) Math.sin(angle));
    }

    private record UnitVertex(float x, float z) {
    }
}
