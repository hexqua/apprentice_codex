package jp.aquafactory.apprenticecodex.renderer.extrudedsprite;

import java.util.List;

public final class ExtrudedSpriteMesh {
    public static final class Quad {
        public final float[] x = new float[4];
        public final float[] y = new float[4];
        public final float[] z = new float[4];
        public final float[] u = new float[4];
        public final float[] v = new float[4];

        public final float nx, ny, nz;

        public Quad(float nx, float ny, float nz) {
            this.nx = nx; this.ny = ny; this.nz = nz;
        }
    }

    public final List<Quad> quads;
    public final float centerX;
    public final float centerY;
    public final float centerZ;

    public ExtrudedSpriteMesh(List<Quad> quads) {
        this.quads = quads;
        if (quads.isEmpty()) {
            centerX = 0.0f;
            centerY = 0.0f;
            centerZ = 0.0f;
            return;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (var quad : quads) {
            for (var i = 0; i < 4; ++i) {
                minX = Math.min(minX, quad.x[i]);
                minY = Math.min(minY, quad.y[i]);
                minZ = Math.min(minZ, quad.z[i]);
                maxX = Math.max(maxX, quad.x[i]);
                maxY = Math.max(maxY, quad.y[i]);
                maxZ = Math.max(maxZ, quad.z[i]);
            }
        }

        // 押し出しメッシュはキャッシュされるため、中心点もここで一度だけ固定する.
        centerX = (minX + maxX) * 0.5f;
        centerY = (minY + maxY) * 0.5f;
        centerZ = (minZ + maxZ) * 0.5f;
    }
}
