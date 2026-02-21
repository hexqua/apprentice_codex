package jp.aquafactory.apprenticecodex.renderer;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GeoBonePoseCache {
    private static final String DEFAULT_KEY = "default";

    private GeoBonePoseCache() {
    }

    public record Pose(Vec3 tip, Vec3 root, long gameTime) {
    }

    public record PosePair(Pose prev, Pose curr) {
    }

    private static final Map<UUID, Map<String, PosePair>> LAST = new HashMap<>();

    public static void put(UUID id, Vec3 tip, Vec3 root, long gameTime) {
        put(id, DEFAULT_KEY, tip, root, gameTime);
    }

    public static void put(UUID id, String key, Vec3 tip, Vec3 root, long gameTime) {
        var curr = new Pose(tip, root, gameTime);
        var keyed = LAST.computeIfAbsent(id, ignored -> new HashMap<>());
        var pair = keyed.get(key);
        if (pair == null) {
            keyed.put(key, new PosePair(curr, curr));
            return;
        }

        keyed.put(key, new PosePair(pair.curr(), curr));
    }

    public static Pose getCurr(UUID id) {
        return getCurr(id, DEFAULT_KEY);
    }

    public static Pose getCurr(UUID id, String key) {
        var keyed = LAST.get(id);
        if (keyed == null) {
            return null;
        }

        var pair = keyed.get(key);
        return pair == null ? null : pair.curr();
    }

    public static Pose getPrev(UUID id) {
        return getPrev(id, DEFAULT_KEY);
    }

    public static Pose getPrev(UUID id, String key) {
        var keyed = LAST.get(id);
        if (keyed == null) {
            return null;
        }

        var pair = keyed.get(key);
        return pair == null ? null : pair.prev();
    }

    public static void remove(UUID id) {
        LAST.remove(id);
    }

    public static void remove(UUID id, String key) {
        var keyed = LAST.get(id);
        if (keyed == null) {
            return;
        }

        keyed.remove(key);
        if (keyed.isEmpty()) {
            LAST.remove(id);
        }
    }
}
