package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.Minecraft;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ScytheRecallRenderEvent {
    private record Trail(Vec3 from, Vec3 to, long time, int color) {}
    private static final List<Trail> TRAILS = new ArrayList<>();
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private ScytheRecallRenderEvent() {}
    public static void add(Vec3 from, Vec3 to, int color) {
        var level = Minecraft.getInstance().level;
        if (world != level) { TRAILS.clear(); world = level; }
        if (level != null) TRAILS.add(new Trail(from, to, level.getGameTime(), color));
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc = Minecraft.getInstance();
        if (mc.level != world || world == null) { TRAILS.clear(); world = mc.level; return; }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        TRAILS.removeIf(t -> world.getGameTime() - t.time >= 4);
        if (TRAILS.isEmpty()) return;
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        pose.pushPose();
        var buffers = mc.renderBuffers().bufferSource();
        var buffer = buffers.getBuffer(ApprenticeRenderTypes.scytheTrail());
        for (var trail : TRAILS) {
            var axis = trail.to.subtract(trail.from);
            var side = new Vec3(-axis.z, 0, axis.x).normalize().scale(1.5);
            if (side.lengthSqr() < 1.0e-8) side = new Vec3(1.5, 0, 0);
            var alpha = Math.max(0, 1 - (world.getGameTime() - trail.time + partial) / 4f);
            // SlashBladeと同じlightningの両面帯を、描画履歴ではなく帰還線分から生成する。
            for (int i = 0; i < 16; i++) {
                float a = i / 16f, b = (i + 1) / 16f;
                var p = trail.from.lerp(trail.to, a);
                var q = trail.from.lerp(trail.to, b);
                // 元の青白い帯の幅と明るさを復元し、手元側を再び最も明るくする。
                ScytheTrailRibbon.band(buffer, pose.last().pose(), p.subtract(camera).add(side), p.subtract(camera).subtract(side),
                        q.subtract(camera).add(side), q.subtract(camera).subtract(side), ScytheThrowEntity.DEFAULT_TRAIL_COLOR,
                        180 / 255f * alpha * a, 180 / 255f * alpha * a, 180 / 255f * alpha * b, 180 / 255f * alpha * b);
                // 学派色は広い淡い層として加え、基礎の視認性を色の明度に依存させない。
                ScytheTrailRibbon.segment(buffer, pose.last().pose(), p.subtract(camera), q.subtract(camera),
                        side.scale(1.3), side.scale(1.3), trail.color,
                        0.45f * alpha * a, 0.45f * alpha * b);
            }
        }
        pose.popPose();
        buffers.endBatch(ApprenticeRenderTypes.scytheTrail());
    }
}
