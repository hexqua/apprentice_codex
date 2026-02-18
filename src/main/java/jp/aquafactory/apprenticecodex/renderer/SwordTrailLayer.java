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

    private final Object2ObjectMap<UUID, Deque<Vec3>> tipHist = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<UUID, Deque<Vec3>> rootHist = new Object2ObjectOpenHashMap<>();

    public SwordTrailLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel model,
                       RenderType renderType, MultiBufferSource bufferSource,
                       VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        // サーバー同期フラグを見る.
        if (!animatable.isTrailActive()) {
            clear(animatable);
            return;
        }

        var tip = model.getBone("trail_tip").orElse(null);
        var root = model.getBone("trail_root").orElse(null);
        if (tip == null || root == null) {
            return;
        }

        var tipPos  = boneWorldPos(tip);
        var rootPos = boneWorldPos(root);
        //noinspection resource
        GeoBonePoseCache.put(animatable.getUUID(), tipPos, rootPos, animatable.level().getGameTime());

        var argb = animatable.getTrailColorARGB();
        push(tipHist.computeIfAbsent(animatable.getUUID(), k -> new ArrayDeque<>()), tipPos);
        push(rootHist.computeIfAbsent(animatable.getUUID(), k -> new ArrayDeque<>()), rootPos);

        renderRibbon(poseStack, bufferSource, animatable, argb, tipHist.get(animatable.getUUID()), rootHist.get(animatable.getUUID()));
    }
    private static Vec3 boneWorldPos(GeoBone bone) {
        var p = bone.getWorldPosition();

        var x = p.x();
        var y = p.y();
        var z = p.z();

        return new Vec3(x, y, z);
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

        // Dequeはインデックスが無いので配列化.
        var tipArr = tips.toArray(Vec3[]::new);
        var rootArr = roots.toArray(Vec3[]::new);
        var n = Math.min(tipArr.length, rootArr.length);
        if (n < 2) {
            return;
        }

        var a = (argb >>> 24) & 0xFF;
        var r = (argb >>> 16) & 0xFF;
        var g = (argb >>>  8) & 0xFF;
        var b = (argb       ) & 0xFF;

        for (var i = 0; i < n - 1; ++i) {
            // 線形ではない変化を入れる.
            var timeFade = i / (float)(n - 1);
            timeFade = timeFade * timeFade;

            var alphaTip  = timeFade;
            var alphaRoot = timeFade * 0.2f;

            // カメラ相対にする.
            var tip0  = tipArr[i].subtract(epos);
            var root0 = rootArr[i].subtract(epos);
            var tip1  = tipArr[i + 1].subtract(epos);
            var root1 = rootArr[i + 1].subtract(epos);

            // quad: tip0 -> root0 -> root1 -> tip1
            var aTip  = (int)(alphaTip  * a);
            var aRoot = (int)(alphaRoot * a);
            vc.vertex(mat, (float)tip0.x,  (float)tip0.y,  (float)tip0.z).color(r, g, b, aTip).endVertex();
            vc.vertex(mat, (float)root0.x, (float)root0.y, (float)root0.z).color(r, g, b, aRoot).endVertex();
            vc.vertex(mat, (float)root1.x, (float)root1.y, (float)root1.z).color(r, g, b, aRoot).endVertex();
            vc.vertex(mat, (float)tip1.x,  (float)tip1.y,  (float)tip1.z).color(r, g, b, aTip).endVertex();
        }
    }

    private void push(Deque<Vec3> dq, Vec3 v) {
        if (dq.size() >= MAX_POINTS) dq.removeFirst();
        dq.addLast(v);
    }

    private void clear(T a) {
        tipHist.remove(a.getUUID());
        rootHist.remove(a.getUUID());
    }
}
