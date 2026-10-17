package jp.aquafactory.apprenticecodex.item;

import net.minecraft.util.Mth;

public final class EquipmentSpellTimingConfigState {
    public static final double DEFAULT_CRAFTSMANS_DELIGHT_COOLDOWN_MULTIPLIER = 0.5D;
    public static final double DEFAULT_MAGI_AGENT_SUIT_BOOTS_COOLDOWN_MULTIPLIER = 0.5D;
    public static final double DEFAULT_MAGI_AGENT_SUIT_BOOTS_CAST_TIME_MULTIPLIER = 0.5D;

    private static double craftsmansDelightCooldownMultiplier =
            DEFAULT_CRAFTSMANS_DELIGHT_COOLDOWN_MULTIPLIER;
    private static double magiAgentSuitBootsCooldownMultiplier =
            DEFAULT_MAGI_AGENT_SUIT_BOOTS_COOLDOWN_MULTIPLIER;
    private static double magiAgentSuitBootsCastTimeMultiplier =
            DEFAULT_MAGI_AGENT_SUIT_BOOTS_CAST_TIME_MULTIPLIER;

    private static double magiAgentSuitAmmoNoConsumeChance = 0.5D;

    public static double magiAgentSuitAmmoNoConsumeChance() {
        return magiAgentSuitAmmoNoConsumeChance;
    }

    public static void setAmmoNoConsumeChance(double chance) {
        magiAgentSuitAmmoNoConsumeChance = Mth.clamp(chance, 0.0D, 1.0D);
    }

    private EquipmentSpellTimingConfigState() {
    }

    public static double craftsmansDelightCooldownMultiplier() {
        return craftsmansDelightCooldownMultiplier;
    }

    public static double magiAgentSuitBootsCooldownMultiplier() {
        return magiAgentSuitBootsCooldownMultiplier;
    }

    public static double magiAgentSuitBootsCastTimeMultiplier() {
        return magiAgentSuitBootsCastTimeMultiplier;
    }

    public static void set(
            double craftsmansDelightMultiplier,
            double magiAgentSuitBootsCooldownMultiplier,
            double magiAgentSuitBootsCastTimeMultiplier
    ) {
        craftsmansDelightCooldownMultiplier = Mth.clamp(craftsmansDelightMultiplier, 0.0D, 1.0D);
        EquipmentSpellTimingConfigState.magiAgentSuitBootsCooldownMultiplier =
                Mth.clamp(magiAgentSuitBootsCooldownMultiplier, 0.0D, 1.0D);
        EquipmentSpellTimingConfigState.magiAgentSuitBootsCastTimeMultiplier =
                Mth.clamp(magiAgentSuitBootsCastTimeMultiplier, 0.0D, 1.0D);
    }

    public static void reset() {
        setAmmoNoConsumeChance(0.5D);
        set(
                DEFAULT_CRAFTSMANS_DELIGHT_COOLDOWN_MULTIPLIER,
                DEFAULT_MAGI_AGENT_SUIT_BOOTS_COOLDOWN_MULTIPLIER,
                DEFAULT_MAGI_AGENT_SUIT_BOOTS_CAST_TIME_MULTIPLIER
        );
    }
}
