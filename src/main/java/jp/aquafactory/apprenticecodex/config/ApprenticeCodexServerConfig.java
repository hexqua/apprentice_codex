package jp.aquafactory.apprenticecodex.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ApprenticeCodexServerConfig {
    public static final ForgeConfigSpec SPEC;
    private static final DamageMultiplierServerConfig DAMAGE_MULTIPLIER_CONFIG;
    private static final ItemsServerConfig ITEMS_CONFIG;

    static {
        var builder = new ForgeConfigSpec.Builder();
        DAMAGE_MULTIPLIER_CONFIG = DamageMultiplierServerConfig.define(builder, DamageMultiplierKey.values());
        ITEMS_CONFIG = ItemsServerConfig.define(builder);
        SPEC = builder.build();
    }

    private ApprenticeCodexServerConfig() {
    }

    public static float damageMultiplier(DamageMultiplierKey key) {
        return DAMAGE_MULTIPLIER_CONFIG.value(key);
    }

    public static float scarletThirstRequiredHealth() {
        return ITEMS_CONFIG.scarletThirstRequiredHealth();
    }

    public static float scarletThirstDrainHealth() {
        return ITEMS_CONFIG.scarletThirstDrainHealth();
    }

    public static float scarletThirstDrainEmergencyHealth() {
        return ITEMS_CONFIG.scarletThirstDrainEmergencyHealth();
    }

    public static float scarletThirstRecoverMana() {
        return ITEMS_CONFIG.scarletThirstRecoverMana();
    }

    public static float scarletThirstRecoverEmergencyMana() {
        return ITEMS_CONFIG.scarletThirstRecoverEmergencyMana();
    }

    public static boolean craftsmansDelightCanImbueEnchantment() {
        return ITEMS_CONFIG.craftsmansDelightCanImbueEnchantment();
    }

    public static float craftsmansDelightRequiredMana() {
        return ITEMS_CONFIG.craftsmansDelightRequiredMana();
    }

    public static int craftsmansDelightFortuneLevel() {
        return ITEMS_CONFIG.craftsmansDelightFortuneLevel();
    }

    public static double pastelStaffAmplifyTintedMagicMultiplier() {
        return ITEMS_CONFIG.pastelStaffAmplifyTintedMagicMultiplier();
    }

    public static boolean disableApprenticeDeskRecipe() {
        return ITEMS_CONFIG.disableApprenticeDeskRecipe();
    }
}
