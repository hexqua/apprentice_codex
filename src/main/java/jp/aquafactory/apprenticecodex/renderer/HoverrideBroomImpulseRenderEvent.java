package jp.aquafactory.apprenticecodex.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class HoverrideBroomImpulseRenderEvent {
    private static final ResourceLocation WAVE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/force_field_wave.png");
    private static final RenderType WAVE_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("hoverride_broom_impulse_additive", WAVE_TEXTURE);
    private static final int MAX_ACTIVE_PULSES = 32;
    private static final int SPARK_COUNT = 12;
    private static final float LIFETIME_TICKS = 8.0F;
    private static final float MIN_RADIUS = 0.2F;
    private static final float MAX_RADIUS = 1.0F;
    private static final Deque<ActivePulse> ACTIVE_PULSES = new ArrayDeque<>();

    private HoverrideBroomImpulseRenderEvent() {
    }

    public static void enqueue(Vec3 center, Vec3 direction) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        var normal = horizontalDirection(direction);
        ACTIVE_PULSES.addLast(new ActivePulse(center, normal, minecraft.level, minecraft.level.getGameTime()));
        while (ACTIVE_PULSES.size() > MAX_ACTIVE_PULSES) {
            ACTIVE_PULSES.removeFirst();
        }
        spawnSparks(minecraft.level, center, normal);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getInstance().level == null) {
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
            if (pulse.level() != level) {
                iterator.remove();
                continue;
            }
            var age = (float)(gameTime - pulse.startGameTime()) + event.getPartialTick();
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
        var radius = Mth.lerp(eased, MIN_RADIUS, MAX_RADIUS);
        var alpha = 0.85F * (1.0F - progress);
        var normal = pulse.normal();
        var vertical = new Vec3(0.0D, 1.0D, 0.0D);
        var horizontal = normal.cross(vertical).normalize();
        var center = pulse.center();
        var p0 = center.add(horizontal.scale(-radius)).add(vertical.scale(-radius));
        var p1 = center.add(horizontal.scale(-radius)).add(vertical.scale(radius));
        var p2 = center.add(horizontal.scale(radius)).add(vertical.scale(radius));
        var p3 = center.add(horizontal.scale(radius)).add(vertical.scale(-radius));
        addQuad(poseStack, buffer, p0, p1, p2, p3, normal, alpha);
        addQuad(poseStack, buffer, p3, p2, p1, p0, normal.reverse(), alpha);
    }

    private static void spawnSparks(ClientLevel level, Vec3 center, Vec3 direction) {
        var random = level.random;
        var backward = direction.scale(-1.0D);
        var tangent = new Vec3(-direction.z, 0.0D, direction.x);
        for (var i = 0; i < SPARK_COUNT; ++i) {
            var t = random.nextFloat();
            var color = new Vector3f(
                    Mth.lerp(t, 0.28F, 0.62F),
                    Mth.lerp(t, 0.78F, 0.36F),
                    Mth.lerp(t, 1.0F, 0.95F)
            );
            var velocity = backward.scale(0.06D + random.nextDouble() * 0.06D)
                    .add(tangent.scale((random.nextDouble() - 0.5D) * 0.12D))
                    .add(0.0D, (random.nextDouble() - 0.5D) * 0.12D, 0.0D);
            level.addParticle(
                    new AdditiveGlowParticleOptions(
                            ParticleRegistry.ADDITIVE_SPARK.get(),
                            0.16F + random.nextFloat() * 0.08F,
                            color.x(), color.y(), color.z(),
                            1,
                            8,
                            3,
                            0.75F,
                            1.25F,
                            0.65F,
                            1.0F,
                            0.02F,
                            0.6F,
                            0.35F,
                            true
                    ),
                    center.x, center.y, center.z,
                    velocity.x, velocity.y, velocity.z
            );
        }
    }

    private static Vec3 horizontalDirection(Vec3 direction) {
        var horizontal = new Vec3(direction.x, 0.0D, direction.z);
        return horizontal.lengthSqr() > 1.0e-8D ? horizontal.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
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
        var transformedNormal = normalMatrix.transform(
                new Vector3f((float)normal.x, (float)normal.y, (float)normal.z)
        );
        buffer.vertex(poseMatrix, (float)position.x, (float)position.y, (float)position.z)
                .color(0.55F * alpha, 0.95F * alpha, alpha, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z())
                .endVertex();
    }

    private record ActivePulse(Vec3 center, Vec3 normal, ClientLevel level, long startGameTime) {
    }
}
