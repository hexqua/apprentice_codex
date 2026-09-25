package jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle;

import net.minecraft.util.Mth;

import java.util.LinkedHashSet;
import java.util.Set;

/** 接続先から同期した表示用設定。JEI は任意依存なので型を参照しない。 */
public final class FullautoEchoConfigState {
    private static final Set<Runnable> LISTENERS = new LinkedHashSet<>();
    private static boolean enabled;
    private static double manaMultiplier = 2.0D;
    private static int cooldownBypassThresholdTicks = 100;
    private static int cooldownReductionTicks = 200;
    private static int reducedCooldownMinimumTicks = 10;

    private FullautoEchoConfigState() {}

    public static boolean enabled() { return enabled; }
    public static double manaMultiplier() { return manaMultiplier; }
    public static int cooldownBypassThresholdTicks() { return cooldownBypassThresholdTicks; }
    public static int cooldownReductionTicks() { return cooldownReductionTicks; }
    public static int reducedCooldownMinimumTicks() { return reducedCooldownMinimumTicks; }

    public static void set(boolean enabled, double multiplier, int threshold, int reduction, int minimum) {
        FullautoEchoConfigState.enabled = enabled;
        manaMultiplier = Double.isFinite(multiplier) ? Mth.clamp(multiplier, 1.0D, 10.0D) : 2.0D;
        cooldownBypassThresholdTicks = Mth.clamp(threshold, 0, 72000);
        cooldownReductionTicks = Mth.clamp(reduction, 0, 72000);
        reducedCooldownMinimumTicks = Mth.clamp(minimum, 0, 72000);
        for (var listener : LISTENERS.toArray(Runnable[]::new)) listener.run();
    }

    public static void reset() { set(false, 2.0D, 100, 200, 10); }
    public static void addChangeListener(Runnable listener) { LISTENERS.add(listener); }
    public static void removeChangeListener(Runnable listener) { LISTENERS.remove(listener); }
}
