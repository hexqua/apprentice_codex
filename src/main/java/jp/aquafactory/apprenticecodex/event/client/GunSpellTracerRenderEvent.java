package jp.aquafactory.apprenticecodex.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class GunSpellTracerRenderEvent {
    private static final ResourceLocation TRACER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/gun_spell_tracer.png");
    private static final RenderType TRACER_RENDER_TYPE =
            RenderType.entityTranslucentEmissive(TRACER_TEXTURE);
    private static final int MAX_ACTIVE_TRACERS = 64;
    private static final float MAX_LIFETIME_TICKS = 5.0F;
    private static final double FADE_OUT_START_DISTANCE = 48.0D;
    private static final double FADE_OUT_END_DISTANCE = 128.0D;
    private static final double TRACER_WIDTH = 1.0D / 16.0D;
    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0E-8D;
    private static final Deque<ActiveTracer> ACTIVE_TRACERS = new ArrayDeque<>();

    private GunSpellTracerRenderEvent() {
    }

    public static void enqueueTracer(Vec3 start, Vec3 end, float speedBlocksPerTick, float length) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null || !isFinite(start) || !isFinite(end)
                || !Float.isFinite(speedBlocksPerTick) || speedBlocksPerTick <= 0.0F
                || !Float.isFinite(length) || length <= 0.0F
                || end.distanceToSqr(start) < MIN_DIRECTION_LENGTH_SQR) {
            return;
        }

        ACTIVE_TRACERS.addLast(new ActiveTracer(level, start, end, speedBlocksPerTick, length));
        while (ACTIVE_TRACERS.size() > MAX_ACTIVE_TRACERS) {
            ACTIVE_TRACERS.removeFirst();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && Minecraft.getInstance().level == null
                && !ACTIVE_TRACERS.isEmpty()) {
            ACTIVE_TRACERS.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE_TRACERS.isEmpty()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            ACTIVE_TRACERS.clear();
            return;
        }

        var partialTick = event.getPartialTick();
        var renderTime = level.getGameTime() + partialTick;
        var cameraPosition = event.getCamera().getPosition();
        var poseStack = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();
        var buffer = buffers.getBuffer(TRACER_RENDER_TYPE);

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        var iterator = ACTIVE_TRACERS.iterator();
        while (iterator.hasNext()) {
            var tracer = iterator.next();
            if (tracer.level != level) {
                iterator.remove();
                continue;
            }

            if (!Double.isFinite(tracer.startRenderTime)) {
                tracer.startRenderTime = renderTime;
            }

            var age = renderTime - tracer.startRenderTime;
            var traveledDistance = Math.max(0.0D, age * tracer.speedBlocksPerTick);
            var totalDistance = tracer.end.distanceTo(tracer.start);
            var tailDistance = Math.max(0.0D, traveledDistance - tracer.length);
            if (age >= MAX_LIFETIME_TICKS || tailDistance >= totalDistance) {
                iterator.remove();
                continue;
            }

            var headDistance = Math.min(traveledDistance, totalDistance);
            if (headDistance - tailDistance <= 1.0E-6D) {
                continue;
            }

            renderTracer(poseStack, buffer, tracer, tailDistance, headDistance);
        }

        poseStack.popPose();
        buffers.endBatch(TRACER_RENDER_TYPE);
    }

    private static void renderTracer(PoseStack poseStack, VertexConsumer buffer, ActiveTracer tracer,
                                     double tailDistance, double headDistance) {
        var direction = tracer.end.subtract(tracer.start).normalize();
        var reference = Math.abs(direction.y) > 0.99D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(0.0D, 1.0D, 0.0D);
        var sideA = direction.cross(reference).normalize();
        var sideB = direction.cross(sideA).normalize();
        var tail = tracer.start.add(direction.scale(tailDistance));
        var head = tracer.start.add(direction.scale(headDistance));
        var tailAlpha = alphaAtDistance(tailDistance);
        var headAlpha = alphaAtDistance(headDistance);

        if (tailAlpha <= 0.0F && headAlpha <= 0.0F) {
            return;
        }

        drawQuad(poseStack, buffer, tail, head, direction, sideA, tailAlpha, headAlpha);
        drawQuad(poseStack, buffer, tail, head, direction, sideB, tailAlpha, headAlpha);
    }

    private static void drawQuad(PoseStack poseStack, VertexConsumer buffer, Vec3 tail, Vec3 head,
                                 Vec3 direction, Vec3 side, float tailAlpha, float headAlpha) {
        var halfWidth = side.scale(TRACER_WIDTH * 0.5D);
        var tailMinus = tail.subtract(halfWidth);
        var tailPlus = tail.add(halfWidth);
        var headPlus = head.add(halfWidth);
        var headMinus = head.subtract(halfWidth);
        var normal = direction.cross(side).normalize();
        var pose = poseStack.last();

        // テクスチャ下端のフェードを後端へ割り当て、移動済み経路を残光として残さない。
        addVertex(buffer, pose.pose(), pose.normal(), tailMinus, 0.0F, 1.0F, normal, tailAlpha);
        addVertex(buffer, pose.pose(), pose.normal(), tailPlus, 1.0F, 1.0F, normal, tailAlpha);
        addVertex(buffer, pose.pose(), pose.normal(), headPlus, 1.0F, 0.0F, normal, headAlpha);
        addVertex(buffer, pose.pose(), pose.normal(), headMinus, 0.0F, 0.0F, normal, headAlpha);
    }

    private static void addVertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                                  Vec3 position, float u, float v, Vec3 normal, float alpha) {
        buffer.vertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normalMatrix, (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }

    private static float alphaAtDistance(double distance) {
        if (distance <= FADE_OUT_START_DISTANCE) {
            return 1.0F;
        }
        if (distance >= FADE_OUT_END_DISTANCE) {
            return 0.0F;
        }
        return (float) ((FADE_OUT_END_DISTANCE - distance)
                / (FADE_OUT_END_DISTANCE - FADE_OUT_START_DISTANCE));
    }

    private static boolean isFinite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private static final class ActiveTracer {
        private final ClientLevel level;
        private final Vec3 start;
        private final Vec3 end;
        private final float speedBlocksPerTick;
        private final float length;
        private double startRenderTime = Double.NaN;

        private ActiveTracer(ClientLevel level, Vec3 start, Vec3 end, float speedBlocksPerTick, float length) {
            this.level = level;
            this.start = start;
            this.end = end;
            this.speedBlocksPerTick = speedBlocksPerTick;
            this.length = length;
        }
    }
}
