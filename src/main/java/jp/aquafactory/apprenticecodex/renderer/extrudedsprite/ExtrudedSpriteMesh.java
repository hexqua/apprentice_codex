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

    public ExtrudedSpriteMesh(List<Quad> quads) {
        this.quads = quads;
    }
}
