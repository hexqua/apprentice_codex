package jp.aquafactory.apprenticecodex.item.swingstaff;

import jp.aquafactory.apprenticecodex.config.item.SoulstainedSteelSwingcastStaffServerConfig;

public final class SoulstainedSteelSwingcastStaffConfigState {
    private static double manaCostPerBlade =
            SoulstainedSteelSwingcastStaffServerConfig.DEFAULT_MANA_COST_PER_BLADE;

    private SoulstainedSteelSwingcastStaffConfigState() {
    }

    public static double manaCostPerBlade() {
        return manaCostPerBlade;
    }

    public static void setManaCostPerBlade(double value) {
        manaCostPerBlade = Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    public static void reset() {
        manaCostPerBlade = SoulstainedSteelSwingcastStaffServerConfig.DEFAULT_MANA_COST_PER_BLADE;
    }
}
