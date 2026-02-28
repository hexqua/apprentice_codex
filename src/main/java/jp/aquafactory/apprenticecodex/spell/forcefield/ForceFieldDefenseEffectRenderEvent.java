package jp.aquafactory.apprenticecodex.spell.forcefield;

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
import net.minecraft.world.phys.Vec2;
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

// 引数として明示しないとあとから見た時に数値の意味がわからなくなるため警告は抑止.
@SuppressWarnings("SameParameterValue")
@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ForceFieldDefenseEffectRenderEvent {
    private static final ResourceLocation SHIELD_OVERLAY_TEXTURE = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/force_field_wall.png");
    private static final ResourceLocation SHIELD_TRIM_TEXTURE = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/force_field_wall_trim.png");
    private static final ResourceLocation SHOCKWAVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/force_field_wave.png");
    private static final RenderType SHIELD_OVERLAY_RENDER_TYPE = createAdditiveEntityRenderType("force_field_shield_overlay_additive", SHIELD_OVERLAY_TEXTURE);
    private static final RenderType SHIELD_TRIM_RENDER_TYPE = createAdditiveEntityRenderType("force_field_shield_trim_additive", SHIELD_TRIM_TEXTURE);
    private static final RenderType RIPPLE_RENDER_TYPE = createAdditiveEntityRenderType("force_field_ripple_additive", SHOCKWAVE_TEXTURE);
    private static final int MAX_ACTIVE_EFFECTS = 128;
    private static final float MIN_EFFECT_SCALE = 0.1f;
    private static final int HOLD_TICKS = 10;
    private static final int FADE_TICKS = 10;
    private static final int TOTAL_TICKS = HOLD_TICKS + FADE_TICKS;
    private static final float SHIELD_ALPHA = 0.95f;
    private static final float WALL_THICKNESS = 0.1f;
    private static final float OUTER_RADIUS = 0.25f;
    private static final float INNER_RADIUS = 0.205f;
    private static final float RIPPLE_LIFETIME_TICKS = 8.0f;
    private static final float RIPPLE_MIN_RADIUS = 0.10f;
    private static final float RIPPLE_MAX_RADIUS = 0.64f;
    private static final float RIPPLE_SURFACE_OFFSET = 0.0015f;
    private static final float SHIELD_TINT = 0.65f;
    private static final Vec2[] OUTER_HEX_VERTICES = buildHexVertices(OUTER_RADIUS);
    private static final Vec2[] INNER_HEX_VERTICES = buildHexVertices(INNER_RADIUS);
    private static final Deque<ActiveEffect> ACTIVE_EFFECTS = new ArrayDeque<>();

    private ForceFieldDefenseEffectRenderEvent() {
    }

    public static void enqueueEffect(Vec3 position, Vec3 normal) {
        enqueueEffect(position, normal, 1.0f, 1.0f, true);
    }

    public static void enqueueEffect(Vec3 position, Vec3 normal, float sizeScale, float lifetimeScale) {
        enqueueEffect(position, normal, sizeScale, lifetimeScale, true);
    }

    public static void enqueueEffect(Vec3 position, Vec3 normal, float sizeScale, float lifetimeScale, boolean renderWave) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        ACTIVE_EFFECTS.addLast(new ActiveEffect(
                position,
                normalizeOrFallback(normal, new Vec3(0, 0, 1)),
                minecraft.level.getGameTime(),
                sanitizeScale(sizeScale),
                sanitizeScale(lifetimeScale),
                renderWave
        ));
        while (ACTIVE_EFFECTS.size() > MAX_ACTIVE_EFFECTS) {
            ACTIVE_EFFECTS.removeFirst();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().level == null && !ACTIVE_EFFECTS.isEmpty()) {
            ACTIVE_EFFECTS.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE_EFFECTS.isEmpty()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            ACTIVE_EFFECTS.clear();
            return;
        }

        var gameTime = level.getGameTime();
        var partialTick = event.getPartialTick();
        var poseStack = event.getPoseStack();
        var cameraPosition = event.getCamera().getPosition();
        var buffers = minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        var iterator = ACTIVE_EFFECTS.iterator();
        while (iterator.hasNext()) {
            var effect = iterator.next();
            var age = (float) (gameTime - effect.startGameTime()) + partialTick;
            var normalizedAge = age / effect.lifetimeScale();
            if (normalizedAge >= TOTAL_TICKS) {
                iterator.remove();
                continue;
            }

            renderEffect(poseStack, buffers, effect, normalizedAge);
        }

        poseStack.popPose();

        buffers.endBatch(SHIELD_OVERLAY_RENDER_TYPE);
        buffers.endBatch(SHIELD_TRIM_RENDER_TYPE);
        buffers.endBatch(RIPPLE_RENDER_TYPE);
    }

    private static void renderEffect(PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers,
                                     ActiveEffect effect, float age) {
        var alpha = getWallAlpha(age);
        if (alpha <= 0.0f) {
            return;
        }

        var center = effect.position();
        var normal = normalizeOrFallback(effect.normal(), new Vec3(0, 0, 1));
        var tangent = buildTangent(normal);
        var bitangent = normalizeOrFallback(normal.cross(tangent), new Vec3(0, 1, 0));
        var effectSize = effect.sizeScale();
        var halfThickness = WALL_THICKNESS * 0.5f * effectSize;

        var overlayBuffer = buffers.getBuffer(SHIELD_OVERLAY_RENDER_TYPE);
        drawFilledHex(poseStack, overlayBuffer, center, normal, tangent, bitangent, halfThickness, effectSize, alpha * 0.75f);
        drawOuterSide(poseStack, overlayBuffer, center, tangent, bitangent, normal, halfThickness, effectSize, alpha * 0.45f);

        var trimBuffer = buffers.getBuffer(SHIELD_TRIM_RENDER_TYPE);
        drawTrimRing(poseStack, trimBuffer, center, normal, tangent, bitangent, halfThickness, effectSize, alpha);

        if (effect.renderWave()) {
            var rippleBuffer = buffers.getBuffer(RIPPLE_RENDER_TYPE);
            drawRipple(poseStack, rippleBuffer, center, normal, tangent, bitangent, halfThickness, effectSize, age, alpha);
        }
    }

    private static void drawFilledHex(PoseStack poseStack, VertexConsumer buffer, Vec3 center, Vec3 normal, Vec3 tangent, Vec3 bitangent,
                                      float halfThickness, float sizeScale, float alpha) {
        var frontCenter = toWorld(center, tangent, bitangent, normal, 0f, 0f, halfThickness);
        var backCenter = toWorld(center, tangent, bitangent, normal, 0f, 0f, -halfThickness);

        for (int i = 0; i < OUTER_HEX_VERTICES.length; i++) {
            var next = (i + 1) % OUTER_HEX_VERTICES.length;
            var currentOuter = OUTER_HEX_VERTICES[i];
            var nextOuter = OUTER_HEX_VERTICES[next];
            var currentX = currentOuter.x * sizeScale;
            var currentY = currentOuter.y * sizeScale;
            var nextX = nextOuter.x * sizeScale;
            var nextY = nextOuter.y * sizeScale;

            var frontA = toWorld(center, tangent, bitangent, normal, currentX, currentY, halfThickness);
            var frontB = toWorld(center, tangent, bitangent, normal, nextX, nextY, halfThickness);
            addTriangleAsQuad(
                    poseStack,
                    buffer,
                    frontCenter,
                    frontA,
                    frontB,
                    uvFromHex(0f),
                    uvFromHex(0f),
                    uvFromHex(currentOuter.x),
                    uvFromHex(currentOuter.y),
                    uvFromHex(nextOuter.x),
                    uvFromHex(nextOuter.y),
                    SHIELD_TINT,
                    SHIELD_TINT,
                    SHIELD_TINT,
                    alpha,
                    normal
            );

            var backA = toWorld(center, tangent, bitangent, normal, currentX, currentY, -halfThickness);
            var backB = toWorld(center, tangent, bitangent, normal, nextX, nextY, -halfThickness);
            addTriangleAsQuad(
                    poseStack,
                    buffer,
                    backCenter,
                    backB,
                    backA,
                    uvFromHex(0f),
                    uvFromHex(0f),
                    uvFromHex(nextOuter.x),
                    uvFromHex(nextOuter.y),
                    uvFromHex(currentOuter.x),
                    uvFromHex(currentOuter.y),
                    SHIELD_TINT,
                    SHIELD_TINT,
                    SHIELD_TINT,
                    alpha,
                    normal.reverse()
            );
        }
    }

    private static void drawOuterSide(PoseStack poseStack, VertexConsumer buffer, Vec3 center, Vec3 tangent, Vec3 bitangent, Vec3 normal,
                                      float halfThickness, float sizeScale, float alpha) {
        for (int i = 0; i < OUTER_HEX_VERTICES.length; i++) {
            var next = (i + 1) % OUTER_HEX_VERTICES.length;
            var currentOuter = OUTER_HEX_VERTICES[i];
            var nextOuter = OUTER_HEX_VERTICES[next];
            var currentX = currentOuter.x * sizeScale;
            var currentY = currentOuter.y * sizeScale;
            var nextX = nextOuter.x * sizeScale;
            var nextY = nextOuter.y * sizeScale;

            var frontA = toWorld(center, tangent, bitangent, normal, currentX, currentY, halfThickness);
            var frontB = toWorld(center, tangent, bitangent, normal, nextX, nextY, halfThickness);
            var backB = toWorld(center, tangent, bitangent, normal, nextX, nextY, -halfThickness);
            var backA = toWorld(center, tangent, bitangent, normal, currentX, currentY, -halfThickness);

            var sideNormal = normalizeOrFallback(
                    tangent.scale((currentX + nextX) * 0.5f).add(bitangent.scale((currentY + nextY) * 0.5f)),
                    tangent
            );

            addQuad(
                    poseStack,
                    buffer,
                    frontA,
                    frontB,
                    backB,
                    backA,
                    uvFromHex(currentOuter.x),
                    uvFromHex(currentOuter.y),
                    uvFromHex(nextOuter.x),
                    uvFromHex(nextOuter.y),
                    uvFromHex(nextOuter.x),
                    uvFromHex(nextOuter.y) + 0.12f,
                    uvFromHex(currentOuter.x),
                    uvFromHex(currentOuter.y) + 0.12f,
                    SHIELD_TINT,
                    SHIELD_TINT,
                    SHIELD_TINT,
                    alpha,
                    sideNormal
            );
        }
    }

    private static void drawTrimRing(PoseStack poseStack, VertexConsumer buffer, Vec3 center, Vec3 normal, Vec3 tangent, Vec3 bitangent,
                                     float halfThickness, float sizeScale, float alpha) {
        for (int i = 0; i < OUTER_HEX_VERTICES.length; i++) {
            var next = (i + 1) % OUTER_HEX_VERTICES.length;
            var outerCurrent = OUTER_HEX_VERTICES[i];
            var outerNext = OUTER_HEX_VERTICES[next];
            var innerCurrent = INNER_HEX_VERTICES[i];
            var innerNext = INNER_HEX_VERTICES[next];
            var outerCurrentX = outerCurrent.x * sizeScale;
            var outerCurrentY = outerCurrent.y * sizeScale;
            var outerNextX = outerNext.x * sizeScale;
            var outerNextY = outerNext.y * sizeScale;
            var innerCurrentX = innerCurrent.x * sizeScale;
            var innerCurrentY = innerCurrent.y * sizeScale;
            var innerNextX = innerNext.x * sizeScale;
            var innerNextY = innerNext.y * sizeScale;

            var frontOuterA = toWorld(center, tangent, bitangent, normal, outerCurrentX, outerCurrentY, halfThickness);
            var frontOuterB = toWorld(center, tangent, bitangent, normal, outerNextX, outerNextY, halfThickness);
            var frontInnerB = toWorld(center, tangent, bitangent, normal, innerNextX, innerNextY, halfThickness);
            var frontInnerA = toWorld(center, tangent, bitangent, normal, innerCurrentX, innerCurrentY, halfThickness);

            addQuad(
                    poseStack,
                    buffer,
                    frontOuterA,
                    frontOuterB,
                    frontInnerB,
                    frontInnerA,
                    uvFromHex(outerCurrent.x),
                    uvFromHex(outerCurrent.y),
                    uvFromHex(outerNext.x),
                    uvFromHex(outerNext.y),
                    uvFromHex(innerNext.x),
                    uvFromHex(innerNext.y),
                    uvFromHex(innerCurrent.x),
                    uvFromHex(innerCurrent.y),
                    SHIELD_TINT,
                    SHIELD_TINT,
                    SHIELD_TINT,
                    alpha,
                    normal
            );

            var backOuterA = toWorld(center, tangent, bitangent, normal, outerCurrentX, outerCurrentY, -halfThickness);
            var backOuterB = toWorld(center, tangent, bitangent, normal, outerNextX, outerNextY, -halfThickness);
            var backInnerB = toWorld(center, tangent, bitangent, normal, innerNextX, innerNextY, -halfThickness);
            var backInnerA = toWorld(center, tangent, bitangent, normal, innerCurrentX, innerCurrentY, -halfThickness);

            addQuad(
                    poseStack,
                    buffer,
                    backOuterA,
                    backInnerA,
                    backInnerB,
                    backOuterB,
                    uvFromHex(outerCurrent.x),
                    uvFromHex(outerCurrent.y),
                    uvFromHex(innerCurrent.x),
                    uvFromHex(innerCurrent.y),
                    uvFromHex(innerNext.x),
                    uvFromHex(innerNext.y),
                    uvFromHex(outerNext.x),
                    uvFromHex(outerNext.y),
                    SHIELD_TINT,
                    SHIELD_TINT,
                    SHIELD_TINT,
                    alpha,
                    normal.reverse()
            );
        }
    }

    private static void drawRipple(PoseStack poseStack, VertexConsumer buffer, Vec3 center, Vec3 normal, Vec3 tangent, Vec3 bitangent,
                                   float halfThickness, float sizeScale, float age, float wallAlpha) {
        if (age >= RIPPLE_LIFETIME_TICKS) {
            return;
        }

        var progress = Mth.clamp(age / RIPPLE_LIFETIME_TICKS, 0f, 1f);
        var rippleAlpha = wallAlpha * (1.0f - progress);
        if (rippleAlpha <= 0.0f) {
            return;
        }

        var eased = 1.0f - (1.0f - progress) * (1.0f - progress);
        var rippleRadius = Mth.lerp(eased, RIPPLE_MIN_RADIUS * sizeScale, RIPPLE_MAX_RADIUS * sizeScale);
        var surfaceOffset = halfThickness + RIPPLE_SURFACE_OFFSET;

        drawRippleOnSurface(poseStack, buffer, center.add(normal.scale(surfaceOffset)), normal, tangent, bitangent, rippleRadius, rippleAlpha);
        drawRippleOnSurface(poseStack, buffer, center.subtract(normal.scale(surfaceOffset)), normal.reverse(), tangent, bitangent, rippleRadius, rippleAlpha);
    }

    private static void drawRippleOnSurface(PoseStack poseStack, VertexConsumer buffer, Vec3 center, Vec3 normal, Vec3 tangent, Vec3 bitangent,
                                            float radius, float alpha) {
        var p0 = toWorld(center, tangent, bitangent, normal, -radius, -radius, 0f);
        var p1 = toWorld(center, tangent, bitangent, normal, -radius, radius, 0f);
        var p2 = toWorld(center, tangent, bitangent, normal, radius, radius, 0f);
        var p3 = toWorld(center, tangent, bitangent, normal, radius, -radius, 0f);

        addDoubleSidedQuad(
                poseStack,
                buffer,
                p0,
                p1,
                p2,
                p3,
                0f, 1f,
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0.95f,
                0.95f,
                0.95f,
                alpha,
                normal
        );
    }

    private static void addTriangleAsQuad(PoseStack poseStack, VertexConsumer buffer, Vec3 p0, Vec3 p1, Vec3 p2,
                                          float u0, float v0, float u1, float v1, float u2, float v2,
                                          float r, float g, float b, float a, Vec3 normal) {
        addQuad(poseStack, buffer, p0, p1, p2, p2, u0, v0, u1, v1, u2, v2, u2, v2, r, g, b, a, normal);
    }

    private static void addDoubleSidedQuad(PoseStack poseStack, VertexConsumer buffer,
                                            Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                            float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3,
                                            float r, float g, float b, float a, Vec3 normal) {
        addQuad(poseStack, buffer, p0, p1, p2, p3, u0, v0, u1, v1, u2, v2, u3, v3, r, g, b, a, normal);
        addQuad(poseStack, buffer, p3, p2, p1, p0, u3, v3, u2, v2, u1, v1, u0, v0, r, g, b, a, normal.reverse());
    }

    private static void addQuad(PoseStack poseStack, VertexConsumer buffer,
                                Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3,
                                float r, float g, float b, float a, Vec3 normal) {
        var pose = poseStack.last();
        var poseMatrix = pose.pose();
        var normalMatrix = pose.normal();

        vertex(buffer, poseMatrix, normalMatrix, p0, u0, v0, r, g, b, a, normal);
        vertex(buffer, poseMatrix, normalMatrix, p1, u1, v1, r, g, b, a, normal);
        vertex(buffer, poseMatrix, normalMatrix, p2, u2, v2, r, g, b, a, normal);
        vertex(buffer, poseMatrix, normalMatrix, p3, u3, v3, r, g, b, a, normal);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix, Vec3 position, float u, float v,
                               float r, float g, float b, float a, Vec3 normal) {
        // ONE,ONE 加算合成では alpha が直接フェードに効きにくいため、RGB 側へも反映して減衰させる。
        var scaledR = r * a;
        var scaledG = g * a;
        var scaledB = b * a;
        buffer.vertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .color(scaledR, scaledG, scaledB, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normalMatrix, (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }

    private static Vec3 toWorld(Vec3 center, Vec3 tangent, Vec3 bitangent, Vec3 normal, float x, float y, float z) {
        return center
                .add(tangent.scale(x))
                .add(bitangent.scale(y))
                .add(normal.scale(z));
    }

    private static Vec3 buildTangent(Vec3 normal) {
        var reference = Math.abs(normal.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        var tangent = reference.cross(normal);
        if (tangent.lengthSqr() <= 1.0e-4) {
            tangent = new Vec3(0, 0, 1).cross(normal);
        }
        return normalizeOrFallback(tangent, new Vec3(1, 0, 0));
    }

    private static float getWallAlpha(float age) {
        if (age < HOLD_TICKS) {
            return SHIELD_ALPHA;
        }

        var fadeProgress = Mth.clamp((age - HOLD_TICKS) / FADE_TICKS, 0f, 1f);
        var fade = 1.0f - easeOutCubic(fadeProgress);
        return SHIELD_ALPHA * fade;
    }

    private static float easeOutCubic(float t) {
        var clamped = Mth.clamp(t, 0f, 1f);
        var inv = 1.0f - clamped;
        return 1.0f - inv * inv * inv;
    }

    private static float uvFromHex(float value) {
        return 0.5f + (value / (OUTER_RADIUS * 2f));
    }

    private static RenderType createAdditiveEntityRenderType(String renderTypeName, ResourceLocation texture) {
        return ApprenticeRenderTypes.additiveEntityNoCull(renderTypeName, texture);
    }

    private static Vec3 normalizeOrFallback(Vec3 vector, Vec3 fallback) {
        if (vector.lengthSqr() <= 1.0e-4) {
            return fallback.normalize();
        }
        return vector.normalize();
    }

    private static float sanitizeScale(float value) {
        if (!Float.isFinite(value)) {
            return 1.0f;
        }
        return Math.max(MIN_EFFECT_SCALE, value);
    }

    private static Vec2[] buildHexVertices(float radius) {
        var vertices = new Vec2[6];
        for (int i = 0; i < vertices.length; i++) {
            var angle = Math.toRadians(i * 60.0 - 90.0);
            vertices[i] = new Vec2((float) (Math.cos(angle) * radius), (float) (Math.sin(angle) * radius));
        }
        return vertices;
    }

    private record ActiveEffect(Vec3 position, Vec3 normal, long startGameTime, float sizeScale, float lifetimeScale,
                                boolean renderWave) {
    }
}
