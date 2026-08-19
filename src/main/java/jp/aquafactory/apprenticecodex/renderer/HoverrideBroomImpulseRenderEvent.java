package jp.aquafactory.apprenticecodex.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
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
public final class HoverrideBroomImpulseRenderEvent {
    private static final ResourceLocation WAVE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/force_field_wave.png");
    private static final RenderType WAVE_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("hoverride_broom_impulse_additive", WAVE_TEXTURE);
    private static final int MAX_ACTIVE_PULSES = 32;
    private static final int SPARK_COUNT = 12;
    private static final int RUSH_PULSE_INTERVAL_TICKS = 4;
    private static final double RUSH_EFFECT_MAX_DISTANCE_SQR = 64.0D * 64.0D;
    private static final Deque<ActivePulse> ACTIVE_PULSES = new ArrayDeque<>();
    private static ClientLevel lastRushEffectLevel;
    private static long lastRushEffectGameTime = Long.MIN_VALUE;

    private HoverrideBroomImpulseRenderEvent() {
    }

    public static void enqueue(Vec3 center, Vec3 direction) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        var normal = horizontalDirection(direction);
        addPulse(new ActivePulse(
                center, normal, minecraft.level, minecraft.level.getGameTime(), PulseStyle.IMPULSE));
        spawnSparks(minecraft.level, center, normal);
    }

    private static void enqueueRushPulse(ClientLevel level, Vec3 center, Vec3 direction) {
        addPulse(new ActivePulse(center, horizontalDirection(direction), level, level.getGameTime(), PulseStyle.RUSH));
    }

    private static void addPulse(ActivePulse pulse) {
        ACTIVE_PULSES.addLast(pulse);
        while (ACTIVE_PULSES.size() > MAX_ACTIVE_PULSES) {
            ACTIVE_PULSES.removeFirst();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            ACTIVE_PULSES.clear();
            lastRushEffectLevel = null;
            lastRushEffectGameTime = Long.MIN_VALUE;
            return;
        }

        var gameTime = level.getGameTime();
        // pause中に同じworld tickの粒子を重複生成しない。
        if (lastRushEffectLevel == level && lastRushEffectGameTime == gameTime) {
            return;
        }
        lastRushEffectLevel = level;
        lastRushEffectGameTime = gameTime;

        var viewer = minecraft.getCameraEntity();
        for (var entity : level.entitiesForRendering()) {
            if (!(entity instanceof HoverrideBroomEntity broom) || !broom.isRushAttackActive()
                    || viewer != null && viewer.distanceToSqr(broom) > RUSH_EFFECT_MAX_DISTANCE_SQR) {
                continue;
            }
            var direction = horizontalDirection(broom.getRushAttackDirection());
            spawnRushMantle(level, broom, direction);
            if (Math.floorMod(gameTime + broom.getId(), RUSH_PULSE_INTERVAL_TICKS) == 0) {
                var center = broom.position().add(0.0D, broom.getBbHeight() * 0.5D, 0.0D);
                enqueueRushPulse(level, center, direction);
            }
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
            var age = (float)(gameTime - pulse.startGameTime())
                    + event.getPartialTick().getGameTimeDeltaPartialTick(true);
            if (age >= pulse.style().lifetimeTicks()) {
                iterator.remove();
                continue;
            }
            renderPulse(poseStack, buffers.getBuffer(WAVE_RENDER_TYPE), pulse, age);
        }
        poseStack.popPose();
        buffers.endBatch(WAVE_RENDER_TYPE);
    }

    private static void renderPulse(PoseStack poseStack, VertexConsumer buffer, ActivePulse pulse, float age) {
        var style = pulse.style();
        var progress = Mth.clamp(age / style.lifetimeTicks(), 0.0F, 1.0F);
        var eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        var radius = Mth.lerp(eased, style.minimumRadius(), style.maximumRadius());
        var alpha = style.alpha() * (1.0F - progress);
        var normal = pulse.normal();
        var vertical = new Vec3(0.0D, 1.0D, 0.0D);
        var horizontal = normal.cross(vertical).normalize();
        var center = pulse.center();
        var p0 = center.add(horizontal.scale(-radius)).add(vertical.scale(-radius));
        var p1 = center.add(horizontal.scale(-radius)).add(vertical.scale(radius));
        var p2 = center.add(horizontal.scale(radius)).add(vertical.scale(radius));
        var p3 = center.add(horizontal.scale(radius)).add(vertical.scale(-radius));
        addQuad(poseStack, buffer, p0, p1, p2, p3, normal, style, alpha);
        addQuad(poseStack, buffer, p3, p2, p1, p0, normal.reverse(), style, alpha);
    }

    private static void spawnRushMantle(ClientLevel level, HoverrideBroomEntity broom, Vec3 direction) {
        var random = level.random;
        var tangent = new Vec3(-direction.z, 0.0D, direction.x);
        var vertical = new Vec3(0.0D, 1.0D, 0.0D);
        var center = broom.position().add(0.0D, broom.getBbHeight() * 0.5D, 0.0D);
        for (var index = 0; index < 2; ++index) {
            var angle = random.nextDouble() * Math.PI * 2.0D;
            var radius = 0.35D + random.nextDouble() * 0.2D;
            var radial = tangent.scale(Math.cos(angle)).add(vertical.scale(Math.sin(angle)));
            var position = center
                    .add(direction.scale(random.nextDouble() * 1.8D - 0.9D))
                    .add(radial.scale(radius));
            var velocity = direction.scale(-0.04D - random.nextDouble() * 0.05D)
                    .add(radial.scale(0.01D + random.nextDouble() * 0.02D));
            var t = random.nextFloat();
            var color = new Vector3f(
                    Mth.lerp(t, 0.28F, 0.62F),
                    Mth.lerp(t, 0.78F, 0.36F),
                    Mth.lerp(t, 1.0F, 0.95F)
            );
            var particleType = index == 0
                    ? ParticleRegistry.ADDITIVE_SPARK.get()
                    : ParticleRegistry.ADDITIVE_RHOMBUS.get();
            level.addParticle(
                    new AdditiveGlowParticleOptions(
                            particleType,
                            0.13F + random.nextFloat() * 0.08F,
                            color.x(), color.y(), color.z(),
                            1,
                            7,
                            3,
                            0.7F,
                            1.25F,
                            0.55F,
                            0.95F,
                            0.02F,
                            0.58F,
                            0.35F,
                            true
                    ),
                    position.x, position.y, position.z,
                    velocity.x, velocity.y, velocity.z
            );
        }
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
                                Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 normal,
                                PulseStyle style, float alpha) {
        var pose = poseStack.last();
        vertex(buffer, pose.pose(), pose.normal(), p0, 0.0F, 1.0F, normal, style, alpha);
        vertex(buffer, pose.pose(), pose.normal(), p1, 0.0F, 0.0F, normal, style, alpha);
        vertex(buffer, pose.pose(), pose.normal(), p2, 1.0F, 0.0F, normal, style, alpha);
        vertex(buffer, pose.pose(), pose.normal(), p3, 1.0F, 1.0F, normal, style, alpha);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                               Vec3 position, float u, float v, Vec3 normal, PulseStyle style, float alpha) {
        var transformedNormal = normalMatrix.transform(
                new Vector3f((float)normal.x, (float)normal.y, (float)normal.z)
        );
        buffer.addVertex(poseMatrix, (float)position.x, (float)position.y, (float)position.z)
                .setColor(style.red() * alpha, style.green() * alpha, style.blue() * alpha, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
    }

    private enum PulseStyle {
        IMPULSE(8.0F, 0.2F, 1.0F, 0.85F, 0.55F, 0.95F, 1.0F),
        RUSH(6.0F, 0.25F, 0.85F, 0.62F, 0.48F, 0.72F, 1.0F);

        private final float lifetimeTicks;
        private final float minimumRadius;
        private final float maximumRadius;
        private final float alpha;
        private final float red;
        private final float green;
        private final float blue;

        PulseStyle(float lifetimeTicks, float minimumRadius, float maximumRadius, float alpha,
                   float red, float green, float blue) {
            this.lifetimeTicks = lifetimeTicks;
            this.minimumRadius = minimumRadius;
            this.maximumRadius = maximumRadius;
            this.alpha = alpha;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        private float lifetimeTicks() {
            return lifetimeTicks;
        }

        private float minimumRadius() {
            return minimumRadius;
        }

        private float maximumRadius() {
            return maximumRadius;
        }

        private float alpha() {
            return alpha;
        }

        private float red() {
            return red;
        }

        private float green() {
            return green;
        }

        private float blue() {
            return blue;
        }
    }

    private record ActivePulse(
            Vec3 center,
            Vec3 normal,
            ClientLevel level,
            long startGameTime,
            PulseStyle style
    ) {
    }
}
