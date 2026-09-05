package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import jp.aquafactory.apprenticecodex.model.SpellReaperScytheModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;
import java.util.WeakHashMap;

public final class ScytheThrowRenderer extends GeoEntityRenderer<ScytheThrowEntity> {
    private record Sample(double time, Vec3 center, Vec3 tip, Vec3 top, Vec3 bottom) {}
    private static final class History {
        final ArrayDeque<Sample> samples = new ArrayDeque<>();
        Sample previous;
        double nextSample;
    }
    // Entityを強参照せず、描画対象外で消えた投擲の履歴も回収できるようにする。
    private final WeakHashMap<ScytheThrowEntity, History> histories = new WeakHashMap<>();
    private Vec3 tip, top, bottom;
    public ScytheThrowRenderer(EntityRendererProvider.Context context) {
        super(context, new SpellReaperScytheModel<>());
        shadowRadius = 0;
    }

    @Override
    public void render(@NotNull ScytheThrowEntity entity, float yaw, float partialTick, @NotNull PoseStack pose,
                       @NotNull MultiBufferSource buffers, int light) {
        tip = top = bottom = null;
        super.render(entity, yaw, partialTick, pose, buffers, light);
        if (tip == null || top == null || bottom == null) { histories.remove(entity); return; }
        double time = entity.tickCount + (double) partialTick;
        var center = entity.getPosition(partialTick);
        var current = new Sample(time, center, center.add(tip), center.add(top), center.add(bottom));
        var history = histories.computeIfAbsent(entity, ignored -> new History());
        var previous = history.previous;
        // 非表示期間やテレポートを跨ぐ巨大な帯を作らない。
        if (previous == null || time < previous.time || time - previous.time > 2
                || center.distanceToSqr(previous.center) > 64) {
            history.samples.clear();
            history.nextSample = time;
            previous = current;
        }
        // 当フレームの実アンカー間を補間する。独立した回転式では方向・位相がずれるため使わない。
        while (history.nextSample <= time) {
            double delta = time - previous.time;
            double t = delta <= 0 ? 1 : Math.clamp((history.nextSample - previous.time) / delta, 0, 1);
            history.samples.addLast(new Sample(history.nextSample, previous.center.lerp(center, t),
                    previous.tip.lerp(current.tip, t), previous.top.lerp(current.top, t), previous.bottom.lerp(current.bottom, t)));
            history.nextSample += 0.25;
        }
        history.previous = current;
        double lifetime = entity.isHovering() ? 3 : 4;
        while (!history.samples.isEmpty() && time - history.samples.getFirst().time > lifetime) history.samples.removeFirst();
        var buffer = buffers.getBuffer(ApprenticeRenderTypes.scytheTrail());
        for (int arm = 0; arm < 2; arm++) {
            Sample last = null;
            for (var sample : history.samples) {
                if (last != null) drawSegment(entity, pose, buffer, center, time, lifetime, arm, last, sample);
                last = sample;
            }
            if (last != null && time > last.time) drawSegment(entity, pose, buffer, center, time, lifetime, arm, last, history.previous);
        }
    }

    @Override
    public void renderRecursively(PoseStack pose, ScytheThrowEntity entity, software.bernie.geckolib.cache.object.GeoBone bone,
                                  RenderType renderType, MultiBufferSource buffers, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                  boolean reRender, float partialTick, int light, int overlay, int color) {
        boolean anchor = bone.getName().equals("anchor_tip") || bone.getName().equals("anchor_top") || bone.getName().equals("anchor_bottom");
        if (anchor) bone.setTrackingMatrices(true);
        super.renderRecursively(pose, entity, bone, renderType, buffers, buffer, reRender, partialTick, light, overlay, color);
        if (anchor && !reRender) {
            // 当フレームの行列確定後に読む。world行列へのfloat遠方座標の混入も避ける。
            var p = bone.getLocalPosition();
            var position = new Vec3(p.x(), p.y(), p.z());
            switch (bone.getName()) {
                case "anchor_tip" -> tip = position;
                case "anchor_top" -> top = position;
                case "anchor_bottom" -> bottom = position;
            }
        }
    }

    private static void drawSegment(ScytheThrowEntity entity, PoseStack pose,
                                    com.mojang.blaze3d.vertex.VertexConsumer buffer, Vec3 center,
                                    double time, double lifetime, int arm, Sample from, Sample to) {
        float fadeA = (float) Math.max(0, 1 - (time - from.time) / lifetime);
        float fadeB = (float) Math.max(0, 1 - (time - to.time) / lifetime);
        var topA = from.top.subtract(center);
        var topB = to.top.subtract(center);
        if (arm == 0) {
            var tipA = from.tip.subtract(center);
            var tipB = to.tip.subtract(center);
            // 刃先～付け根が通った面にSchool色、刃先寄りには独立した白い層を残す。
            ScytheTrailRibbon.band(buffer, pose.last().pose(), tipA, topA, tipB, topB, entity.getTrailColor(),
                    fadeA * 0.65f, fadeA * 0.12f, fadeB * 0.65f, fadeB * 0.12f);
            ScytheTrailRibbon.band(buffer, pose.last().pose(), tipA, tipA.lerp(topA, 0.28), tipB, tipB.lerp(topB, 0.28),
                    0xEEF7FF, fadeA * 0.8f, fadeA * 0.2f, fadeB * 0.8f, fadeB * 0.2f);
        } else {
            // 柄側は刃側を邪魔しない短く細い補助残像にする。
            var bottomA = from.bottom.subtract(center);
            var bottomB = to.bottom.subtract(center);
            fadeA = (float) Math.max(0, 1 - (time - from.time) / 2);
            fadeB = (float) Math.max(0, 1 - (time - to.time) / 2);
            ScytheTrailRibbon.band(buffer, pose.last().pose(), bottomA, bottomA.lerp(topA, 0.12), bottomB, bottomB.lerp(topB, 0.12),
                    entity.getTrailColor(), fadeA * 0.3f, 0, fadeB * 0.3f, 0);
        }
    }
}
