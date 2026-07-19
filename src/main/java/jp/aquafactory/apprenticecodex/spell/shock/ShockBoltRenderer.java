package jp.aquafactory.apprenticecodex.spell.shock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class ShockBoltRenderer extends EntityRenderer<ShockBoltEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/shock_bolt.png");
    private static final RenderType RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCull("shock_bolt_additive", TEXTURE);

    private static final float OUTER_WIDTH = 0.24f;
    private static final float INNER_WIDTH = 0.11f;

    public ShockBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(@NotNull ShockBoltEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight) {
        var localEnd = entity.getEndPosition().subtract(entity.position());
        if (localEnd.lengthSqr() < 1.0e-6) {
            return;
        }

        var lifeProgress = entity.getLifeProgress(partialTicks);
        var trimStart = getTrimStart(lifeProgress);
        var alphaScale = getAlphaScale(lifeProgress);
        var points = buildPath(localEnd, entity.getPathSeed());
        var totalLength = getPathLength(points);
        if (totalLength <= 1.0e-4f) {
            return;
        }

        var consumer = bufferSource.getBuffer(RENDER_TYPE);
        var cameraPos = entityRenderDispatcher.camera.getPosition();
        var visibleStart = totalLength * trimStart;
        var scrollV = -(entity.tickCount + partialTicks) * 0.45f;

        drawPath(entity, poseStack, consumer, cameraPos, points, visibleStart, scrollV,
                OUTER_WIDTH, 0.42f, 0.86f, 1.00f, 0.42f * alphaScale);
        drawPath(entity, poseStack, consumer, cameraPos, points, visibleStart, scrollV + 0.35f,
                INNER_WIDTH, 0.92f, 0.98f, 1.00f, 0.86f * alphaScale);

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ShockBoltEntity entity) {
        return TEXTURE;
    }

    private static float getTrimStart(float lifeProgress) {
        if (lifeProgress <= 0.35f) {
            return 0.0f;
        }

        var progress = Mth.clamp((lifeProgress - 0.35f) / 0.65f, 0.0f, 1.0f);
        return easeInCubic(progress);
    }

    private static float getAlphaScale(float lifeProgress) {
        var fadeProgress = Mth.clamp((lifeProgress - 0.55f) / 0.45f, 0.0f, 1.0f);
        return 1.0f - 0.55f * easeInCubic(fadeProgress);
    }

    private static List<Vec3> buildPath(Vec3 localEnd, int seed) {
        var length = localEnd.length();
        var segmentCount = Mth.clamp((int) Math.ceil(length / 1.6), 6, 18);
        var direction = localEnd.normalize();

        var upSeed = Math.abs(direction.y) < 0.92 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        var right = direction.cross(upSeed).normalize();
        var up = right.cross(direction).normalize();
        var maxJitter = Math.min(1.25, Math.max(0.28, length * 0.06));

        var points = new ArrayList<Vec3>(segmentCount + 1);
        points.add(Vec3.ZERO);

        for (var i = 1; i < segmentCount; i++) {
            var t = i / (double) segmentCount;
            var jitterScale = getJitterEnvelope(t);
            var random = RandomSource.create((((long) seed) << 32) ^ (0x9E3779B97F4A7C15L + i * 31L));
            var lateralX = (random.nextDouble() * 2.0 - 1.0) * maxJitter * jitterScale;
            var lateralY = (random.nextDouble() * 2.0 - 1.0) * maxJitter * jitterScale;

            var point = direction.scale(length * t)
                    .add(right.scale(lateralX))
                    .add(up.scale(lateralY));
            points.add(point);
        }

        points.add(localEnd);
        return points;
    }

    private static double getJitterEnvelope(double t) {
        var centerWeight = Math.sin(Math.PI * t);
        var endClamp = t <= 0.72
                ? 1.0
                : Math.pow(Mth.clamp((float) ((1.0 - t) / 0.28), 0.0f, 1.0f), 2.0);
        return centerWeight * endClamp;
    }

    private static float getPathLength(List<Vec3> points) {
        var length = 0.0f;
        for (var i = 1; i < points.size(); i++) {
            length += (float) points.get(i - 1).distanceTo(points.get(i));
        }
        return length;
    }

    private static float easeInCubic(float value) {
        var clamped = Mth.clamp(value, 0.0f, 1.0f);
        return clamped * clamped * clamped;
    }

    private static void drawPath(ShockBoltEntity entity, PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPos,
                                 List<Vec3> points, float visibleStart, float scrollV,
                                 float width, float red, float green, float blue, float alpha) {
        var consumed = 0.0f;

        for (var i = 1; i < points.size(); i++) {
            var start = points.get(i - 1);
            var end = points.get(i);
            var segmentLength = (float) start.distanceTo(end);
            if (segmentLength <= 1.0e-5f) {
                continue;
            }

            var segmentVisibleStart = Math.max(0.0f, visibleStart - consumed);
            if (segmentVisibleStart >= segmentLength) {
                consumed += segmentLength;
                continue;
            }

            var visibleFrom = segmentVisibleStart <= 1.0e-5f
                    ? start
                    : start.lerp(end, segmentVisibleStart / segmentLength);
            drawSegment(entity, poseStack, consumer, cameraPos, visibleFrom, end, width, red, green, blue, alpha,
                    scrollV + consumed);
            consumed += segmentLength;
        }
    }

    private static void drawSegment(ShockBoltEntity entity, PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPos,
                                    Vec3 start, Vec3 end, float width, float red, float green, float blue, float alpha,
                                    float scrollV) {
        var segment = end.subtract(start);
        var length = (float) segment.length();
        if (length <= 1.0e-5f) {
            return;
        }

        var direction = segment.normalize();
        var midpointWorld = entity.position().add(start.add(end).scale(0.5));
        var cameraDirection = cameraPos.subtract(midpointWorld);
        if (cameraDirection.lengthSqr() <= 1.0e-6) {
            cameraDirection = new Vec3(0.0, 1.0, 0.0);
        }

        var side = direction.cross(cameraDirection.normalize());
        if (side.lengthSqr() <= 1.0e-6) {
            var fallbackUp = Math.abs(direction.y) < 0.95 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
            side = direction.cross(fallbackUp);
        }
        side = side.normalize().scale(width * 0.5f);

        var normal = side.normalize().cross(direction).normalize();
        var v0 = scrollV;
        var v1 = scrollV + length * 1.35f;

        var startLeft = start.subtract(side);
        var startRight = start.add(side);
        var endRight = end.add(side);
        var endLeft = end.subtract(side);

        var pose = poseStack.last();
        addVertex(pose.pose(), pose.normal(), consumer, startLeft, 0.0f, v0, red, green, blue, alpha, normal);
        addVertex(pose.pose(), pose.normal(), consumer, startRight, 1.0f, v0, red, green, blue, alpha, normal);
        addVertex(pose.pose(), pose.normal(), consumer, endRight, 1.0f, v1, red, green, blue, alpha, normal);
        addVertex(pose.pose(), pose.normal(), consumer, endLeft, 0.0f, v1, red, green, blue, alpha, normal);
    }

    private static void addVertex(Matrix4f poseMatrix, Matrix3f normalMatrix, VertexConsumer consumer, Vec3 position,
                                  float u, float v, float red, float green, float blue, float alpha, Vec3 normal) {
        var transformedNormal = normalMatrix.transform(new Vector3f((float) normal.x, (float) normal.y, (float) normal.z));
        consumer.addVertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
    }
}
