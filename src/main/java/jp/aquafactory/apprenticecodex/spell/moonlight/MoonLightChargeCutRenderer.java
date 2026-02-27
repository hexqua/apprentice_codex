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
    private static final float FLASH_PHASE_END = 0.08f;
    private static final float OPEN_PHASE_END = 0.32f;
    private static final float CLOSE_PHASE_START = 0.60f;
    private static final float RIM_EXPAND_BLOCKS = 0.045f;
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
                drawVCutSurfaces(
                        poseStack,
                        core,
                        state.nearHalfWidth(),
                        state.farHalfWidth(),
                        MoonLightChargeCutEntity.AREA_HEIGHT_BLOCKS,
                        255,
                        255,
                        255,
                        coreAlpha
                );
            }

            if (rimAlpha > 0) {
                var rim = buffer.getBuffer(RenderType.lightning());
                drawVCutSurfaces(
                        poseStack,
                        rim,
                        state.nearHalfWidth() + RIM_EXPAND_BLOCKS,
                        state.farHalfWidth() + RIM_EXPAND_BLOCKS,
                        MoonLightChargeCutEntity.AREA_HEIGHT_BLOCKS,
                        RIM_RED,
                        RIM_GREEN,
                        RIM_BLUE,
                        rimAlpha
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
        var totalTicks = Math.max(1.0f, MoonLightChargeCutEntity.PROCESS_DURATION_TICKS);
        var t = Mth.clamp(activeTicks / totalTicks, 0.0f, 1.0f);

        var open = 1.0f;
        if (t <= FLASH_PHASE_END) {
            open = Mth.lerp(t / FLASH_PHASE_END, 0.06f, 0.18f);
        } else if (t <= OPEN_PHASE_END) {
            open = Mth.lerp((t - FLASH_PHASE_END) / (OPEN_PHASE_END - FLASH_PHASE_END), 0.18f, 1.0f);
        }

        var closeProgress = 0.0f;
        if (t > CLOSE_PHASE_START) {
            closeProgress = smoothStep((t - CLOSE_PHASE_START) / (1.0f - CLOSE_PHASE_START));
        }

        var nearCloseScale = Math.max(0.05f, 1.0f - closeProgress);
        var farCloseScale = Mth.lerp(closeProgress, 1.0f, 0.35f);
        var nearHalfWidth = MoonLightChargeCutEntity.VISUAL_NEAR_HALF_WIDTH_BLOCKS * open * nearCloseScale;
        var farHalfWidth = MoonLightChargeCutEntity.VISUAL_FAR_HALF_WIDTH_BLOCKS * open * farCloseScale;
        nearHalfWidth = Math.max(nearHalfWidth, 0.002f);
        farHalfWidth = Math.max(farHalfWidth, nearHalfWidth + 0.01f);

        var coreAlpha = 0.0f;
        if (t > FLASH_PHASE_END) {
            if (t <= OPEN_PHASE_END) {
                coreAlpha = Mth.lerp((t - FLASH_PHASE_END) / (OPEN_PHASE_END - FLASH_PHASE_END), 0.0f, 0.9f);
            } else {
                var coreFade = Mth.clamp((t - CLOSE_PHASE_START) / (1.0f - CLOSE_PHASE_START), 0.0f, 1.0f);
                coreAlpha = Mth.lerp(coreFade, 0.9f, 0.1f);
            }
        }

        var rimBaseAlpha = 0.85f;
        if (t <= FLASH_PHASE_END) {
            rimBaseAlpha = Mth.lerp(t / FLASH_PHASE_END, 0.45f, 1.0f);
        } else if (t > CLOSE_PHASE_START) {
            rimBaseAlpha = Mth.lerp((t - CLOSE_PHASE_START) / (1.0f - CLOSE_PHASE_START), 0.85f, 0.3f);
        }

        var pulse = 0.85f + 0.15f * Mth.sin((entity.tickCount + partialTicks) * 0.9f * (float) (Math.PI * 2.0));
        var rimAlpha = Mth.clamp(rimBaseAlpha * pulse, 0.0f, 1.0f);
        coreAlpha *= (1.0f - closeProgress * 0.35f);

        return new RiftVisualState(nearHalfWidth, farHalfWidth, coreAlpha, rimAlpha);
    }

    private static float smoothStep(float value) {
        var t = Mth.clamp(value, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static void drawVCutSurfaces(PoseStack poseStack, VertexConsumer vc,
                                         float nearHalfWidth, float farHalfWidth, float height,
                                         int red, int green, int blue, int alpha) {
        if (nearHalfWidth <= 0.0f || farHalfWidth <= 0.0f || height <= 0.0f || alpha <= 0) {
            return;
        }
        farHalfWidth = Math.max(farHalfWidth, nearHalfWidth + 0.005f);

        var halfAngleRad = (MoonLightChargeCutEntity.V_NOTCH_ANGLE_DEGREES * 0.5f) * Mth.DEG_TO_RAD;
        var tanHalf = (float) Math.tan(halfAngleRad);
        if (tanHalf <= 1.0e-4f) {
            return;
        }

        var notchDepth = Mth.clamp(
                farHalfWidth / tanHalf,
                MoonLightChargeCutEntity.MIN_NOTCH_DEPTH,
                MoonLightChargeCutEntity.MAX_NOTCH_DEPTH
        );
        var matrix = poseStack.last().pose();

        addQuad(
                matrix,
                vc,
                -nearHalfWidth, height, 0.0f,
                -nearHalfWidth, 0.0f, 0.0f,
                -farHalfWidth, 0.0f, -notchDepth,
                -farHalfWidth, height, -notchDepth,
                red, green, blue, alpha
        );

        addQuad(
                matrix,
                vc,
                nearHalfWidth, height, 0.0f,
                farHalfWidth, height, -notchDepth,
                farHalfWidth, 0.0f, -notchDepth,
                nearHalfWidth, 0.0f, 0.0f,
                red, green, blue, alpha
        );
    }

    private static void addQuad(Matrix4f matrix, VertexConsumer vc,
                                float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                int red, int green, int blue, int alpha) {
        vc.vertex(matrix, x0, y0, z0).color(red, green, blue, alpha).endVertex();
        vc.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).endVertex();
        vc.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).endVertex();
        vc.vertex(matrix, x3, y3, z3).color(red, green, blue, alpha).endVertex();
    }

    private record RiftVisualState(float nearHalfWidth, float farHalfWidth, float coreAlpha, float rimAlpha) {}
}
