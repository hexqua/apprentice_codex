package jp.aquafactory.apprenticecodex.renderer;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GeoBonePoseCache {
    private GeoBonePoseCache(){}

    public record Pose(Vec3 tip, Vec3 root, long gameTime) {}
    public record PosePair(Pose prev, Pose curr) {}

    private static final Map<UUID, PosePair> LAST = new HashMap<>();

    public static void put(UUID id, Vec3 tip, Vec3 root, long gameTime) {
        Pose curr = new Pose(tip, root, gameTime);
        PosePair pair = LAST.get(id);
        if (pair == null) {
            // 初回はprev=currで埋める.
            LAST.put(id, new PosePair(curr, curr));
        } else {
            // 1フレ遅延.
            LAST.put(id, new PosePair(pair.curr(), curr));
        }
    }

    public static Pose getCurr(UUID id) {
        PosePair p = LAST.get(id);
        return p == null ? null : p.curr();
    }

    public static Pose getPrev(UUID id) {
        PosePair p = LAST.get(id);
        return p == null ? null : p.prev();
    }

    public static void remove(UUID id) {
        LAST.remove(id);
    }
}
