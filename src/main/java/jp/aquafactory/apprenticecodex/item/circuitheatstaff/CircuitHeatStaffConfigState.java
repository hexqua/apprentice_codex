package jp.aquafactory.apprenticecodex.item.circuitheatstaff;

public final class CircuitHeatStaffConfigState {
    public static final int DEFAULT_COOLDOWN_BYPASS_MAX_REMAINING_TICKS = 0;

    private static int cooldownBypassMaxRemainingTicks = DEFAULT_COOLDOWN_BYPASS_MAX_REMAINING_TICKS;

    private CircuitHeatStaffConfigState() {
    }

    public static int cooldownBypassMaxRemainingTicks() {
        return cooldownBypassMaxRemainingTicks;
    }

    public static void setCooldownBypassMaxRemainingTicks(int value) {
        cooldownBypassMaxRemainingTicks = Math.max(0, value);
    }

    public static void reset() {
        setCooldownBypassMaxRemainingTicks(DEFAULT_COOLDOWN_BYPASS_MAX_REMAINING_TICKS);
    }
}
