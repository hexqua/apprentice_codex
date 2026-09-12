package jp.aquafactory.apprenticecodex.spell.echoarrow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class EchoArrowRenderer extends EntityRenderer<EchoArrowEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID,
            "textures/entity/echo_arrow_arrow.png");
    private static final RenderType ARROW = ApprenticeRenderTypes.entityAdditiveGlowNoCull("echo_arrow", TEXTURE);
    private static final RenderType FOLLOWUP = RenderType.entityTranslucent(TEXTURE);

    public EchoArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public void render(EchoArrowEntity entity, float yaw, float partialTick, @NotNull PoseStack pose,
                       @NotNull MultiBufferSource buffers, int light) {
        Vec3 origin = entity.getPosition(partialTick);
        var trail = entity.trail();
        if (entity.isInitial()) {
            int index = 0;
            for (var sample : trail) {
                float alpha = 0.30f * (1 - index++ / 3f);
                drawArrow(pose, buffers, sample.position().subtract(origin), sample.direction(), alpha, 1, ARROW, 4);
            }
        } else if (!trail.isEmpty()) {
            // 1tick前の全身残像は後続の矢と重なるため、補間位置から0.35tickだけ遡る。
            Vec3 previous = trail.getFirst().position();
            Vec3 ghost = partialTick >= .35f
                    ? previous.lerp(entity.position(), partialTick - .35f)
                    : (trail.size() > 1 ? trail.get(1).position().lerp(previous, 1 + partialTick - .35f) : previous);
            drawArrow(pose, buffers, ghost.subtract(origin), entity.getDeltaMovement(), .12f, .65f, ARROW, 2);
        }
        drawArrow(pose, buffers, Vec3.ZERO, entity.getDeltaMovement(), 1,
                entity.isInitial() ? 1 : .65f, entity.isInitial() ? ARROW : FOLLOWUP, entity.isInitial() ? 4 : 2);
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private static void drawArrow(PoseStack pose, MultiBufferSource buffers, Vec3 offset, Vec3 direction,
                                  float alpha, float scale, RenderType renderType, int faces) {
        // 共有モデルの矢先は-Z。pitchを適用した後で飛行方向へ反転する。
        pose.pushPose();
        pose.translate(offset.x, offset.y, offset.z);
        pose.mulPose(Axis.YP.rotationDegrees((float) (-Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG + 90)));
        pose.mulPose(Axis.XP.rotationDegrees((float) (Mth.atan2(direction.horizontalDistance(), direction.y) * Mth.RAD_TO_DEG - 90)));
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.scale(scale, scale, scale);
        renderModel(pose, buffers, alpha, renderType, faces);
        pose.popPose();
    }

    public static void renderModel(PoseStack pose, MultiBufferSource buffers) {
        renderModel(pose, buffers, 1, ARROW, 4);
    }

    private static void renderModel(PoseStack pose, MultiBufferSource buffers, float alpha, RenderType renderType, int faces) {
        pose.pushPose();
        pose.scale(0.13f, 0.13f, 0.13f);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.translate(-2, 0, 0);
        var consumer = buffers.getBuffer(renderType);
        // 両面描画の追撃は交差する2面だけにし、同一平面の重ね描きを避ける。
        for (int face = 0; face < faces; face++) {
            pose.mulPose(Axis.XP.rotationDegrees(90));
            var matrix = pose.last().pose();
            float[][] points = {{-8, -2, 0, 0}, {8, -2, .5f, 0}, {8, 2, .5f, .15625f}, {-8, 2, 0, .15625f}};
            for (var point : points)
                consumer.addVertex(matrix, point[0], point[1], 0)
                        .setColor(1f, 1f, 1f, alpha).setUv(point[2], point[3]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
        }
        pose.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EchoArrowEntity entity) {
        return TEXTURE;
    }
}
