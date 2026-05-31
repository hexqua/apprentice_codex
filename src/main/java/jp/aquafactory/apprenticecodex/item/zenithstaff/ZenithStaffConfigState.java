package jp.aquafactory.apprenticecodex.item.zenithstaff;

public final class ZenithStaffConfigState {
    public static final float DEFAULT_MANA_COST_MULTIPLIER = 5.0F;

    private static float manaCostMultiplier = DEFAULT_MANA_COST_MULTIPLIER;

    private ZenithStaffConfigState() {
    }

    public static float manaCostMultiplier() {
        return manaCostMultiplier;
    }

    public static void setManaCostMultiplier(float manaCostMultiplier) {
        ZenithStaffConfigState.manaCostMultiplier = Math.max(1.0F, manaCostMultiplier);
    }

    public static void reset() {
        manaCostMultiplier = DEFAULT_MANA_COST_MULTIPLIER;
    }
}
