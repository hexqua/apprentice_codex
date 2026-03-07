package jp.aquafactory.apprenticecodex.spell.senseevil;

import com.mojang.blaze3d.systems.RenderSystem;
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
import java.util.Deque;
import java.util.List;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class SenseEvilHighlightRenderEvent {
    private static final ResourceLocation LIGHT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "textures/spell/sense_evil_light.png"
    );
    private static final RenderType LIGHT_RENDER_TYPE = ApprenticeRenderTypes.additiveEntityNoCullNoDepth(
            "sense_evil_light_additive_no_depth",
            LIGHT_TEXTURE
    );
    private static final int HOLD_TICKS = 80;
    private static final int FADE_TICKS = 20;
    private static final int TOTAL_TICKS = HOLD_TICKS + FADE_TICKS;
    private static final int MAX_ACTIVE_CASTS = 8;
    private static final int PARTICLES_PER_TARGET = 18;
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
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().level == null && !ACTIVE_CASTS.isEmpty()) {
            ACTIVE_CASTS.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE_CASTS.isEmpty()) {
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
        var buffers = minecraft.renderBuffers().bufferSource();
        var gameTime = level.getGameTime();
        var partialTick = event.getPartialTick();
        var cameraRotation = new Quaternionf(event.getCamera().rotation());

        // RenderType の NO_DEPTH_TEST は setup 時に深度テストを切らないため、ここで明示的に無効化する。
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        try {
            poseStack.pushPose();
            poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

            var iterator = ACTIVE_CASTS.iterator();
            while (iterator.hasNext()) {
                var activeCast = iterator.next();
                var age = (float) (gameTime - activeCast.startGameTime()) + partialTick;
                if (age >= TOTAL_TICKS) {
                    iterator.remove();
                    continue;
                }

                renderCast(poseStack, buffers.getBuffer(LIGHT_RENDER_TYPE), activeCast.targets(), age, cameraRotation);
            }

            poseStack.popPose();
            buffers.endBatch(LIGHT_RENDER_TYPE);
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private static void renderCast(PoseStack poseStack, VertexConsumer buffer, List<HighlightTarget> targets, float age,
                                   Quaternionf cameraRotation) {
        var fadeAlpha = getFadeAlpha(age);
        if (fadeAlpha <= 0.0f) {
            return;
        }

        for (var target : targets) {
            renderHighlight(poseStack, buffer, target, age, fadeAlpha, cameraRotation);
        }
    }

    private static void renderHighlight(PoseStack poseStack, VertexConsumer buffer, HighlightTarget target, float age, float fadeAlpha,
                                        Quaternionf cameraRotation) {
        var center = target.position();
        var basePhase = pulsePhase(center);
        var pulse = 0.88f + 0.12f * (0.5f + 0.5f * Mth.sin(age * 0.25f + basePhase));

        drawBillboard(poseStack, buffer, center, target.scale() * 0.46f, fadeAlpha * 0.38f * pulse, cameraRotation);
        drawBillboard(
                poseStack,
                buffer,
                center.add(0.0, target.scale() * 0.18f, 0.0),
                target.scale() * 0.26f,
                fadeAlpha * 0.24f * pulse,
                cameraRotation
        );

        for (int i = 0; i < PARTICLES_PER_TARGET; i++) {
            renderFlameParticle(poseStack, buffer, target, age, fadeAlpha, cameraRotation, i);
        }
    }

    private static void renderFlameParticle(PoseStack poseStack, VertexConsumer buffer, HighlightTarget target, float age,
                                            float fadeAlpha, Quaternionf cameraRotation, int index) {
        var center = target.position();
        var seedA = noise(center, index * 17 + 3);
        var seedB = noise(center, index * 17 + 7);
        var seedC = noise(center, index * 17 + 11);
        var seedD = noise(center, index * 17 + 13);
        var cycle = 14.0f + 12.0f * seedA;
        var progress = (age + seedB * cycle) % cycle / cycle;
        var rise = target.scale() * (0.14f + progress * (0.95f + 0.45f * seedC));
        var radius = target.scale() * (0.04f + 0.16f * seedD) * (1.0f - progress * 0.55f);
        var swirl = age * (0.03f + 0.03f * seedA) + seedC * Mth.TWO_PI;
        var offsetX = Mth.cos(swirl) * radius;
        var offsetZ = Mth.sin(swirl) * radius;
        var offsetY = -target.scale() * 0.18f + rise;
        var alpha = fadeAlpha * (1.0f - progress) * (1.0f - progress) * (0.22f + 0.45f * seedB);
        if (alpha <= 0.01f) {
            return;
        }

        var size = target.scale() * (0.07f + 0.12f * (1.0f - progress) + 0.04f * seedD);
        var particlePos = center.add(offsetX, offsetY, offsetZ);
        drawBillboard(poseStack, buffer, particlePos, size, alpha, cameraRotation);
    }

    private static void drawBillboard(PoseStack poseStack, VertexConsumer buffer, Vec3 center, float size, float alpha,
                                      Quaternionf cameraRotation) {
        var right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(cameraRotation).mul(size);
        var up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(cameraRotation).mul(size);
        var normalVector = new Vector3f(0.0f, 0.0f, 1.0f);
        normalVector.rotate(cameraRotation);
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
                alpha,
                new Vec3(normalVector.x(), normalVector.y(), normalVector.z())
        );
    }

    private static void addDoubleSidedQuad(PoseStack poseStack, VertexConsumer buffer,
                                           Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                           float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3,
                                           float alpha, Vec3 normal) {
        addQuad(poseStack, buffer, p0, p1, p2, p3, u0, v0, u1, v1, u2, v2, u3, v3, alpha, normal);
        addQuad(poseStack, buffer, p3, p2, p1, p0, u3, v3, u2, v2, u1, v1, u0, v0, alpha, normal.reverse());
    }

    private static void addQuad(PoseStack poseStack, VertexConsumer buffer,
                                Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3,
                                float alpha, Vec3 normal) {
        var pose = poseStack.last();
        var poseMatrix = pose.pose();
        var normalMatrix = pose.normal();

        vertex(buffer, poseMatrix, normalMatrix, p0, u0, v0, alpha, normal);
        vertex(buffer, poseMatrix, normalMatrix, p1, u1, v1, alpha, normal);
        vertex(buffer, poseMatrix, normalMatrix, p2, u2, v2, alpha, normal);
        vertex(buffer, poseMatrix, normalMatrix, p3, u3, v3, alpha, normal);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix, Vec3 position, float u, float v,
                               float alpha, Vec3 normal) {
        // 加算合成では alpha だけ下げても見え方が安定しないため、RGB 側にも同じ係数を掛ける。
        buffer.vertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .color(alpha, alpha, alpha, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normalMatrix, (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }

    private static float getFadeAlpha(float age) {
        if (age < HOLD_TICKS) {
            return 1.0f;
        }

        var progress = Mth.clamp((age - HOLD_TICKS) / FADE_TICKS, 0.0f, 1.0f);
        var eased = 1.0f - (1.0f - progress) * (1.0f - progress);
        return 1.0f - eased;
    }

    private static float pulsePhase(Vec3 position) {
        var seed = position.x * 0.173 + position.y * 0.271 + position.z * 0.347;
        return (float) (seed - Math.floor(seed)) * Mth.TWO_PI;
    }

    private static float noise(Vec3 position, int salt) {
        var value = Math.sin(position.x * 12.9898 + position.y * 78.233 + position.z * 37.719 + salt * 17.123);
        return (float) (value - Math.floor(value));
    }

    public record HighlightTarget(Vec3 position, float scale) {
    }

    private record ActiveCast(List<HighlightTarget> targets, long startGameTime) {
    }
}
