package jp.aquafactory.apprenticecodex.item.curios.manathruster;

public final class ManaThrusterConfigState {
    public static final float DEFAULT_MANA_COST_PER_TICK = 7.5F;

    private static float manaCostPerTick = DEFAULT_MANA_COST_PER_TICK;

    private ManaThrusterConfigState() {
    }

    public static float manaCostPerTick() {
        return manaCostPerTick;
    }

    public static void set(float manaCostPerTick) {
        ManaThrusterConfigState.manaCostPerTick = manaCostPerTick;
    }

    public static void reset() {
        set(DEFAULT_MANA_COST_PER_TICK);
    }
}
