package jp.aquafactory.apprenticecodex.spell.moonlight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class MoonLightChargeCutRenderer extends EntityRenderer<MoonLightChargeCutEntity> {
    private static final float PHASE_A_END = 0.12f;
    private static final float PHASE_B_END = 0.35f;
    private static final float PHASE_C_END = 0.70f;
    private static final float RIM_EXPAND_BLOCKS = 0.045f;
    private static final float CAP_REGION_RATIO = 0.20f;
    private static final float CAP_TAPER_MIN = 0.70f;
    private static final float CAP_TAPER_MAX = 0.85f;
    private static final float CORE_MAX_ALPHA = 0.30f;
    private static final float RIM_PULSE_FREQUENCY = 0.72f;
    private static final float FOCUS_PULSE_FREQUENCY = 1.05f;
    private static final float CAP_FLICKER_FREQUENCY = 2.20f;
    private static final int FOCUS_RED = 240;
    private static final int FOCUS_GREEN = 248;
    private static final int FOCUS_BLUE = 255;
    private static final int RIM_RED = 214;
    private static final int RIM_GREEN = 236;
    private static final int RIM_BLUE = 255;

    public MoonLightChargeCutRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(@NotNull MoonLightChargeCutEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        if (entity.isProcessingStarted()) {
            var progress = Mth.clamp(
                    entity.getProcessedDistanceForRender(partialTicks),
                    0.0f,
                    entity.getDistanceBlocks()
            );
            var state = calculateVisualState(entity, partialTicks);
            var coreAlpha = Mth.clamp((int) (state.coreAlpha() * 255.0f), 0, 255);
            var rimAlpha = Mth.clamp((int) (state.rimAlpha() * 255.0f), 0, 255);

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
            poseStack.translate(0.0, 0.0, progress);

            if (coreAlpha > 0) {
                var core = buffer.getBuffer(RenderType.endPortal());
                drawVCutLayer(
                        poseStack,
                        core,
                        state.nearHalfWidth(),
                        state.farHalfWidth(),
                        state.notchDepth(),
                        MoonLightChargeCutEntity.AREA_HEIGHT_BLOCKS,
                        255,
                        255,
                        255,
                        coreAlpha,
                        0.0f
                );
            }

            if (rimAlpha > 0) {
                var rim = buffer.getBuffer(RenderType.lightning());
                var rimNearHalfWidth = state.nearHalfWidth() + RIM_EXPAND_BLOCKS;
                var rimFarHalfWidth = state.farHalfWidth() + RIM_EXPAND_BLOCKS;
                drawVCutLayer(
                        poseStack,
                        rim,
                        rimNearHalfWidth,
                        rimFarHalfWidth,
                        calculateNotchDepth(rimFarHalfWidth),
                        MoonLightChargeCutEntity.AREA_HEIGHT_BLOCKS,
                        RIM_RED,
                        RIM_GREEN,
                        RIM_BLUE,
                        rimAlpha,
                        state.capFlicker()
                );
            }

            if (state.focusAlpha() > 0.0f) {
                var focusAlpha = Mth.clamp((int) (state.focusAlpha() * 255.0f), 0, 255);
                var focus = buffer.getBuffer(RenderType.lightning());
                drawFocusSlit(
                        poseStack,
                        focus,
                        state.notchDepth(),
                        MoonLightChargeCutEntity.AREA_HEIGHT_BLOCKS,
                        state.open(),
                        FOCUS_RED,
                        FOCUS_GREEN,
                        FOCUS_BLUE,
                        focusAlpha
                );
            }

            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MoonLightChargeCutEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private static RiftVisualState calculateVisualState(MoonLightChargeCutEntity entity, float partialTicks) {
        var activeTicks = Math.max(
                0.0f,
                entity.tickCount - MoonLightChargeCutEntity.PROCESS_START_DELAY_TICKS + partialTicks
        );
        var totalTicks = Math.max(1.0f, (float) MoonLightChargeCutEntity.PROCESS_DURATION_TICKS);
        var t = Mth.clamp(activeTicks / totalTicks, 0.0f, 1.0f);

        var open = 1.0f;
        if (t < PHASE_A_END) {
            open = Mth.lerp(t / PHASE_A_END, 0.03f, 0.16f);
        } else if (t < PHASE_B_END) {
            var openT = smoothStep((t - PHASE_A_END) / (PHASE_B_END - PHASE_A_END));
            open = Mth.lerp(openT, 0.16f, 1.0f);
        }

        var stitch = t <= PHASE_C_END ? 0.0f : smoothStep((t - PHASE_C_END) / (1.0f - PHASE_C_END));
        var farClose = smoothStep(Mth.clamp(stitch * 1.35f, 0.0f, 1.0f));
        var nearClose = smoothStep(Mth.clamp((stitch - 0.35f) / 0.65f, 0.0f, 1.0f));

        var nearHalfWidth = MoonLightChargeCutEntity.VISUAL_NEAR_HALF_WIDTH_BLOCKS * open * Math.max(0.0f, 1.0f - nearClose);
        var farHalfWidth = MoonLightChargeCutEntity.VISUAL_FAR_HALF_WIDTH_BLOCKS * open * Math.max(0.0f, 1.0f - farClose);
        nearHalfWidth = Math.max(nearHalfWidth, 0.002f);
        farHalfWidth = Math.max(farHalfWidth, 0.002f);

        var rimBaseAlpha = 0.0f;
        if (t < PHASE_A_END) {
            rimBaseAlpha = Mth.lerp(t / PHASE_A_END, 0.42f, 0.72f);
        } else if (t < PHASE_B_END) {
            rimBaseAlpha = Mth.lerp((t - PHASE_A_END) / (PHASE_B_END - PHASE_A_END), 0.72f, 0.98f);
        } else if (t < PHASE_C_END) {
            rimBaseAlpha = 0.90f;
        } else {
            rimBaseAlpha = Mth.lerp((t - PHASE_C_END) / (1.0f - PHASE_C_END), 0.90f, 0.28f);
        }
        var rimPulse = 0.82f + 0.18f * Mth.sin((entity.tickCount + partialTicks) * RIM_PULSE_FREQUENCY * (float) (Math.PI * 2.0));
        var rimAlpha = Mth.clamp(rimBaseAlpha * rimPulse, 0.0f, 1.0f);

        var coreAlpha = 0.0f;
        if (t >= PHASE_A_END) {
            if (t < PHASE_B_END) {
                var delayedB = Mth.clamp((t - (PHASE_A_END + 0.08f)) / ((PHASE_B_END - PHASE_A_END) - 0.08f), 0.0f, 1.0f);
                coreAlpha = CORE_MAX_ALPHA * smoothStep(delayedB);
            } else if (t < PHASE_C_END) {
                coreAlpha = CORE_MAX_ALPHA;
            } else {
                coreAlpha = CORE_MAX_ALPHA * (1.0f - smoothStep((t - PHASE_C_END) / (1.0f - PHASE_C_END)));
            }
        }

        var focusBaseAlpha = 0.0f;
        if (t < PHASE_A_END) {
            focusBaseAlpha = Mth.lerp(t / PHASE_A_END, 0.65f, 1.0f);
        } else if (t < PHASE_C_END) {
            focusBaseAlpha = 0.88f;
        } else {
            focusBaseAlpha = Mth.lerp((t - PHASE_C_END) / (1.0f - PHASE_C_END), 0.88f, 0.35f);
        }
        var focusPulse = 0.82f + 0.18f * Mth.sin((entity.tickCount + partialTicks) * FOCUS_PULSE_FREQUENCY * (float) (Math.PI * 2.0));
        var focusAlpha = Mth.clamp(focusBaseAlpha * focusPulse, 0.0f, 1.0f);

        var capSin = 0.72f + 0.28f * Mth.sin((entity.tickCount + partialTicks) * CAP_FLICKER_FREQUENCY * (float) (Math.PI * 2.0));
        var capNoise = 0.60f + 0.40f * hash01(entity.getId() * 37.0f + entity.tickCount * 1.31f);
        var capFlicker = Mth.clamp(capSin * 0.65f + capNoise * 0.35f, 0.0f, 1.0f);
        if (t < PHASE_B_END) {
            capFlicker = Mth.lerp(Mth.clamp(t / PHASE_B_END, 0.0f, 1.0f), 0.45f, capFlicker);
        }

        return new RiftVisualState(
                nearHalfWidth,
                farHalfWidth,
                calculateNotchDepth(farHalfWidth),
                open,
                coreAlpha,
                rimAlpha,
                focusAlpha,
                capFlicker
        );
    }

    private static float smoothStep(float value) {
        var t = Mth.clamp(value, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static float hash01(float seed) {
        return Mth.frac(Mth.sin(seed * 12.9898f) * 43758.547f);
    }

    private static void drawVCutLayer(PoseStack poseStack, VertexConsumer vc,
                                      float nearHalfWidth, float farHalfWidth, float notchDepth, float height,
                                      int red, int green, int blue, int alpha, float capFlicker) {
        if (nearHalfWidth <= 0.0f || farHalfWidth <= 0.0f || height <= 0.0f || alpha <= 0) {
            return;
        }
        var matrix = poseStack.last().pose();
        var capHeight = height * CAP_REGION_RATIO;
        var topStart = height - capHeight;
        var capTaper = Mth.lerp(capFlicker, CAP_TAPER_MIN, CAP_TAPER_MAX);
        var capAlpha = Mth.clamp((int) (alpha * Mth.lerp(capFlicker, 0.92f, 1.22f)), 0, 255);

        drawVCutSegment(
                matrix,
                vc,
                nearHalfWidth,
                farHalfWidth,
                notchDepth,
                0.0f,
                capHeight,
                capTaper,
                1.0f,
                red, green, blue, alpha
        );

        drawVCutSegment(
                matrix,
                vc,
                nearHalfWidth,
                farHalfWidth,
                notchDepth,
                capHeight,
                topStart,
                1.0f,
                1.0f,
                red, green, blue, alpha
        );

        drawVCutSegment(
                matrix,
                vc,
                nearHalfWidth,
                farHalfWidth,
                notchDepth,
                topStart,
                height,
                1.0f,
                capTaper,
                red, green, blue, capAlpha
        );

        drawVCutSegment(
                matrix,
                vc,
                nearHalfWidth,
                farHalfWidth,
                notchDepth,
                0.0f,
                capHeight,
                capTaper,
                1.0f,
                red, green, blue, capAlpha
        );
    }

    private static void drawVCutSegment(Matrix4f matrix, VertexConsumer vc,
                                        float nearHalfWidth, float farHalfWidth, float notchDepth,
                                        float yStart, float yEnd, float startScale, float endScale,
                                        int red, int green, int blue, int alpha) {
        if (alpha <= 0 || yEnd <= yStart) {
            return;
        }

        var leftTopNear = -nearHalfWidth * endScale;
        var leftBottomNear = -nearHalfWidth * startScale;
        var leftBottomFar = -farHalfWidth * startScale;
        var leftTopFar = -farHalfWidth * endScale;
        addQuad(
                matrix,
                vc,
                leftTopNear, yEnd, 0.0f,
                leftBottomNear, yStart, 0.0f,
                leftBottomFar, yStart, -notchDepth,
                leftTopFar, yEnd, -notchDepth,
                red, green, blue, alpha
        );

        var rightTopNear = nearHalfWidth * endScale;
        var rightTopFar = farHalfWidth * endScale;
        var rightBottomFar = farHalfWidth * startScale;
        var rightBottomNear = nearHalfWidth * startScale;
        addQuad(
                matrix,
                vc,
                rightTopNear, yEnd, 0.0f,
                rightTopFar, yEnd, -notchDepth,
                rightBottomFar, yStart, -notchDepth,
                rightBottomNear, yStart, 0.0f,
                red, green, blue, alpha
        );
    }

    private static void drawFocusSlit(PoseStack poseStack, VertexConsumer vc, float notchDepth, float height, float open,
                                      int red, int green, int blue, int alpha) {
        if (alpha <= 0 || height <= 0.0f) {
            return;
        }
        var matrix = poseStack.last().pose();
        var centerY = height * 0.5f;
        var slitHalfHeight = height * Mth.lerp(open, 0.10f, 0.22f);
        var slitHalfWidth = Mth.lerp(open, 0.002f, 0.015f);
        var slitHalfDepth = Mth.lerp(open, 0.002f, 0.012f);
        var slitZ = -notchDepth + 0.003f;

        addDoubleSidedQuad(
                matrix,
                vc,
                -slitHalfWidth, centerY + slitHalfHeight, slitZ,
                -slitHalfWidth, centerY - slitHalfHeight, slitZ,
                slitHalfWidth, centerY - slitHalfHeight, slitZ,
                slitHalfWidth, centerY + slitHalfHeight, slitZ,
                red, green, blue, alpha
        );

        addDoubleSidedQuad(
                matrix,
                vc,
                0.0f, centerY + slitHalfHeight, slitZ - slitHalfDepth,
                0.0f, centerY - slitHalfHeight, slitZ - slitHalfDepth,
                0.0f, centerY - slitHalfHeight, slitZ + slitHalfDepth,
                0.0f, centerY + slitHalfHeight, slitZ + slitHalfDepth,
                red, green, blue, alpha
        );
    }

    private static void addDoubleSidedQuad(Matrix4f matrix, VertexConsumer vc,
                                           float x0, float y0, float z0,
                                           float x1, float y1, float z1,
                                           float x2, float y2, float z2,
                                           float x3, float y3, float z3,
                                           int red, int green, int blue, int alpha) {
        addQuad(matrix, vc, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, red, green, blue, alpha);
        addQuad(matrix, vc, x3, y3, z3, x2, y2, z2, x1, y1, z1, x0, y0, z0, red, green, blue, alpha);
    }

    private static float calculateNotchDepth(float farHalfWidth) {
        var halfAngleRad = (MoonLightChargeCutEntity.V_NOTCH_ANGLE_DEGREES * 0.5f) * Mth.DEG_TO_RAD;
        var tanHalf = (float) Math.tan(halfAngleRad);
        if (tanHalf <= 1.0e-4f) {
            return MoonLightChargeCutEntity.MIN_NOTCH_DEPTH;
        }
        return Mth.clamp(
                farHalfWidth / tanHalf,
                MoonLightChargeCutEntity.MIN_NOTCH_DEPTH,
                MoonLightChargeCutEntity.MAX_NOTCH_DEPTH
        );
    }

    private static void addQuad(Matrix4f matrix, VertexConsumer vc,
                                float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                int red, int green, int blue, int alpha) {
        vc.addVertex(matrix, x0, y0, z0).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x3, y3, z3).setColor(red, green, blue, alpha);
    }

    private record RiftVisualState(float nearHalfWidth, float farHalfWidth, float notchDepth, float open,
                                   float coreAlpha, float rimAlpha, float focusAlpha, float capFlicker) {}
}
