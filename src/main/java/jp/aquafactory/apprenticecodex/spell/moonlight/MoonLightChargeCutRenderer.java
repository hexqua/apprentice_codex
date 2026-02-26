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

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
            poseStack.translate(0.0, 0.0, progress);

            var vc = buffer.getBuffer(RenderType.endPortal());
            drawVCutSurfaces(
                    poseStack,
                    vc,
                    MoonLightChargeCutEntity.AREA_HALF_WIDTH_BLOCKS,
                    MoonLightChargeCutEntity.AREA_HEIGHT_BLOCKS
            );
            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MoonLightChargeCutEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private static void drawVCutSurfaces(PoseStack poseStack, VertexConsumer vc, float halfWidth, float height) {
        if (halfWidth <= 0.0f || height <= 0.0f) {
            return;
        }

        var halfAngleRad = (MoonLightChargeCutEntity.V_NOTCH_ANGLE_DEGREES * 0.5f) * Mth.DEG_TO_RAD;
        var tanHalf = (float) Math.tan(halfAngleRad);
        if (tanHalf <= 1.0e-4f) {
            return;
        }

        var notchDepth = Mth.clamp(
                halfWidth / tanHalf,
                MoonLightChargeCutEntity.MIN_NOTCH_DEPTH,
                MoonLightChargeCutEntity.MAX_NOTCH_DEPTH
        );
        var matrix = poseStack.last().pose();

        addQuad(
                matrix,
                vc,
                0.0f, height, 0.0f,
                0.0f, 0.0f, 0.0f,
                -halfWidth, 0.0f, -notchDepth,
                -halfWidth, height, -notchDepth
        );

        addQuad(
                matrix,
                vc,
                0.0f, height, 0.0f,
                halfWidth, height, -notchDepth,
                halfWidth, 0.0f, -notchDepth,
                0.0f, 0.0f, 0.0f
        );
    }

    private static void addQuad(Matrix4f matrix, VertexConsumer vc,
                                float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3) {
        vc.vertex(matrix, x0, y0, z0).color(255, 255, 255, 255).endVertex();
        vc.vertex(matrix, x1, y1, z1).color(255, 255, 255, 255).endVertex();
        vc.vertex(matrix, x2, y2, z2).color(255, 255, 255, 255).endVertex();
        vc.vertex(matrix, x3, y3, z3).color(255, 255, 255, 255).endVertex();
    }
}
