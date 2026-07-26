package jp.aquafactory.apprenticecodex.spell.senseevil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.renderer.WallThroughHighlightRenderSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class SenseEvilHighlightRenderEvent {
    private static final ResourceLocation CIRCLE_TEXTURE = texture("sense_evil_light_circle");
    private static final RenderType CIRCLE_RENDER_TYPE = renderType("sense_evil_light_circle", CIRCLE_TEXTURE);
    private static final int HOLD_TICKS = 80;
    private static final int FADE_TICKS = 20;
    private static final int TOTAL_TICKS = HOLD_TICKS + FADE_TICKS;
    private static final int MAX_ACTIVE_CASTS = 8;
    // 中心の灯り感はここで固定コスト化し、見た目調整で粒子数を増やし続けないようにする。
    private static final int CORE_PARTICLE_COUNT = 6;
    private static final float TAU = (float) (Math.PI * 2.0);
    private static final Deque<ActiveCast> ACTIVE_CASTS = new ArrayDeque<>();

    private SenseEvilHighlightRenderEvent() {
    }

    public static void enqueueHighlights(List<HighlightTarget> targets) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || targets.isEmpty()) {
            return;
        }

        ACTIVE_CASTS.addLast(new ActiveCast(List.copyOf(targets), minecraft.level.getGameTime()));
        while (ACTIVE_CASTS.size() > MAX_ACTIVE_CASTS) {
            ACTIVE_CASTS.removeFirst();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null && !ACTIVE_CASTS.isEmpty()) {
            ACTIVE_CASTS.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!WallThroughHighlightRenderSupport.shouldRenderAt(event.getStage()) || ACTIVE_CASTS.isEmpty()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            ACTIVE_CASTS.clear();
            return;
        }

        var poseStack = event.getPoseStack();
        var cameraPosition = event.getCamera().getPosition();
        var gameTime = level.getGameTime();
        var partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        var cameraRotation = new Quaternionf(event.getCamera().rotation());

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        var circleBuffer = WallThroughHighlightRenderSupport.getBuffer(CIRCLE_RENDER_TYPE);
        var iterator = ACTIVE_CASTS.iterator();
        while (iterator.hasNext()) {
            var activeCast = iterator.next();
            var age = (float) (gameTime - activeCast.startGameTime()) + partialTick;
            if (age >= TOTAL_TICKS) {
                iterator.remove();
                continue;
            }

            renderCast(poseStack, circleBuffer, activeCast.targets(), age, cameraRotation);
        }

        poseStack.popPose();
        WallThroughHighlightRenderSupport.endBatch(CIRCLE_RENDER_TYPE);
    }

    private static void renderCast(PoseStack poseStack, VertexConsumer circleBuffer,
                                   List<HighlightTarget> targets, float age, Quaternionf cameraRotation) {
        var fadeAlpha = getFadeAlpha(age);
        if (fadeAlpha <= 0.0f) {
            return;
        }

        for (var target : targets) {
            renderHighlight(poseStack, circleBuffer, target, age, fadeAlpha, cameraRotation);
        }
    }

    private static void renderHighlight(PoseStack poseStack, VertexConsumer circleBuffer,
                                        HighlightTarget target, float age, float fadeAlpha, Quaternionf cameraRotation) {
        // 中心の太い発光と、外周を抜ける小粒子を分けて重ねる。
        for (int i = 0; i < CORE_PARTICLE_COUNT; i++) {
            renderCoreParticle(poseStack, circleBuffer, target, age, fadeAlpha, cameraRotation, i);
        }

        var profile = target.variant().getProfile();
        for (int i = 0; i < profile.flameCount(); i++) {
            renderFlameParticle(poseStack, circleBuffer, target, age, fadeAlpha, cameraRotation, i);
        }
    }

    private static void renderCoreParticle(PoseStack poseStack, VertexConsumer buffer, HighlightTarget target, float age,
                                           float fadeAlpha, Quaternionf cameraRotation, int index) {
        var center = target.position();
        var profile = target.variant().getProfile();
        var seedA = noise(center, index * 29 + 5);
        var seedB = noise(center, index * 29 + 11);
        var seedC = noise(center, index * 29 + 17);
        var cycle = 22.0f + 8.0f * seedA;
        var progress = (age + seedB * cycle) % cycle / cycle;
        var localAge = progress * cycle;
        var color = mixColorFromWhite(
                profile.circleRed(),
                profile.circleGreen(),
                profile.circleBlue(),
                localAge,
                profile.circleWhitenTicks()
        );
        var offsetX = ((seedA * 2.0f) - 1.0f) * target.scale() * 0.065f;
        var offsetZ = ((seedB * 2.0f) - 1.0f) * target.scale() * 0.065f;
        var offsetY = target.scale() * (0.02f + 0.055f * seedC);
        var fadeWindow = Mth.clamp((progress - 0.76f) / 0.24f, 0.0f, 1.0f);
        var alpha = fadeAlpha
                * (0.92f + 0.24f * seedC)
                * (1.0f - fadeWindow * fadeWindow)
                * profile.circleAlpha();
        if (alpha <= 0.01f) {
            return;
        }

        var sizeJitter = 0.9f + 0.22f * seedC;
        var size = target.scale() * (0.27f + 0.07f * seedA + 0.05f * (1.0f - progress)) * sizeJitter * profile.circleScaleMultiplier();
        var particlePos = center.add(offsetX, offsetY, offsetZ);
        drawBillboard(
                poseStack,
                buffer,
                particlePos,
                size,
                size,
                0.0f,
                color.red(),
                color.green(),
                color.blue(),
                alpha,
                cameraRotation
        );
    }

    private static void renderFlameParticle(PoseStack poseStack, VertexConsumer buffer, HighlightTarget target, float age,
                                            float fadeAlpha, Quaternionf cameraRotation, int index) {
        var center = target.position();
        var profile = target.variant().getProfile();
        var seedA = noise(center, index * 17 + 3);
        var seedB = noise(center, index * 17 + 7);
        var seedC = noise(center, index * 17 + 11);
        var seedD = noise(center, index * 17 + 13);
        var cycle = 14.0f + 12.0f * seedA;
        var progress = (age + seedB * cycle) % cycle / cycle;
        var rise = target.scale() * (0.02f + progress * (0.46f + 0.12f * seedC));
        var orbitBase = target.scale() * (0.1f + 0.11f * seedD);
        var radius = orbitBase * (0.9f - progress * 0.28f);
        var swirl = age * (0.028f + 0.02f * seedA) + seedC * Mth.TWO_PI;
        var offsetX = Mth.cos(swirl) * radius;
        var offsetZ = Mth.sin(swirl) * radius;
        var offsetY = target.scale() * 0.02f + rise;
        var alpha = fadeAlpha
                * Mth.clamp(1.08f - progress, 0.0f, 1.0f)
                * (0.18f + 0.34f * seedB)
                * profile.flameAlphaMultiplier();
        if (alpha <= 0.01f) {
            return;
        }

        var localAge = progress * cycle;
        var color = mixColorFromWhite(
                profile.flameRed(),
                profile.flameGreen(),
                profile.flameBlue(),
                localAge,
                profile.flameWhitenTicks()
        );
        var size = target.scale() * (0.07f + 0.08f * (1.0f - progress) + 0.02f * seedD) * profile.flameSizeMultiplier();
        var roll = seedC * TAU + age * (0.04f + 0.05f * seedA);
        var particlePos = center.add(offsetX, offsetY, offsetZ);
        drawBillboard(
                poseStack,
                buffer,
                particlePos,
                size,
                size,
                roll,
                color.red(),
                color.green(),
                color.blue(),
                alpha,
                cameraRotation
        );
    }

    private static void drawBillboard(PoseStack poseStack, VertexConsumer buffer, Vec3 center, float size,
                                      float red, float green, float blue, float alpha, Quaternionf cameraRotation) {
        drawBillboard(poseStack, buffer, center, size, size, 0.0f, red, green, blue, alpha, cameraRotation);
    }

    private static void drawBillboard(PoseStack poseStack, VertexConsumer buffer, Vec3 center,
                                      float width, float height, float roll,
                                      float red, float green, float blue, float alpha, Quaternionf cameraRotation) {
        var facing = new Vector3f(0.0f, 0.0f, 1.0f).rotate(cameraRotation);
        var right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(cameraRotation);
        var up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(cameraRotation);
        if (Math.abs(roll) > 1.0e-4f) {
            right.rotateAxis(roll, facing.x(), facing.y(), facing.z());
            up.rotateAxis(roll, facing.x(), facing.y(), facing.z());
        }
        right.mul(width);
        up.mul(height);
        var p0 = center.subtract(right.x + up.x, right.y + up.y, right.z + up.z);
        var p1 = center.add(-right.x + up.x, -right.y + up.y, -right.z + up.z);
        var p2 = center.add(right.x + up.x, right.y + up.y, right.z + up.z);
        var p3 = center.add(right.x - up.x, right.y - up.y, right.z - up.z);
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
                new Vec3(facing.x(), facing.y(), facing.z())
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

    private static void vertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix, Vec3 position, float u, float v,
                               float red, float green, float blue, float alpha, Vec3 normal) {
        // 加算合成では alpha だけ下げても見え方が安定しないため、RGB 側にも同じ係数を掛ける。
        var transformedNormal = normalMatrix.transform(new Vector3f((float) normal.x, (float) normal.y, (float) normal.z));
        buffer.addVertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .setColor(clampUnit(red * alpha), clampUnit(green * alpha), clampUnit(blue * alpha), clampUnit(alpha))
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
    }

    private static HighlightColor mixColorFromWhite(float red, float green, float blue, float age, int whitenTicks) {
        return new HighlightColor(
                mixFromWhite(red, age, whitenTicks),
                mixFromWhite(green, age, whitenTicks),
                mixFromWhite(blue, age, whitenTicks)
        );
    }

    private static float mixFromWhite(float targetColor, float age, int whitenTicks) {
        if (whitenTicks <= 0) {
            return clampUnit(targetColor);
        }

        var progress = easeOutCubic(age / (float) whitenTicks);
        return Mth.lerp(progress, 1.0f, clampUnit(targetColor));
    }

    private static float easeOutCubic(float progress) {
        var clamped = Mth.clamp(progress, 0.0f, 1.0f);
        var inverse = 1.0f - clamped;
        return 1.0f - inverse * inverse * inverse;
    }

    private static float clampUnit(float value) {
        return Mth.clamp(value, 0.0f, 1.0f);
    }

    private static float getFadeAlpha(float age) {
        if (age < HOLD_TICKS) {
            return 1.0f;
        }

        var progress = Mth.clamp((age - HOLD_TICKS) / FADE_TICKS, 0.0f, 1.0f);
        var eased = 1.0f - (1.0f - progress) * (1.0f - progress);
        return 1.0f - eased;
    }

    private static float noise(Vec3 position, int salt) {
        var value = Math.sin(position.x * 12.9898 + position.y * 78.233 + position.z * 37.719 + salt * 17.123);
        return (float) (value - Math.floor(value));
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/" + path + ".png");
    }

    private static RenderType renderType(String name, ResourceLocation texture) {
        return ApprenticeRenderTypes.entityAdditiveGlowNoCullNoDepth(name + "_additive_no_depth", texture);
    }

    public record HighlightTarget(Vec3 position, float scale, SenseEvilHighlightVariant variant) {
    }

    private record ActiveCast(List<HighlightTarget> targets, long startGameTime) {
    }

    private record HighlightColor(float red, float green, float blue) {
    }
}
