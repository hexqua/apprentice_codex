package jp.aquafactory.apprenticecodex.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public class SwordTrailLayer<T extends Entity & GeoEntity & ISwordTrailEntity> extends GeoRenderLayer<T> {
    private static final int MAX_POINTS = 12;

    private final Object2ObjectMap<UUID, Object2ObjectMap<String, Deque<Vec3>>> tipHist = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<UUID, Object2ObjectMap<String, Deque<Vec3>>> rootHist = new Object2ObjectOpenHashMap<>();

    public SwordTrailLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel model,
                       RenderType renderType, MultiBufferSource bufferSource,
                       VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!animatable.isTrailActive()) {
            clear(animatable);
            return;
        }

        var uuid = animatable.getUUID();
        var argb = animatable.getTrailColorARGB();
        for (var pair : animatable.getTrailBonePairs()) {
            var tip = model.getBone(pair.tipBone()).orElse(null);
            var root = model.getBone(pair.rootBone()).orElse(null);
            if (tip == null || root == null) {
                continue;
            }

            var tipPos = boneWorldPos(tip);
            var rootPos = boneWorldPos(root);

            var tips = getHistory(tipHist, uuid, pair.cacheKey());
            var roots = getHistory(rootHist, uuid, pair.cacheKey());
            push(tips, tipPos);
            push(roots, rootPos);
            renderRibbon(poseStack, bufferSource, animatable, argb, tips, roots);
        }
    }

    private Deque<Vec3> getHistory(Object2ObjectMap<UUID, Object2ObjectMap<String, Deque<Vec3>>> history, UUID entityId, String key) {
        return history.computeIfAbsent(entityId, ignored -> new Object2ObjectOpenHashMap<>())
                .computeIfAbsent(key, ignored -> new ArrayDeque<>());
    }

    private static Vec3 boneWorldPos(GeoBone bone) {
        var p = bone.getWorldPosition();
        return new Vec3(p.x(), p.y(), p.z());
    }

    @SuppressWarnings("DuplicatedCode")
    private static void renderRibbon(PoseStack poseStack,
                                     MultiBufferSource buffers,
                                     Entity entity,
                                     int argb,
                                     Deque<Vec3> tips,
                                     Deque<Vec3> roots) {
        var vc = buffers.getBuffer(RenderType.lightning());
        var epos = entity.position();
        var mat = poseStack.last().pose();

        var tipArr = tips.toArray(Vec3[]::new);
        var rootArr = roots.toArray(Vec3[]::new);
        var n = Math.min(tipArr.length, rootArr.length);
        if (n < 2) {
            return;
        }

        var a = (argb >>> 24) & 0xFF;
        var r = (argb >>> 16) & 0xFF;
        var g = (argb >>> 8) & 0xFF;
        var b = (argb) & 0xFF;

        for (var i = 0; i < n - 1; ++i) {
            var timeFade = i / (float) (n - 1);
            timeFade = timeFade * timeFade;

            var alphaTip = timeFade;
            var alphaRoot = timeFade * 0.2f;

            var tip0 = tipArr[i].subtract(epos);
            var root0 = rootArr[i].subtract(epos);
            var tip1 = tipArr[i + 1].subtract(epos);
            var root1 = rootArr[i + 1].subtract(epos);

            var aTip = (int) (alphaTip * a);
            var aRoot = (int) (alphaRoot * a);
            vc.vertex(mat, (float) tip0.x, (float) tip0.y, (float) tip0.z).color(r, g, b, aTip).endVertex();
            vc.vertex(mat, (float) root0.x, (float) root0.y, (float) root0.z).color(r, g, b, aRoot).endVertex();
            vc.vertex(mat, (float) root1.x, (float) root1.y, (float) root1.z).color(r, g, b, aRoot).endVertex();
            vc.vertex(mat, (float) tip1.x, (float) tip1.y, (float) tip1.z).color(r, g, b, aTip).endVertex();
            // カメラ位置や振り方向による裏面消失を防ぐため、逆順の面も描画する.
            vc.vertex(mat, (float) tip1.x, (float) tip1.y, (float) tip1.z).color(r, g, b, aTip).endVertex();
            vc.vertex(mat, (float) root1.x, (float) root1.y, (float) root1.z).color(r, g, b, aRoot).endVertex();
            vc.vertex(mat, (float) root0.x, (float) root0.y, (float) root0.z).color(r, g, b, aRoot).endVertex();
            vc.vertex(mat, (float) tip0.x, (float) tip0.y, (float) tip0.z).color(r, g, b, aTip).endVertex();
        }
    }

    private void push(Deque<Vec3> deque, Vec3 value) {
        if (deque.size() >= MAX_POINTS) {
            deque.removeFirst();
        }
        deque.addLast(value);
    }

    private void clear(T animatable) {
        tipHist.remove(animatable.getUUID());
        rootHist.remove(animatable.getUUID());
    }
}
