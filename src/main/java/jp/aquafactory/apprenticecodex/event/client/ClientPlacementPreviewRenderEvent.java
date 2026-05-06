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
    private static final UnitVertex[] UNIT_RING = buildUnitRing();

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
        var baseCenter = previewData.baseCenter();
        var radius = previewData.radius();
        var axis = Vec3.atLowerCornerOf(previewData.normal().getNormal()).scale(previewData.height());
        var tangentA = tangentA(previewData.normal());
        var tangentB = tangentB(previewData.normal(), tangentA);
        var red = ((activePreview.color() >> 16) & 0xFF) / 255.0f;
        var green = ((activePreview.color() >> 8) & 0xFF) / 255.0f;
        var blue = (activePreview.color() & 0xFF) / 255.0f;
        var buffer = buffers.getBuffer(RENDER_TYPE);
        var poseMatrix = poseStack.last().pose();

        for (int index = 0; index < UNIT_RING.length; index++) {
            var nextIndex = (index + 1) % UNIT_RING.length;
            var current = UNIT_RING[index];
            var next = UNIT_RING[nextIndex];
            var bottomA = ringPoint(baseCenter, tangentA, tangentB, current, radius);
            var bottomB = ringPoint(baseCenter, tangentA, tangentB, next, radius);
            var topB = bottomB.add(axis);
            var topA = bottomA.add(axis);
            addVertex(buffer, poseMatrix, bottomA, red, green, blue, BASE_ALPHA);
            addVertex(buffer, poseMatrix, bottomB, red, green, blue, BASE_ALPHA);
            addVertex(buffer, poseMatrix, topB, red, green, blue, 0.0f);
            addVertex(buffer, poseMatrix, topA, red, green, blue, 0.0f);
        }
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

    private static UnitVertex[] buildUnitRing() {
        var vertices = new UnitVertex[SEGMENT_COUNT];
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            var angle = Math.toRadians(i * (360.0 / SEGMENT_COUNT));
            vertices[i] = new UnitVertex((float) Math.cos(angle), (float) Math.sin(angle));
        }
        return vertices;
    }

    private record UnitVertex(float x, float z) {
    }
}
