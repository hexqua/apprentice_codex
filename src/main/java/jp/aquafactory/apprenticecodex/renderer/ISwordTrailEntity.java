package jp.aquafactory.apprenticecodex.renderer;

import java.util.List;

public interface ISwordTrailEntity {
    record TrailBonePair(String cacheKey, String tipBone, String rootBone) {
    }

    enum TrailFadeEasing {
        EASE_OUT_QUAD {
            @Override
            public float apply(float t) {
                var x = clamp01(t);
                var inv = 1.0f - x;
                return 1.0f - inv * inv;
            }
        },
        EASE_IN_OUT_SINE {
            @Override
            public float apply(float t) {
                var x = clamp01(t);
                return (float) (0.5d * (1.0d - Math.cos(Math.PI * x)));
            }
        };

        public abstract float apply(float t);

        private static float clamp01(float t) {
            return Math.max(0.0f, Math.min(1.0f, t));
        }
    }

    boolean isTrailActive();

    int getTrailColorARGB();

    default int getTrailHistoryTicks() {
        return 12;
    }

    default TrailFadeEasing getTrailFadeEasing() {
        return TrailFadeEasing.EASE_OUT_QUAD;
    }

    default List<TrailBonePair> getTrailBonePairs() {
        return List.of(new TrailBonePair("default", "trail_tip", "trail_root"));
    }
}
