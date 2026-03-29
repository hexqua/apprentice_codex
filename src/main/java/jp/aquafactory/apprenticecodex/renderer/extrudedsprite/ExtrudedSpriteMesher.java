package jp.aquafactory.apprenticecodex.renderer.extrudedsprite;

import com.mojang.blaze3d.platform.NativeImage;

import java.util.ArrayList;
import java.util.List;

public final class ExtrudedSpriteMesher {
    private static boolean isSolid(NativeImage img, int x, int y) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) {
            return false;
        }
        // ABGR 注意：NativeImageはRGBA系のユーティリティがやや罠.
        var argb = img.getPixelRGBA(x, y);
        var a = (argb >> 24) & 0xFF;
        return a >= 1;
    }

    public static ExtrudedSpriteMesh bake(NativeImage img, float thickness) {
        var w = img.getWidth();
        var h = img.getHeight();
        var zFront = 0.0f;
        var zBack  = thickness;

        List<ExtrudedSpriteMesh.Quad> quads = new ArrayList<>();

        // Front.
        // ここでは「表＝手前に向く」を +Z として扱う.
        {
            var q = new ExtrudedSpriteMesh.Quad(0, 0, 1);
            q.x[0] = 0; q.y[0] = 0; q.z[0] = zFront;
            q.x[1] = 1; q.y[1] = 0; q.z[1] = zFront;
            q.x[2] = 1; q.y[2] = 1; q.z[2] = zFront;
            q.x[3] = 0; q.y[3] = 1; q.z[3] = zFront;

            q.u[0] = 0; q.v[0] = 1;
            q.u[1] = 1; q.v[1] = 1;
            q.u[2] = 1; q.v[2] = 0;
            q.u[3] = 0; q.v[3] = 0;

            quads.add(q);
        }

        // Back.
        {
            var q = new ExtrudedSpriteMesh.Quad(0, 0, -1);
            q.x[0] = 0; q.y[0] = 0; q.z[0] = zBack;
            q.x[1] = 0; q.y[1] = 1; q.z[1] = zBack;
            q.x[2] = 1; q.y[2] = 1; q.z[2] = zBack;
            q.x[3] = 1; q.y[3] = 0; q.z[3] = zBack;

            q.u[0] = 0; q.v[0] = 1;
            q.u[1] = 0; q.v[1] = 0;
            q.u[2] = 1; q.v[2] = 0;
            q.u[3] = 1; q.v[3] = 1;

            quads.add(q);
        }

        // Sides (輪郭押し出し).
        for (var py = 0; py < h; py++) {
            for (var px = 0; px < w; px++) {
                if (!isSolid(img, px, py)) continue;

                var x0 = (px)     / (float) w;
                var x1 = (px + 1) / (float) w;
                var y0 = 1.0f - ((py + 1) / (float) h);
                var y1 = 1.0f - (py / (float) h);

                var u0 = x0;
                var u1 = x1;
                var v0 = y0;
                var v1 = y1;
                var uCenter = (px + 0.5f) / (float) w;
                // UV の V は画像上端が 0 のため、側面用サンプルはここで反転しない.
                var vCenter = (py + 0.5f) / (float) h;

                // 左境界 -> 壁(法線 -X)
                // 側面は宣言法線と頂点順を一致させないと陰影が反転する.
                if (!isSolid(img, px - 1, py)) {
                    var q = new ExtrudedSpriteMesh.Quad(-1, 0, 0);

                    q.x[0] = x0; q.y[0] = y0; q.z[0] = zFront;
                    q.x[1] = x0; q.y[1] = y0; q.z[1] = zBack;
                    q.x[2] = x0; q.y[2] = y1; q.z[2] = zBack;
                    q.x[3] = x0; q.y[3] = y1; q.z[3] = zFront;

                    q.u[0] = uCenter; q.v[0] = vCenter;
                    q.u[1] = uCenter; q.v[1] = vCenter;
                    q.u[2] = uCenter; q.v[2] = vCenter;
                    q.u[3] = uCenter; q.v[3] = vCenter;

                    quads.add(q);
                }

                // 右境界 -> 壁(法線 +X)
                if (!isSolid(img, px + 1, py)) {
                    var q = new ExtrudedSpriteMesh.Quad(1, 0, 0);

                    q.x[0] = x1; q.y[0] = y0; q.z[0] = zBack;
                    q.x[1] = x1; q.y[1] = y0; q.z[1] = zFront;
                    q.x[2] = x1; q.y[2] = y1; q.z[2] = zFront;
                    q.x[3] = x1; q.y[3] = y1; q.z[3] = zBack;

                    q.u[0] = uCenter; q.v[0] = vCenter;
                    q.u[1] = uCenter; q.v[1] = vCenter;
                    q.u[2] = uCenter; q.v[2] = vCenter;
                    q.u[3] = uCenter; q.v[3] = vCenter;

                    quads.add(q);
                }

                // 上境界 -> モデル上面(法線 +Y)
                if (!isSolid(img, px, py - 1)) {
                    var q = new ExtrudedSpriteMesh.Quad(0, 1, 0);

                    q.x[0] = x0; q.y[0] = y1; q.z[0] = zFront;
                    q.x[1] = x0; q.y[1] = y1; q.z[1] = zBack;
                    q.x[2] = x1; q.y[2] = y1; q.z[2] = zBack;
                    q.x[3] = x1; q.y[3] = y1; q.z[3] = zFront;

                    q.u[0] = uCenter; q.v[0] = vCenter;
                    q.u[1] = uCenter; q.v[1] = vCenter;
                    q.u[2] = uCenter; q.v[2] = vCenter;
                    q.u[3] = uCenter; q.v[3] = vCenter;

                    quads.add(q);
                }

                // 下境界 -> モデル下面(法線 -Y)
                if (!isSolid(img, px, py + 1)) {
                    var q = new ExtrudedSpriteMesh.Quad(0, -1, 0);

                    q.x[0] = x0; q.y[0] = y0; q.z[0] = zBack;
                    q.x[1] = x0; q.y[1] = y0; q.z[1] = zFront;
                    q.x[2] = x1; q.y[2] = y0; q.z[2] = zFront;
                    q.x[3] = x1; q.y[3] = y0; q.z[3] = zBack;

                    q.u[0] = uCenter; q.v[0] = vCenter;
                    q.u[1] = uCenter; q.v[1] = vCenter;
                    q.u[2] = uCenter; q.v[2] = vCenter;
                    q.u[3] = uCenter; q.v[3] = vCenter;

                    quads.add(q);
                }
            }
        }

        return new ExtrudedSpriteMesh(quads);
    }
}
