package jp.aquafactory.apprenticecodex.spell.echoarrow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.*;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class EchoArrowCoreRenderer extends EntityRenderer<EchoArrowCoreEntity> {
    private record Tail(Level level, Vec3 position, Vec3 direction, long start, int count,
                        long end, int duration, int seed, long visibleSince) {
    }

    private static final List<Tail> TAILS = new ArrayList<>();
    private static final Map<EchoArrowCoreEntity, Long> VISIBLE_SINCE = new WeakHashMap<>();

    public EchoArrowCoreRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public void render(EchoArrowCoreEntity entity, float yaw, float partialTick, @NotNull PoseStack pose,
                       @NotNull MultiBufferSource buffers, int light) {
        double time = entity.level().getGameTime() + partialTick;
        long visible = VISIBLE_SINCE.getOrDefault(entity, entity.level().getGameTime());
        renderCore(pose, buffers, time, entity.startTime(), entity.getId(), 1);
        renderWaves(pose, buffers, entity.direction(), time, entity.startTime(), entity.totalCount(), visible, Long.MAX_VALUE);
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private static void renderCore(PoseStack pose, MultiBufferSource buffers, double time, long start, int seed, float ending) {
        float pulse = start >= 0 && time >= start ? (float) Math.max(0, 1 - ((time - start) % 3) / 3) : 0;
        float charge = start < 0 ? 0 : Mth.clamp((float) (time - (start - 10)) / 10, 0, 1);
        float scale = (0.35f - charge * 0.025f + pulse * 0.045f) * ending;
        if (scale <= 0) return;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((float) (time * 1.8 + seed * 37)));
        pose.mulPose(Axis.ZP.rotationDegrees(25));
        pose.mulPose(Axis.XP.rotationDegrees(15));
        pose.scale(scale, scale, scale);
        // endPortalはPOSITION専用。縁の色・透明度は別パスで扱う。
        var portal = buffers.getBuffer(RenderType.endPortal());
        for (int axis = 0; axis < 3; axis++) {
            for (int sign : new int[]{-1, 1}) {
                float[][] corners = {{-.5f, -.5f}, {.5f, -.5f}, {.5f, .5f}, {-.5f, .5f}};
                for (int i = 0; i < 4; i++) {
                    var c = corners[sign > 0 ? i : 3 - i];
                    float[] p = new float[3];
                    p[axis] = sign * .5f;
                    p[(axis + 1) % 3] = c[0];
                    p[(axis + 2) % 3] = c[1];
                    portal.addVertex(pose.last().pose(), p[0], p[1], p[2]);
                }
            }
        }
        var rim = buffers.getBuffer(RenderType.lightning());
        float alpha = (.32f + charge * .25f + pulse * .2f) * ending;
        for (int axis = 0; axis < 3; axis++)
            for (int a : new int[]{-1, 1})
                for (int b : new int[]{-1, 1}) {
                    float[] p = new float[3];
                    p[axis] = -.51f;
                    p[(axis + 1) % 3] = a * .51f;
                    p[(axis + 2) % 3] = b * .51f;
                    float[] q = p.clone();
                    q[axis] = .51f;
                    for (int side = 1; side <= 2; side++) {
                        int cross = (axis + side) % 3;
                        float[] p1 = p.clone(), p2 = p.clone(), q1 = q.clone(), q2 = q.clone();
                        p1[cross] -= .012f;
                        q1[cross] -= .012f;
                        p2[cross] += .012f;
                        q2[cross] += .012f;
                        quad(pose, rim, p1, q1, q2, p2, alpha);
                        quad(pose, rim, p2, q2, q1, p1, alpha);
                    }
                }
        pose.popPose();
    }

    private static void renderWaves(PoseStack pose, MultiBufferSource buffers, Vec3 direction, double time,
                                    long start, int count, long visibleSince, long ended) {
        if (start < 0 || time < start || direction.lengthSqr() < 1e-8) return;
        long last = Math.min((count + 2L) / 3 - 1, (long) Math.floor((time - start) / 3));
        pose.pushPose();
        pose.mulPose(new Quaternionf().rotationTo(new org.joml.Vector3f(0, 0, 1), direction.toVector3f()));
        var consumer = buffers.getBuffer(RenderType.lightning());
        for (long shot = Math.max(0, last - 2); shot <= last; shot++) {
            long born = start + shot * 3;
            if (born < visibleSince || born > ended) continue;
            float age = (float) (time - born);
            if (age < 0 || age >= 8) continue;
            float radius = .2f + .5f * age / 8, alpha = .45f * (1 - age / 8), width = .012f;
            for (int i = 0; i < 48; i++) {
                double a = i * Math.PI * 2 / 48, b = (i + 1) * Math.PI * 2 / 48;
                float[] p = {(float) Math.cos(a) * (radius - width), (float) Math.sin(a) * (radius - width), 0};
                float[] q = {(float) Math.cos(b) * (radius - width), (float) Math.sin(b) * (radius - width), 0};
                float[] r = {(float) Math.cos(b) * (radius + width), (float) Math.sin(b) * (radius + width), 0};
                float[] s = {(float) Math.cos(a) * (radius + width), (float) Math.sin(a) * (radius + width), 0};
                quad(pose, consumer, p, q, r, s, alpha);
                quad(pose, consumer, s, r, q, p, alpha);
            }
        }
        pose.popPose();
    }

    private static void quad(PoseStack pose, VertexConsumer consumer, float[] a, float[] b, float[] c, float[] d, float alpha) {
        for (var point : new float[][]{a, b, c, d})
            consumer.addVertex(pose.last().pose(), point[0], point[1], point[2])
                    .setColor(.35f, .9f, .85f, alpha);
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide && event.getEntity() instanceof EchoArrowCoreEntity core)
            VISIBLE_SINCE.put(core, event.getLevel().getGameTime());
    }

    @SubscribeEvent
    public static void removed(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide && event.getEntity() instanceof EchoArrowCoreEntity core) {
            Long visible = VISIBLE_SINCE.remove(core);
            if (core.clientEndTicks() > 0) TAILS.add(new Tail(event.getLevel(), core.position(), core.direction(),
                    core.startTime(), core.totalCount(), event.getLevel().getGameTime(), core.clientEndTicks(), core.getId(),
                    visible == null ? event.getLevel().getGameTime() : visible));
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        TAILS.removeIf(t -> t.level() != level || level.getGameTime() - t.end() >= 8);
        VISIBLE_SINCE.keySet().removeIf(e -> e.level() != level || e.isRemoved());
    }

    @SubscribeEvent
    public static void renderTails(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || TAILS.isEmpty()) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        double time = minecraft.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(true);
        var pose = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();
        var camera = event.getCamera().getPosition();
        for (var tail : TAILS) {
            if (tail.level() != minecraft.level) continue;
            pose.pushPose();
            var offset = tail.position().subtract(camera);
            pose.translate(offset.x, offset.y, offset.z);
            renderCore(pose, buffers, time, tail.start(), tail.seed(), Math.max(0, 1 - (float) (time - tail.end()) / tail.duration()));
            renderWaves(pose, buffers, tail.direction(), time, tail.start(), tail.count(), tail.visibleSince(), tail.end());
            pose.popPose();
        }
        buffers.endBatch(RenderType.endPortal());
        buffers.endBatch(RenderType.lightning());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EchoArrowCoreEntity entity) {
        return net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;
    }
}
