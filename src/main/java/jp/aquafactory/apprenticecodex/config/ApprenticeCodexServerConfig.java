package jp.aquafactory.apprenticecodex.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ApprenticeCodexServerConfig {
    public static final ModConfigSpec SPEC;
    private static final DamageMultiplierServerConfig DAMAGE_MULTIPLIER_CONFIG;
    private static final ItemsServerConfig ITEMS_CONFIG;
    private static final SpellsServerConfig SPELLS_CONFIG;
    static {
        var builder = new ModConfigSpec.Builder();
        DAMAGE_MULTIPLIER_CONFIG = DamageMultiplierServerConfig.define(builder, DamageMultiplierKey.values());
        ITEMS_CONFIG = ItemsServerConfig.define(builder);
        SPELLS_CONFIG = SpellsServerConfig.define(builder);
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

    public static float forceFieldDrainManaBasePerHit() {
        return SPELLS_CONFIG.forceFieldDrainManaBasePerHit();
    }
}

