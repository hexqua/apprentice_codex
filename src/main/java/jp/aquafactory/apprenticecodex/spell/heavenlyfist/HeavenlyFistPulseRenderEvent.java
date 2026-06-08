package jp.aquafactory.apprenticecodex.spell.heavenlyfist;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class HeavenlyFistPulseRenderEvent {
    private static final ResourceLocation WAVE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/force_field_wave.png");
    private static final RenderType WAVE_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("heavenly_fist_pulse_additive", WAVE_TEXTURE);
    private static final int MAX_ACTIVE_PULSES = 32;
    private static final float LIFETIME_TICKS = 14.0F;
    private static final float MIN_RADIUS = 0.35F;
    private static final float WAVE_HEIGHT = 0.04F;
    private static final float COLOR_RED = 0.72F;
    private static final float COLOR_GREEN = 0.26F;
    private static final float COLOR_BLUE = 1.0F;
    private static final Deque<ActivePulse> ACTIVE_PULSES = new ArrayDeque<>();

    private HeavenlyFistPulseRenderEvent() {
    }

    public static void enqueuePulse(Vec3 center, float maxRadius) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        ACTIVE_PULSES.addLast(new ActivePulse(center, minecraft.level.getGameTime(), Math.max(MIN_RADIUS, maxRadius)));
        while (ACTIVE_PULSES.size() > MAX_ACTIVE_PULSES) {
            ACTIVE_PULSES.removeFirst();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null && !ACTIVE_PULSES.isEmpty()) {
            ACTIVE_PULSES.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE_PULSES.isEmpty()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            ACTIVE_PULSES.clear();
            return;
        }

        var cameraPosition = event.getCamera().getPosition();
        var gameTime = level.getGameTime();
        var partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        var poseStack = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        var iterator = ACTIVE_PULSES.iterator();
        while (iterator.hasNext()) {
            var pulse = iterator.next();
            var age = (float) (gameTime - pulse.startGameTime()) + partialTick;
            if (age >= LIFETIME_TICKS) {
                iterator.remove();
                continue;
            }

            renderPulse(poseStack, buffers.getBuffer(WAVE_RENDER_TYPE), pulse.center(), pulse.maxRadius(), age);
        }

        poseStack.popPose();
        buffers.endBatch(WAVE_RENDER_TYPE);
    }

    private static void renderPulse(PoseStack poseStack, VertexConsumer buffer, Vec3 center, float maxRadius, float age) {
        var progress = Mth.clamp(age / LIFETIME_TICKS, 0.0F, 1.0F);
        var eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        var radius = Mth.lerp(eased, MIN_RADIUS, maxRadius);
        var alpha = 0.75F * (1.0F - progress);
        if (alpha <= 0.0F) {
            return;
        }

        var pose = poseStack.last();
        var poseMatrix = pose.pose();
        var normalMatrix = pose.normal();
        var y = (float) center.y + WAVE_HEIGHT;

        var p0 = new Vec3(center.x - radius, y, center.z - radius);
        var p1 = new Vec3(center.x - radius, y, center.z + radius);
        var p2 = new Vec3(center.x + radius, y, center.z + radius);
        var p3 = new Vec3(center.x + radius, y, center.z - radius);
        var up = new Vec3(0.0D, 1.0D, 0.0D);
        var down = new Vec3(0.0D, -1.0D, 0.0D);

        addVertex(buffer, poseMatrix, normalMatrix, p0, 0.0F, 1.0F, up, alpha);
        addVertex(buffer, poseMatrix, normalMatrix, p1, 0.0F, 0.0F, up, alpha);
        addVertex(buffer, poseMatrix, normalMatrix, p2, 1.0F, 0.0F, up, alpha);
        addVertex(buffer, poseMatrix, normalMatrix, p3, 1.0F, 1.0F, up, alpha);

        addVertex(buffer, poseMatrix, normalMatrix, p3, 1.0F, 1.0F, down, alpha);
        addVertex(buffer, poseMatrix, normalMatrix, p2, 1.0F, 0.0F, down, alpha);
        addVertex(buffer, poseMatrix, normalMatrix, p1, 0.0F, 0.0F, down, alpha);
        addVertex(buffer, poseMatrix, normalMatrix, p0, 0.0F, 1.0F, down, alpha);
    }

    private static void addVertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                                  Vec3 position, float u, float v, Vec3 normal, float alpha) {
        var transformedNormal = normalMatrix.transform(new Vector3f((float) normal.x, (float) normal.y, (float) normal.z));
        buffer.addVertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .setColor(COLOR_RED * alpha, COLOR_GREEN * alpha, COLOR_BLUE * alpha, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
    }

    private record ActivePulse(Vec3 center, long startGameTime, float maxRadius) {
    }
}
