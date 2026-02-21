package jp.aquafactory.apprenticecodex.renderer;

import java.util.List;

public interface ISwordTrailEntity {
    record TrailBonePair(String cacheKey, String tipBone, String rootBone) {
    }

    boolean isTrailActive();

    int getTrailColorARGB();

    default List<TrailBonePair> getTrailBonePairs() {
        return List.of(new TrailBonePair("default", "trail_tip", "trail_root"));
    }
}
