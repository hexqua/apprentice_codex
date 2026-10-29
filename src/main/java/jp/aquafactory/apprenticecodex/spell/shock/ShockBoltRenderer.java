package jp.aquafactory.apprenticecodex.spell.shock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
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

public class ShockBoltRenderer extends EntityRenderer<ShockBoltEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/shock_bolt.png");
    private final RenderType renderType;
    private final ResourceLocation texture;

    private static final float OUTER_WIDTH = 0.24f;
    private static final float INNER_WIDTH = 0.11f;

    public ShockBoltRenderer(EntityRendererProvider.Context context) {
        this(context, TEXTURE);
    }

    public ShockBoltRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        super(context);
        this.texture = texture;
        // ねじれた帯の外層・内層が互いを遮らないよう、地形の深度テストだけ残して深度は書き込まない。
        this.renderType = ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly("shock_bolt_additive_" + texture.getPath(), texture);
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
        var points = buildPath(localEnd, entity.getPathSeed());
        var totalLength = getPathLength(points);
        if (totalLength <= 1.0e-4f) {
            return;
        }

        var consumer = bufferSource.getBuffer(renderType);
        var cameraPos = entityRenderDispatcher.camera.getPosition();
        var visibleStart = totalLength * trimStart;
        var sections = BoltRibbonGeometry.build(points, cameraPos.subtract(entity.position()), OUTER_WIDTH * 0.5);

        drawPath(poseStack, consumer, sections, visibleStart, OUTER_WIDTH, 0.42f, 0.86f, 1.00f);
        drawPath(poseStack, consumer, sections, visibleStart, INNER_WIDTH, 0.92f, 0.98f, 1.00f);

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ShockBoltEntity entity) {
        return texture;
    }

    private static float getTrimStart(float lifeProgress) {
        if (lifeProgress <= 0.35f) {
            return 0.0f;
        }

        var progress = Mth.clamp((lifeProgress - 0.35f) / 0.65f, 0.0f, 1.0f);
        return easeInCubic(progress);
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

    private static void drawPath(PoseStack poseStack, VertexConsumer consumer, List<BoltRibbonGeometry.Section> sections,
                                 float visibleStart, float width, float red, float green, float blue) {
        var pose = poseStack.last();
        double halfWidth = width * 0.5;
        for (int i = 1; i < sections.size(); i++) {
            var start = sections.get(i - 1);
            var end = sections.get(i);
            if (end.distance() < visibleStart) continue;
            double length = end.distance() - start.distance();
            double trim = length > 1.0e-6 ? Mth.clamp((visibleStart - start.distance()) / length, 0, 1) : 0;
            var left = start.left(halfWidth).lerp(end.left(halfWidth), trim);
            var right = start.right(halfWidth).lerp(end.right(halfWidth), trim);
            var normal = start.normal().lerp(end.normal(), trim).normalize();
            // 同一断面のUVと法線を共有し、色の帯と照明の両方を折れ目で連続させる。
            // 長手方向の中央を使うため、元のテクスチャの色を保ったまま接続できる。
            addVertex(pose.pose(), pose.normal(), consumer, left, 0, 0.5f, red, green, blue, 1, normal);
            addVertex(pose.pose(), pose.normal(), consumer, right, 1, 0.5f, red, green, blue, 1, normal);
            addVertex(pose.pose(), pose.normal(), consumer, end.right(halfWidth), 1, 0.5f, red, green, blue, 1, end.normal());
            addVertex(pose.pose(), pose.normal(), consumer, end.left(halfWidth), 0, 0.5f, red, green, blue, 1, end.normal());
        }
    }

    public static void drawSegment(Vec3 origin, PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPos,
                                    Vec3 start, Vec3 end, float width, float red, float green, float blue, float alpha,
                                    float scrollV) {
        var segment = end.subtract(start);
        var length = (float) segment.length();
        if (length <= 1.0e-5f) {
            return;
        }

        var direction = segment.normalize();
        var midpointWorld = origin.add(start.add(end).scale(0.5));
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
        consumer.vertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normalMatrix, (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }
}
