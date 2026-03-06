package jp.aquafactory.apprenticecodex.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ApprenticeCodexServerConfig {
    public static final ForgeConfigSpec SPEC;
    private static final DamageMultiplierServerConfig DAMAGE_MULTIPLIER_CONFIG;
    private static final BlocksServerConfig BLOCKS_CONFIG;
    private static final ItemsServerConfig ITEMS_CONFIG;
    private static final SpellsServerConfig SPELLS_CONFIG;

    static {
        var builder = new ForgeConfigSpec.Builder();
        DAMAGE_MULTIPLIER_CONFIG = DamageMultiplierServerConfig.define(builder, DamageMultiplierKey.values());
        BLOCKS_CONFIG = BlocksServerConfig.define(builder);
        ITEMS_CONFIG = ItemsServerConfig.define(builder);
        SPELLS_CONFIG = SpellsServerConfig.define(builder);
        SPEC = builder.build();
    }

    private ApprenticeCodexServerConfig() {
    }

    public static float damageMultiplier(DamageMultiplierKey key) {
        return DAMAGE_MULTIPLIER_CONFIG.value(key);
    }

    public static int arcanumInAJarTicksPerStoredParameter() {
        return BLOCKS_CONFIG.arcanumInAJarTicksPerStoredParameter();
    }

    public static boolean apprenticeDeskEnableSpellCraftBlacklist() {
        return BLOCKS_CONFIG.apprenticeDeskEnableSpellCraftBlacklist();
    }

    public static java.util.List<String> apprenticeDeskSpellCraftBlacklist() {
        return BLOCKS_CONFIG.apprenticeDeskSpellCraftBlacklist();
    }

    public static boolean apprenticeDeskRequireSameSchool() {
        return BLOCKS_CONFIG.apprenticeDeskRequireSameSchool();
    }

    public static boolean apprenticeDeskDisableCommonRarityConversion() {
        return BLOCKS_CONFIG.apprenticeDeskDisableCommonRarityConversion();
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
