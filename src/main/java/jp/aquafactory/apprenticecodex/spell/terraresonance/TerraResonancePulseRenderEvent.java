package jp.aquafactory.apprenticecodex.spell.terraresonance;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
public final class TerraResonancePulseRenderEvent {
    private static final ResourceLocation WAVE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/force_field_wave.png");
    private static final RenderType WAVE_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("terra_resonance_pulse_additive", WAVE_TEXTURE);
    private static final int MAX_ACTIVE_PULSES = 16;
    private static final float LIFETIME_TICKS = 14.0F;
    private static final float MIN_RADIUS = 0.2F;
    private static final float SURFACE_OFFSET = 0.004F;
    private static final float COLOR_RED = 0.22F;
    private static final float COLOR_GREEN = 1.0F;
    private static final float COLOR_BLUE = 0.38F;
    private static final Deque<ActivePulse> ACTIVE_PULSES = new ArrayDeque<>();

    private TerraResonancePulseRenderEvent() {
    }

    public static void enqueuePulse(Vec3 center, Direction selectedFace, float maxRadius) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        ACTIVE_PULSES.addLast(new ActivePulse(
                center,
                selectedFace,
                minecraft.level.getGameTime(),
                Math.max(MIN_RADIUS, maxRadius)
        ));
        while (ACTIVE_PULSES.size() > MAX_ACTIVE_PULSES) {
            ACTIVE_PULSES.removeFirst();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().level == null) {
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

        var poseStack = event.getPoseStack();
        var cameraPosition = event.getCamera().getPosition();
        var buffers = minecraft.renderBuffers().bufferSource();
        var gameTime = level.getGameTime();

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        var iterator = ACTIVE_PULSES.iterator();
        while (iterator.hasNext()) {
            var pulse = iterator.next();
            var age = (float) (gameTime - pulse.startGameTime()) + event.getPartialTick();
            if (age >= LIFETIME_TICKS) {
                iterator.remove();
                continue;
            }
            renderPulse(poseStack, buffers.getBuffer(WAVE_RENDER_TYPE), pulse, age);
        }
        poseStack.popPose();
        buffers.endBatch(WAVE_RENDER_TYPE);
    }

    private static void renderPulse(PoseStack poseStack, VertexConsumer buffer, ActivePulse pulse, float age) {
        var progress = Mth.clamp(age / LIFETIME_TICKS, 0.0F, 1.0F);
        var eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        var radius = Mth.lerp(eased, MIN_RADIUS, pulse.maxRadius());
        var alpha = 0.8F * (1.0F - progress);
        var normal = Vec3.atLowerCornerOf(pulse.selectedFace().getNormal());
        var tangentA = tangentA(pulse.selectedFace());
        var tangentB = normal.cross(tangentA).normalize();
        var center = pulse.center().add(normal.scale(SURFACE_OFFSET));

        var p0 = center.add(tangentA.scale(-radius)).add(tangentB.scale(-radius));
        var p1 = center.add(tangentA.scale(-radius)).add(tangentB.scale(radius));
        var p2 = center.add(tangentA.scale(radius)).add(tangentB.scale(radius));
        var p3 = center.add(tangentA.scale(radius)).add(tangentB.scale(-radius));
        addQuad(poseStack, buffer, p0, p1, p2, p3, normal, alpha);
        addQuad(poseStack, buffer, p3, p2, p1, p0, normal.reverse(), alpha);
    }

    private static Vec3 tangentA(Direction face) {
        return face.getAxis() == Direction.Axis.X
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : new Vec3(1.0D, 0.0D, 0.0D);
    }

    private static void addQuad(PoseStack poseStack, VertexConsumer buffer,
                                Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 normal, float alpha) {
        var pose = poseStack.last();
        vertex(buffer, pose.pose(), pose.normal(), p0, 0.0F, 1.0F, normal, alpha);
        vertex(buffer, pose.pose(), pose.normal(), p1, 0.0F, 0.0F, normal, alpha);
        vertex(buffer, pose.pose(), pose.normal(), p2, 1.0F, 0.0F, normal, alpha);
        vertex(buffer, pose.pose(), pose.normal(), p3, 1.0F, 1.0F, normal, alpha);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                               Vec3 position, float u, float v, Vec3 normal, float alpha) {
        buffer.vertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .color(COLOR_RED * alpha, COLOR_GREEN * alpha, COLOR_BLUE * alpha, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normalMatrix, (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }

    private record ActivePulse(Vec3 center, Direction selectedFace, long startGameTime, float maxRadius) {
    }
}
