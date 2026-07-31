package jp.aquafactory.apprenticecodex.item.swingstaff;

import jp.aquafactory.apprenticecodex.config.item.HighTierSwingcastStaffServerConfig;

public final class HighTierSwingcastStaffConfigState {
    private static int diamondCooldownReductionTicks =
            HighTierSwingcastStaffServerConfig.DEFAULT_DIAMOND_COOLDOWN_REDUCTION_TICKS;
    private static int netheriteCooldownReductionTicks =
            HighTierSwingcastStaffServerConfig.DEFAULT_NETHERITE_COOLDOWN_REDUCTION_TICKS;

    private HighTierSwingcastStaffConfigState() {
    }

    public static int diamondCooldownReductionTicks() {
        return diamondCooldownReductionTicks;
    }

    public static int netheriteCooldownReductionTicks() {
        return netheriteCooldownReductionTicks;
    }

    public static void setCooldownReductionTicks(int diamondTicks, int netheriteTicks) {
        diamondCooldownReductionTicks = Math.max(0, diamondTicks);
        netheriteCooldownReductionTicks = Math.max(0, netheriteTicks);
    }

    public static void reset() {
        diamondCooldownReductionTicks =
                HighTierSwingcastStaffServerConfig.DEFAULT_DIAMOND_COOLDOWN_REDUCTION_TICKS;
        netheriteCooldownReductionTicks =
                HighTierSwingcastStaffServerConfig.DEFAULT_NETHERITE_COOLDOWN_REDUCTION_TICKS;
    }
}
