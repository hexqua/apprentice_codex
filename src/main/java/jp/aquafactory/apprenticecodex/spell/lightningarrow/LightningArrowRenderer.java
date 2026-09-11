package jp.aquafactory.apprenticecodex.spell.lightningarrow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.render.RenderHelper;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.spell.shock.ShockBoltRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class LightningArrowRenderer extends EntityRenderer<LightningArrowEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "textures/entity/lightning_arrow_arrow.png");
    private static final net.minecraft.client.renderer.RenderType TRAIL = ApprenticeRenderTypes.entityAdditiveGlowNoCull(
            "shock_bolt_additive", ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/shock_bolt.png"));

    public LightningArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public void render(LightningArrowEntity entity, float yaw, float partialTick, @NotNull PoseStack pose,
                       @NotNull MultiBufferSource buffers, int light) {
        var flight = entity.flight();
        if (flight == null) return;
        var distance = entity.visualDistance(partialTick);
        var origin = entity.getPosition(partialTick);
        var head = flight.point(distance);
        var startDistance = Math.max(0, distance - flight.speed() * LightningArrowEntity.TRAIL_TICKS
                + flight.speed() * entity.visualStopAge(partialTick));
        var consumer = buffers.getBuffer(TRAIL);
        var previous = flight.point(startDistance);
        var previousDistance = startDistance;
        for (var index = (int) Math.floor(startDistance) + 1; previousDistance < distance; index++) {
            var nextDistance = Math.min(index, distance);
            var point = nextDistance == distance ? head : trailPoint(flight, nextDistance);
            var center = flight.point(nextDistance);
            // 中心線は通れる隙間でも、装飾の横揺れが壁内へ潜り込まないよう制限する。
            var radialHit = entity.level().clip(new ClipContext(center, point, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, entity));
            if (radialHit.getType() == HitResult.Type.BLOCK) point = center;
            var segmentHit = entity.level().clip(new ClipContext(previous, point, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, entity));
            if (segmentHit.getType() == HitResult.Type.BLOCK) point = segmentHit.getLocation();
            var ageDistance = distance - (previousDistance + nextDistance) * 0.5
                    + flight.speed() * entity.visualStopAge(partialTick);
            var alpha = (float) Math.clamp(1 - ageDistance / (flight.speed() * LightningArrowEntity.TRAIL_TICKS), 0, 1);
            var scroll = -(entity.tickCount + partialTick) * 0.45F + (float) previousDistance;
            var camera = entityRenderDispatcher.camera.getPosition();
            ShockBoltRenderer.drawSegment(origin, pose, consumer, camera, previous.subtract(origin), point.subtract(origin),
                    0.24F, 0.42F, 0.86F, 1.0F, 0.42F * alpha, scroll);
            ShockBoltRenderer.drawSegment(origin, pose, consumer, camera, previous.subtract(origin), point.subtract(origin),
                    0.11F, 0.92F, 0.98F, 1.0F, 0.86F * alpha, scroll + 0.35F);
            previous = point;
            previousDistance = nextDistance;
        }
        if (!entity.stopped() || distance < entity.traveledDistance() - 1.0e-5) {
            pose.pushPose();
            var offset = head.subtract(origin);
            pose.translate(offset.x, offset.y, offset.z);
            var direction = flight.direction();
            pose.mulPose(Axis.YP.rotationDegrees((float) (-Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG + 90)));
            // モデル内のY軸90度回転で矢先は+Zを向くため、上向きの照準には負のX回転を適用する。
            pose.mulPose(Axis.XP.rotationDegrees((float) (Mth.atan2(direction.horizontalDistance(), direction.y) * Mth.RAD_TO_DEG - 90)));
            renderModel(pose, buffers);
            pose.popPose();
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private static Vec3 trailPoint(LightningArrowFlight flight, double distance) {
        var direction = flight.direction();
        var up = Math.abs(direction.y) < 0.92 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        var right = direction.cross(up).normalize();
        up = right.cross(direction).normalize();
        var random = RandomSource.create((((long) flight.seed()) << 32) ^ ((long) distance * 31));
        var angle = random.nextDouble() * Math.PI * 2;
        var radius = random.nextDouble() * 0.4;
        return flight.point(distance).add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
    }

    public static void renderModel(PoseStack pose, MultiBufferSource buffers) {
        pose.scale(0.13F, 0.13F, 0.13F);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.translate(-2, 0, 0);
        var consumer = buffers.getBuffer(RenderHelper.CustomerRenderType.magic(TEXTURE));
        // MagicArrowと共通の16x5側面を使い、提供画像の色を白の頂点色で維持する。
        for (int face = 0; face < 4; face++) {
            pose.mulPose(Axis.XP.rotationDegrees(90));
            var matrix = pose.last().pose();
            consumer.addVertex(matrix, -8, -2, 0).setColor(-1).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 8, -2, 0).setColor(-1).setUv(0.5F, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, 8, 2, 0).setColor(-1).setUv(0.5F, 0.15625F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
            consumer.addVertex(matrix, -8, 2, 0).setColor(-1).setUv(0, 0.15625F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull LightningArrowEntity entity) {
        return TEXTURE;
    }
}
