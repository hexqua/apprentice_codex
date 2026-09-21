package jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;

public final class FullautoCooldownPolicy {
    private FullautoCooldownPolicy() {}

    public static int resolve(int baseCooldown, int effectiveCooldown, int castTime) {
        return calculate(baseCooldown, effectiveCooldown, castTime,
                ApprenticeCodexServerConfig.fullautoRapidcastSpellrifleCooldownBypassThresholdTicks(),
                ApprenticeCodexServerConfig.fullautoRapidcastSpellrifleCooldownReductionTicks(),
                ApprenticeCodexServerConfig.fullautoRapidcastSpellrifleReducedCooldownMinimumTicks());
    }

    public static int calculate(int baseCooldown, int effectiveCooldown, int castTime,
                                int threshold, int reduction, int minimum) {
        // 発動条件は装備やAttributeで変動させず、詠唱時間も実CDにだけ加える。
        if (Math.max(0, baseCooldown) <= threshold) return 0;
        int combined = (int) Math.min(Integer.MAX_VALUE,
                (long) Math.max(0, effectiveCooldown) + Math.max(0, castTime));
        return Math.min(combined, Math.max(minimum, combined - Math.max(0, reduction)));
    }
}
