package jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle;

import java.util.LinkedHashSet;
import java.util.Set;

/** 接続先から同期した表示用設定。JEI は任意依存なので型を参照しない。 */
public final class FullautoEchoConfigState {
    private static final Set<Runnable> LISTENERS = new LinkedHashSet<>();
    private static boolean enabled;
    private static double manaMultiplier = 2.0D;

    private FullautoEchoConfigState() {}

    public static boolean enabled() { return enabled; }
    public static double manaMultiplier() { return manaMultiplier; }

    public static void set(boolean enabled, double multiplier) {
        FullautoEchoConfigState.enabled = enabled;
        manaMultiplier = Double.isFinite(multiplier) ? Math.clamp(multiplier, 1.0D, 10.0D) : 2.0D;
        for (var listener : LISTENERS.toArray(Runnable[]::new)) listener.run();
    }

    public static void reset() { set(false, 2.0D); }
    public static void addChangeListener(Runnable listener) { LISTENERS.add(listener); }
    public static void removeChangeListener(Runnable listener) { LISTENERS.remove(listener); }
}
