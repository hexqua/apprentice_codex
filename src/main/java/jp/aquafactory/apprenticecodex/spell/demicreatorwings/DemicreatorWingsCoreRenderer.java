package jp.aquafactory.apprenticecodex.spell.demicreatorwings;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class DemicreatorWingsCoreRenderer extends EntityRenderer<DemicreatorWingsCoreEntity> {
    private static final RenderType RANGE_RENDER_TYPE =
            ApprenticeRenderTypes.translucentColorNoCull("demicreator_wings_range");
    private static final int WARNING_START_TICKS = DemicreatorWingsManager.ALERT_THRESHOLD_TICKS;
    private static final int WARNING_FADE_TICKS = 20;
    private static final float COLUMN_HEIGHT = 4.0f;
    private static final float BAND_HEIGHT = 0.42f;

    public DemicreatorWingsCoreRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(@NotNull DemicreatorWingsCoreEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var viewer = Minecraft.getInstance().player;
        if (viewer != null && viewer.getId() == entity.getOwnerEntityId()) {
            var warningProgress = Mth.clamp((WARNING_START_TICKS - entity.getRemainingTicks()) / (float) WARNING_FADE_TICKS, 0.0f, 1.0f);
            var red = Mth.lerp(warningProgress, 0.15f, 0.95f);
            var green = Mth.lerp(warningProgress, 0.9f, 0.18f);
            var blue = Mth.lerp(warningProgress, 0.25f, 0.2f);
            var consumer = buffer.getBuffer(RANGE_RENDER_TYPE);
            var halfExtent = entity.getAllowedRadius();

            poseStack.pushPose();
            renderSquareColumn(poseStack, consumer, halfExtent, 0.0f, COLUMN_HEIGHT, red, green, blue, 0.38f);
            if (Math.abs(viewer.getEyeY() - entity.getY()) >= 8.0) {
                var bandCenterY = (float) (viewer.getEyeY() - entity.getY());
                renderSquareColumn(
                        poseStack,
                        consumer,
                        halfExtent,
                        bandCenterY - BAND_HEIGHT * 0.5f,
                        bandCenterY + BAND_HEIGHT * 0.5f,
                        red,
                        green,
                        blue,
                        0.28f
                );
            }
            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(@NotNull DemicreatorWingsCoreEntity entity, @NotNull Frustum frustum,
                                double camX, double camY, double camZ) {
        var viewer = Minecraft.getInstance().player;
        if (viewer != null && viewer.getId() == entity.getOwnerEntityId()) {
            // 枠は発動者だけが見る案内表示なので、コアが画面外でも描画更新を止めない。
            return true;
        }

        return super.shouldRender(entity, frustum, camX, camY, camZ);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull DemicreatorWingsCoreEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private static void renderSquareColumn(PoseStack poseStack, VertexConsumer consumer, float halfExtent,
                                           float minY, float maxY, float red, float green, float blue, float alpha) {
        var poseMatrix = poseStack.last().pose();
        addWall(consumer, poseMatrix, +halfExtent, minY, -halfExtent, +halfExtent, minY, +halfExtent, +halfExtent, maxY, +halfExtent, +halfExtent, maxY, -halfExtent, red, green, blue, alpha);
        addWall(consumer, poseMatrix, -halfExtent, minY, +halfExtent, -halfExtent, minY, -halfExtent, -halfExtent, maxY, -halfExtent, -halfExtent, maxY, +halfExtent, red, green, blue, alpha);
        addWall(consumer, poseMatrix, +halfExtent, minY, +halfExtent, -halfExtent, minY, +halfExtent, -halfExtent, maxY, +halfExtent, +halfExtent, maxY, +halfExtent, red, green, blue, alpha);
        addWall(consumer, poseMatrix, -halfExtent, minY, -halfExtent, +halfExtent, minY, -halfExtent, +halfExtent, maxY, -halfExtent, -halfExtent, maxY, -halfExtent, red, green, blue, alpha);
    }

    private static void addWall(VertexConsumer consumer, Matrix4f poseMatrix,
                                float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float red, float green, float blue, float alpha) {
        consumer.addVertex(poseMatrix, x0, y0, z0).setColor(red, green, blue, alpha);
        consumer.addVertex(poseMatrix, x1, y1, z1).setColor(red, green, blue, alpha);
        consumer.addVertex(poseMatrix, x2, y2, z2).setColor(red, green, blue, 0.0f);
        consumer.addVertex(poseMatrix, x3, y3, z3).setColor(red, green, blue, 0.0f);
    }
}
