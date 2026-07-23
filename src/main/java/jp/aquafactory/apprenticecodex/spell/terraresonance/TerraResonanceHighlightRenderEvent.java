package jp.aquafactory.apprenticecodex.spell.terraresonance;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class TerraResonanceHighlightRenderEvent {
    private static final ResourceLocation RHOMBUS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/particle/glow_rhombus.png");
    private static final RenderType RHOMBUS_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCullNoDepth(
                    "terra_resonance_rhombus_additive_no_depth",
                    RHOMBUS_TEXTURE
            );
    private static final int HOLD_TICKS = 80;
    private static final int FADE_TICKS = 20;
    private static final int TOTAL_TICKS = HOLD_TICKS + FADE_TICKS;
    private static final int MAX_ACTIVE_CASTS = 4;
    // 多数表示時は全対象から二重コアと補助菱形を省き、発見位置の視認性とFPSを両立する。
    private static final int DETAILED_RENDER_TARGET_LIMIT = 512;
    private static final int AUXILIARY_RHOMBUS_COUNT = 2;
    private static final float AUXILIARY_CYCLE_TICKS = 18.0F;
    private static final float WHITE_FADE_TICKS = 5.0F;
    private static final float COLOR_RED = 34.0F / 255.0F;
    private static final float COLOR_GREEN = 1.0F;
    private static final float COLOR_BLUE = 102.0F / 255.0F;
    private static final Deque<ActiveCast> ACTIVE_CASTS = new ArrayDeque<>();

    private TerraResonanceHighlightRenderEvent() {
    }

    public static void enqueueHighlights(List<BlockPos> targets) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || targets.isEmpty()) {
            return;
        }

        ACTIVE_CASTS.addLast(new ActiveCast(List.copyOf(targets), minecraft.level, minecraft.level.getGameTime()));
        while (ACTIVE_CASTS.size() > MAX_ACTIVE_CASTS) {
            ACTIVE_CASTS.removeFirst();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
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

        var cameraPosition = event.getCamera().getPosition();
        var cameraRotation = new Quaternionf(event.getCamera().rotation());
        var poseStack = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();
        var buffer = buffers.getBuffer(RHOMBUS_RENDER_TYPE);
        var gameTime = level.getGameTime();

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        var iterator = ACTIVE_CASTS.iterator();
        while (iterator.hasNext()) {
            var cast = iterator.next();
            if (cast.level() != level) {
                iterator.remove();
                continue;
            }
            var age = (float) (gameTime - cast.startGameTime())
                    + event.getPartialTick().getGameTimeDeltaPartialTick(true);
            if (age >= TOTAL_TICKS) {
                iterator.remove();
                continue;
            }
            renderTargets(poseStack, buffer, cast.targets(), age, cameraRotation);
        }
        poseStack.popPose();
        // RenderType側でも深度テストを切るが、実際の描画はendBatch時に行われるため、
        // Forge 1.20.1の描画順に左右されず壁越し表示になるよう描き出し中も明示する。
        RenderSystem.disableDepthTest();
        try {
            buffers.endBatch(RHOMBUS_RENDER_TYPE);
        } finally {
            RenderSystem.enableDepthTest();
        }
    }

    private static void renderTargets(PoseStack poseStack, VertexConsumer buffer, List<BlockPos> targets,
                                      float age, Quaternionf cameraRotation) {
        var fade = getFadeAlpha(age);
        var pulse = 0.9F + 0.1F * Mth.sin(age * 0.35F);
        var initialColorProgress = clampUnit(age / WHITE_FADE_TICKS);
        var coreRed = Mth.lerp(initialColorProgress, 1.0F, COLOR_RED);
        var coreGreen = Mth.lerp(initialColorProgress, 1.0F, COLOR_GREEN);
        var coreBlue = Mth.lerp(initialColorProgress, 1.0F, COLOR_BLUE);
        var simplified = targets.size() > DETAILED_RENDER_TARGET_LIMIT;
        for (var target : targets) {
            var center = target.getCenter();

            drawBillboard(
                    poseStack,
                    buffer,
                    center,
                    0.30F * pulse,
                    coreRed,
                    coreGreen,
                    coreBlue,
                    fade * 0.72F,
                    cameraRotation,
                    0.0F
            );
            if (simplified) {
                continue;
            }
            drawBillboard(
                    poseStack,
                    buffer,
                    center,
                    0.19F * pulse,
                    coreRed,
                    coreGreen,
                    coreBlue,
                    fade * 0.62F,
                    cameraRotation,
                    (float) (Math.PI / 4.0D)
            );

            for (int index = 0; index < AUXILIARY_RHOMBUS_COUNT; index++) {
                renderAuxiliaryRhombus(poseStack, buffer, center, age, fade, cameraRotation, index);
            }
        }
    }

    private static void renderAuxiliaryRhombus(
            PoseStack poseStack,
            VertexConsumer buffer,
            Vec3 center,
            float age,
            float castFade,
            Quaternionf cameraRotation,
            int index
    ) {
        var seed = noise(center, index);
        var cycleOffset = (float) ((seed + index * 0.5D) * AUXILIARY_CYCLE_TICKS);
        var cycleAge = (age + cycleOffset) % AUXILIARY_CYCLE_TICKS;
        var progress = cycleAge / AUXILIARY_CYCLE_TICKS;
        var angle = (float) (seed * Math.PI * 2.0D + progress * Math.PI * 2.0D);
        var radius = 0.04D + progress * 0.11D;
        var position = center.add(
                Mth.cos(angle) * radius,
                progress * 0.28D,
                Mth.sin(angle) * radius
        );

        var colorProgress = clampUnit(cycleAge / WHITE_FADE_TICKS);
        var red = Mth.lerp(colorProgress, 1.0F, COLOR_RED);
        var green = Mth.lerp(colorProgress, 1.0F, COLOR_GREEN);
        var blue = Mth.lerp(colorProgress, 1.0F, COLOR_BLUE);
        var alpha = castFade * Mth.sin(progress * Mth.PI) * 0.62F;
        var size = Mth.lerp(progress, 0.16F, 0.23F);

        drawBillboard(
                poseStack,
                buffer,
                position,
                size,
                red,
                green,
                blue,
                alpha,
                cameraRotation,
                angle
        );
    }

    private static void drawBillboard(PoseStack poseStack, VertexConsumer buffer, Vec3 center, float size,
                                      float red, float green, float blue, float alpha,
                                      Quaternionf cameraRotation, float roll) {
        var facing = new Vector3f(0.0F, 0.0F, 1.0F).rotate(cameraRotation);
        var right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(cameraRotation);
        var up = new Vector3f(0.0F, 1.0F, 0.0F).rotate(cameraRotation);
        if (roll != 0.0F) {
            right.rotateAxis(roll, facing.x(), facing.y(), facing.z());
            up.rotateAxis(roll, facing.x(), facing.y(), facing.z());
        }
        right.mul(size);
        up.mul(size);
        var p0 = center.subtract(right.x + up.x, right.y + up.y, right.z + up.z);
        var p1 = center.add(-right.x + up.x, -right.y + up.y, -right.z + up.z);
        var p2 = center.add(right.x + up.x, right.y + up.y, right.z + up.z);
        var p3 = center.add(right.x - up.x, right.y - up.y, right.z - up.z);
        var normal = new Vec3(facing.x(), facing.y(), facing.z());
        addQuad(poseStack, buffer, p0, p1, p2, p3, normal, red, green, blue, alpha);
        addQuad(poseStack, buffer, p3, p2, p1, p0, normal.reverse(), red, green, blue, alpha);
    }

    private static double noise(Vec3 position, int salt) {
        var value = Mth.sin((float) (
                position.x * 12.9898D
                        + position.y * 78.233D
                        + position.z * 37.719D
                        + salt * 19.19D
        )) * 43758.5453D;
        return value - Math.floor(value);
    }

    private static float clampUnit(float value) {
        return Mth.clamp(value, 0.0F, 1.0F);
    }

    private static void addQuad(PoseStack poseStack, VertexConsumer buffer,
                                Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 normal,
                                float red, float green, float blue, float alpha) {
        var pose = poseStack.last();
        vertex(buffer, pose.pose(), pose.normal(), p0, 0.0F, 1.0F, normal, red, green, blue, alpha);
        vertex(buffer, pose.pose(), pose.normal(), p1, 0.0F, 0.0F, normal, red, green, blue, alpha);
        vertex(buffer, pose.pose(), pose.normal(), p2, 1.0F, 0.0F, normal, red, green, blue, alpha);
        vertex(buffer, pose.pose(), pose.normal(), p3, 1.0F, 1.0F, normal, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                               Vec3 position, float u, float v, Vec3 normal,
                               float red, float green, float blue, float alpha) {
        var transformedNormal = normalMatrix.transform(
                new Vector3f((float) normal.x, (float) normal.y, (float) normal.z)
        );
        buffer.addVertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .setColor(red * alpha, green * alpha, blue * alpha, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
    }

    private static float getFadeAlpha(float age) {
        if (age < HOLD_TICKS) {
            return 1.0F;
        }
        var progress = Mth.clamp((age - HOLD_TICKS) / FADE_TICKS, 0.0F, 1.0F);
        return 1.0F - progress * progress;
    }

    private record ActiveCast(List<BlockPos> targets, ClientLevel level, long startGameTime) {
    }
}
