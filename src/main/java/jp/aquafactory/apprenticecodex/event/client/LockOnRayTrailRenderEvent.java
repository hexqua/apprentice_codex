package jp.aquafactory.apprenticecodex.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRayCurve;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayDeque;
import java.util.Deque;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class LockOnRayTrailRenderEvent {
    private static final RenderType RENDER_TYPE = ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly(
            "lock_on_ray_trail", ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID,
                    "textures/spell/lock_on_ray_laser.png"));
    private static final Deque<Segment> SEGMENTS = new ArrayDeque<>();
    private static final int FADE_TICKS = 8;

    private LockOnRayTrailRenderEvent() {}

    public static void enqueue(LockOnRayCurve curve) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        SEGMENTS.addLast(new Segment(level, curve, level.getGameTime()));
        // 多人数連射でも古い描画データを無制限に保持しない。
        while (SEGMENTS.size() > 4096) SEGMENTS.removeFirst();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null) { SEGMENTS.clear(); return; }
        SEGMENTS.removeIf(segment -> segment.level != level || level.getGameTime() - segment.created > FADE_TICKS + 1);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || SEGMENTS.isEmpty()) return;
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) return;
        var camera = event.getCamera().getPosition();
        double time = level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(true);
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        var buffers = minecraft.renderBuffers().bufferSource();
        var buffer = buffers.getBuffer(RENDER_TYPE);
        for (var segment : SEGMENTS) {
            if (segment.level != level) continue;
            double age = time - segment.created;
            double head = Mth.clamp(age, 0, 1);
            var curve = segment.curve;
            int count = Mth.clamp((int) Math.ceil(curve.start().distanceTo(curve.end()) * 8), 8, 128);
            for (int i = 0; i < count; i++) {
                double a = i / (double) count, b = Math.min((i + 1.0) / count, head);
                if (a >= b) break;
                var start = curve.position(a);
                var end = curve.position(b);
                var sideA = side(curve.tangent(a), camera.subtract(start));
                var sideB = side(curve.tangent(b), camera.subtract(end));
                if (sideA.dot(sideB) < 0) sideB = sideB.scale(-1);
                draw(pose.last(), buffer, start, end, sideA, sideB, 0.12, brightness(age - a) * 0.45f, brightness(age - b) * 0.45f);
                draw(pose.last(), buffer, start, end, sideA, sideB, 0.055, brightness(age - a), brightness(age - b));
            }
        }
        pose.popPose();
        buffers.endBatch(RENDER_TYPE);
    }

    private static float brightness(double age) {
        double t = Mth.clamp(age / FADE_TICKS, 0, 1);
        return (float) (1 - t * t * (3 - 2 * t));
    }

    private static Vec3 side(Vec3 tangent, Vec3 cameraDirection) {
        var side = tangent.cross(cameraDirection);
        if (side.lengthSqr() < 1.0e-10) side = tangent.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0e-10) side = new Vec3(1, 0, 0);
        return side.normalize();
    }

    private static void draw(PoseStack.Pose pose, VertexConsumer buffer, Vec3 start, Vec3 end,
                             Vec3 sideA, Vec3 sideB, double width, float brightnessA, float brightnessB) {
        vertex(pose, buffer, start.subtract(sideA.scale(width)), 0, brightnessA);
        vertex(pose, buffer, start.add(sideA.scale(width)), 1, brightnessA);
        vertex(pose, buffer, end.add(sideB.scale(width)), 1, brightnessB);
        vertex(pose, buffer, end.subtract(sideB.scale(width)), 0, brightnessB);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vec3 point, float u, float brightness) {
        // 長手方向の中央を使い、tick区間ごとのテクスチャ端で継ぎ目を作らない。
        // ONE + ONEの加算合成は頂点alphaでは暗くならないため、RGBを直接減衰させる。
        buffer.addVertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .setColor(brightness, brightness, brightness, 1f).setUv(u, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0, 1, 0);
    }

    private record Segment(ClientLevel level, LockOnRayCurve curve, long created) {}
}
