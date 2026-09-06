package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** 回転軌跡と帰還で共通の、中央が明るく縁へ透ける両面帯。 */
final class ScytheTrailRibbon {
    private ScytheTrailRibbon() {}

    static void band(VertexConsumer buffer, Matrix4f pose, Vec3 outerA, Vec3 innerA, Vec3 outerB, Vec3 innerB,
                     int color, float outerAlphaA, float innerAlphaA, float outerAlphaB, float innerAlphaB) {
        Vec3[] points = {outerA, innerA, innerB, outerB};
        float[] alphas = {outerAlphaA, innerAlphaA, innerAlphaB, outerAlphaB};
        for (int face = 0; face < 2; face++) {
            for (int j = 0; j < 4; j++) {
                int i = face == 0 ? j : 3 - j;
                var v = points[i];
                buffer.addVertex(pose, (float) v.x, (float) v.y, (float) v.z)
                        .setColor((color >> 16) & 255, (color >> 8) & 255, color & 255,
                                (int) (255 * Math.clamp(alphas[i], 0, 1)));
            }
        }
    }

    static void segment(VertexConsumer buffer, Matrix4f pose, Vec3 from, Vec3 to,
                        Vec3 fromSide, Vec3 toSide, int color, float fromAlpha, float toAlpha) {
        // 淡い外側と細い芯を重ね、単色の板に見えないようにする。
        strip(buffer, pose, from, to, fromSide, toSide, color, fromAlpha, toAlpha);
        int core = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            int channel = (color >> shift) & 255;
            core |= (channel + (255 - channel) / 3) << shift;
        }
        strip(buffer, pose, from, to, fromSide.scale(0.18), toSide.scale(0.18), core, fromAlpha, toAlpha);
    }

    private static void strip(VertexConsumer buffer, Matrix4f pose, Vec3 from, Vec3 to,
                              Vec3 fromSide, Vec3 toSide, int color, float fromAlpha, float toAlpha) {
        for (int side : new int[]{-1, 1}) {
            Vec3[] points = {from, from.add(fromSide.scale(side)), to.add(toSide.scale(side)), to};
            float[] alphas = {fromAlpha, 0, 0, toAlpha};
            for (int face = 0; face < 2; face++) {
                for (int j = 0; j < 4; j++) {
                    int i = face == 0 ? j : 3 - j;
                    var v = points[i];
                    buffer.addVertex(pose, (float) v.x, (float) v.y, (float) v.z)
                            .setColor((color >> 16) & 255, (color >> 8) & 255, color & 255,
                                    (int) (255 * Math.clamp(alphas[i], 0, 1)));
                }
            }
        }
    }
}
