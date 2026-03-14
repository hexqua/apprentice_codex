package jp.aquafactory.apprenticecodex.item.crystalbladedstaff;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.packet.ManaSiphonOrbEffectPacket;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ManaSiphonOrbRenderEvent {
    private static final ResourceLocation ORB_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/mana_siphon_orb.png");
    private static final net.minecraft.client.renderer.RenderType ORB_RENDER_TYPE =
            ApprenticeRenderTypes.additiveEntityNoCull("mana_siphon_orb_additive", ORB_TEXTURE);
    private static final int MAX_ACTIVE_ORBS = 192;
    private static final float SCATTER_DURATION_TICKS = 6.0f;
    private static final double SCATTER_GRAVITY = 0.012d;
    private static final int MAX_TRAIL_POINTS = 12;
    private static final float TRAIL_SAMPLE_INTERVAL_TICKS = 0.35f;
    private static final float BASE_RED = 0.45f;
    private static final float BASE_GREEN = 0.95f;
    private static final float BASE_BLUE = 1.0f;
    private static final List<ActiveOrb> ACTIVE_ORBS = new ArrayList<>();

    private ManaSiphonOrbRenderEvent() {
    }

    public static void enqueueEffect(Vec3 impactPosition, int ownerEntityId, List<ManaSiphonOrbEffectPacket.OrbData> orbs) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || orbs.isEmpty()) {
            return;
        }

        var startGameTime = minecraft.level.getGameTime();
        for (var orb : orbs) {
            ACTIVE_ORBS.add(new ActiveOrb(
                    impactPosition,
                    ownerEntityId,
                    startGameTime,
                    orb,
                    new ArrayDeque<>(),
                    Vec3.ZERO,
                    Float.NEGATIVE_INFINITY
            ));
        }

        if (ACTIVE_ORBS.size() > MAX_ACTIVE_ORBS) {
            ACTIVE_ORBS.subList(0, ACTIVE_ORBS.size() - MAX_ACTIVE_ORBS).clear();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().level == null && !ACTIVE_ORBS.isEmpty()) {
            ACTIVE_ORBS.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE_ORBS.isEmpty()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            ACTIVE_ORBS.clear();
            return;
        }

        var poseStack = event.getPoseStack();
        var camera = event.getCamera();
        var partialTick = event.getPartialTick();
        var gameTime = level.getGameTime();
        var bufferSource = minecraft.renderBuffers().bufferSource();
        var buffer = bufferSource.getBuffer(ORB_RENDER_TYPE);
        var cameraPosition = camera.getPosition();
        var cameraRotation = new Quaternionf(camera.rotation());

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        var iterator = ACTIVE_ORBS.iterator();
        while (iterator.hasNext()) {
            var orb = iterator.next();
            var age = (float) (gameTime - orb.startGameTime) + partialTick;
            if (age >= orb.totalLifetimeTicks()) {
                iterator.remove();
                continue;
            }

            var owner = level.getEntity(orb.ownerEntityId);
            var currentPosition = currentPosition(orb, owner, age);
            updateTrail(orb, currentPosition, age);
            renderTrail(poseStack, buffer, orb, cameraRotation);
            renderOrb(poseStack, buffer, orb, age, currentPosition, cameraRotation);
        }

        poseStack.popPose();
        bufferSource.endBatch(ORB_RENDER_TYPE);
    }

    private static Vec3 currentPosition(ActiveOrb orb, Entity owner, float age) {
        var returnStartTicks = orb.orbData.returnDelayTicks();
        if (age < SCATTER_DURATION_TICKS) {
            return scatterPosition(orb, age);
        }

        if (age < returnStartTicks) {
            var anchor = scatterPosition(orb, SCATTER_DURATION_TICKS);
            var hoverTicks = age - SCATTER_DURATION_TICKS;
            var phase = hoverTicks * 0.14f + orb.orbData.phaseOffset();
            return anchor.add(
                    Math.cos(phase * 0.9f) * 0.05d,
                    Math.sin(phase * 1.3f) * 0.03d,
                    Math.sin(phase) * 0.05d
            );
        }

        if (owner == null) {
            return orb.lastPosition;
        }

        var anchor = currentPosition(orb, null, returnStartTicks - 0.001f);
        var target = ownerTargetPosition(owner);
        var progress = Mth.clamp((age - returnStartTicks) / orb.orbData.returnDurationTicks(), 0.0f, 1.0f);
        var eased = easeOutQuint(progress);
        var travel = target.subtract(anchor);
        var axis = horizontalPerpendicular(travel);
        var spiral = axis.scale((1.0f - eased) * 0.08d * Math.sin(progress * Math.PI * 3.0 + orb.orbData.phaseOffset()));
        return anchor.lerp(target, eased).add(spiral);
    }

    private static Vec3 scatterPosition(ActiveOrb orb, float age) {
        var scatter = new Vec3(orb.orbData.scatterX(), orb.orbData.scatterY(), orb.orbData.scatterZ());
        return orb.impactPosition.add(
                scatter.scale(age)
        ).add(0.0d, -SCATTER_GRAVITY * age * age, 0.0d);
    }

    private static Vec3 ownerTargetPosition(Entity owner) {
        return owner.position().add(0.0d, owner.getBbHeight() * 0.55d, 0.0d);
    }

    private static void updateTrail(ActiveOrb orb, Vec3 currentPosition, float age) {
        orb.lastPosition = currentPosition;
        if (age < orb.orbData.returnDelayTicks()) {
            orb.trailPoints.clear();
            orb.lastTrailSampleAge = age;
            return;
        }

        if (!Float.isFinite(orb.lastTrailSampleAge) || age - orb.lastTrailSampleAge >= TRAIL_SAMPLE_INTERVAL_TICKS) {
            orb.trailPoints.addLast(currentPosition);
            orb.lastTrailSampleAge = age;
        } else if (!orb.trailPoints.isEmpty()) {
            orb.trailPoints.removeLast();
            orb.trailPoints.addLast(currentPosition);
        } else {
            orb.trailPoints.addLast(currentPosition);
        }

        while (orb.trailPoints.size() > MAX_TRAIL_POINTS) {
            orb.trailPoints.removeFirst();
        }
    }

    private static void renderTrail(PoseStack poseStack, VertexConsumer buffer, ActiveOrb orb, Quaternionf cameraRotation) {
        if (orb.trailPoints.isEmpty()) {
            return;
        }

        var i = 0;
        var trailSize = orb.trailPoints.size();
        for (var point : orb.trailPoints) {
            var ageRatio = trailSize == 1 ? 1.0f : (float) i / (trailSize - 1);
            var alpha = 0.1f + ageRatio * 0.22f;
            var scale = orb.orbData.scale() * (0.42f + ageRatio * 0.52f);
            renderBillboard(poseStack, buffer, point, scale, alpha, ageRatio * 0.8f, 0.65f, cameraRotation);
            i++;
        }
    }

    private static void renderOrb(PoseStack poseStack, VertexConsumer buffer, ActiveOrb orb, float age, Vec3 position,
                                  Quaternionf cameraRotation) {
        var pulse = 0.9f + 0.1f * (float) Math.sin(age * 0.35f + orb.orbData.phaseOffset());
        var baseScale = orb.orbData.scale() * pulse;
        var fade = getAlpha(orb, age);
        if (fade <= 0.0f) {
            return;
        }

        renderBillboard(poseStack, buffer, position, baseScale, fade, age * 0.05f, 1.0f, cameraRotation);
        renderBillboard(poseStack, buffer,
                position.add(
                        Math.cos(age * 0.11f + orb.orbData.phaseOffset()) * 0.015d,
                        Math.sin(age * 0.13f + orb.orbData.phaseOffset()) * 0.01d,
                        Math.sin(age * 0.09f + orb.orbData.phaseOffset()) * 0.015d
                ),
                baseScale * 0.72f, fade * 0.9f, -age * 0.09f + orb.orbData.phaseOffset(), 0.82f, cameraRotation);
        renderBillboard(poseStack, buffer,
                position.add(
                        Math.sin(age * 0.07f + orb.orbData.phaseOffset()) * 0.012d,
                        Math.cos(age * 0.1f + orb.orbData.phaseOffset()) * 0.012d,
                        Math.cos(age * 0.12f + orb.orbData.phaseOffset()) * 0.012d
                ),
                baseScale * 0.48f, fade * 0.75f, age * 0.13f + orb.orbData.phaseOffset() * 0.5f, 0.68f, cameraRotation);
    }

    private static float getAlpha(ActiveOrb orb, float age) {
        var totalLifetime = orb.totalLifetimeTicks();
        var remaining = totalLifetime - age;
        if (remaining <= 2.0f) {
            return Mth.clamp(remaining / 2.0f, 0.0f, 1.0f) * 0.9f;
        }

        if (age < SCATTER_DURATION_TICKS) {
            return 0.65f + 0.25f * (age / SCATTER_DURATION_TICKS);
        }

        return 0.9f;
    }

    private static void renderBillboard(PoseStack poseStack, VertexConsumer buffer, Vec3 position,
                                        float scale, float alpha, float rotation, float tintStrength,
                                        Quaternionf cameraRotation) {
        var rotated = new Quaternionf(cameraRotation).rotateZ(rotation);
        var right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(rotated).mul(scale * 0.5f);
        var up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(rotated).mul(scale * 0.5f);
        var normalVector = new Vector3f(0.0f, 0.0f, 1.0f).rotate(rotated);
        var p0 = position.subtract(right.x + up.x, right.y + up.y, right.z + up.z);
        var p1 = position.add(-right.x + up.x, -right.y + up.y, -right.z + up.z);
        var p2 = position.add(right.x + up.x, right.y + up.y, right.z + up.z);
        var p3 = position.add(right.x - up.x, right.y - up.y, right.z - up.z);
        var red = Mth.clamp(BASE_RED + 0.2f * tintStrength, 0.0f, 1.0f) * alpha;
        var green = Mth.clamp(BASE_GREEN + 0.05f * tintStrength, 0.0f, 1.0f) * alpha;
        var blue = Mth.clamp(BASE_BLUE, 0.0f, 1.0f) * alpha;
        addDoubleSidedQuad(
                poseStack,
                buffer,
                p0,
                p1,
                p2,
                p3,
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
                red,
                green,
                blue,
                alpha,
                new Vec3(normalVector.x(), normalVector.y(), normalVector.z())
        );
    }

    private static void addDoubleSidedQuad(PoseStack poseStack, VertexConsumer buffer,
                                           Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                           float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3,
                                           float red, float green, float blue, float alpha, Vec3 normal) {
        addQuad(poseStack, buffer, p0, p1, p2, p3, u0, v0, u1, v1, u2, v2, u3, v3, red, green, blue, alpha, normal);
        addQuad(poseStack, buffer, p3, p2, p1, p0, u3, v3, u2, v2, u1, v1, u0, v0, red, green, blue, alpha, normal.reverse());
    }

    private static void addQuad(PoseStack poseStack, VertexConsumer buffer,
                                Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3,
                                float red, float green, float blue, float alpha, Vec3 normal) {
        var pose = poseStack.last();
        var poseMatrix = pose.pose();
        var normalMatrix = pose.normal();
        vertex(buffer, poseMatrix, normalMatrix, p0, u0, v0, red, green, blue, alpha, normal);
        vertex(buffer, poseMatrix, normalMatrix, p1, u1, v1, red, green, blue, alpha, normal);
        vertex(buffer, poseMatrix, normalMatrix, p2, u2, v2, red, green, blue, alpha, normal);
        vertex(buffer, poseMatrix, normalMatrix, p3, u3, v3, red, green, blue, alpha, normal);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                               Vec3 position, float u, float v,
                               float red, float green, float blue, float alpha, Vec3 normal) {
        buffer.vertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normalMatrix, (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }

    private static Vec3 horizontalPerpendicular(Vec3 vector) {
        var horizontal = new Vec3(-vector.z, 0.0d, vector.x);
        if (horizontal.lengthSqr() <= 1.0e-4) {
            return new Vec3(1.0d, 0.0d, 0.0d);
        }
        return horizontal.normalize();
    }

    private static float easeOutQuint(float value) {
        var clamped = Mth.clamp(value, 0.0f, 1.0f);
        return 1.0f - (float) Math.pow(1.0f - clamped, 5.0);
    }

    private static final class ActiveOrb {
        private final Vec3 impactPosition;
        private final int ownerEntityId;
        private final long startGameTime;
        private final ManaSiphonOrbEffectPacket.OrbData orbData;
        private final Deque<Vec3> trailPoints;
        private Vec3 lastPosition;
        private float lastTrailSampleAge;

        private ActiveOrb(Vec3 impactPosition, int ownerEntityId, long startGameTime,
                          ManaSiphonOrbEffectPacket.OrbData orbData, Deque<Vec3> trailPoints,
                          Vec3 lastPosition, float lastTrailSampleAge) {
            this.impactPosition = impactPosition;
            this.ownerEntityId = ownerEntityId;
            this.startGameTime = startGameTime;
            this.orbData = orbData;
            this.trailPoints = trailPoints;
            this.lastPosition = lastPosition;
            this.lastTrailSampleAge = lastTrailSampleAge;
        }

        private float totalLifetimeTicks() {
            return orbData.returnDelayTicks() + orbData.returnDurationTicks() + 2.0f;
        }
    }
}
