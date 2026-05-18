package jp.aquafactory.apprenticecodex.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ApprenticeCodexServerConfig {
    public static final ModConfigSpec SPEC;
    private static final DamageMultiplierServerConfig DAMAGE_MULTIPLIER_CONFIG;
    private static final BlocksServerConfig BLOCKS_CONFIG;
    private static final ItemsServerConfig ITEMS_CONFIG;
    private static final LootServerConfig LOOT_CONFIG;
    private static final SpellsServerConfig SPELLS_CONFIG;
    static {
        var builder = new ModConfigSpec.Builder();
        DAMAGE_MULTIPLIER_CONFIG = DamageMultiplierServerConfig.define(builder, DamageMultiplierKey.values());
        BLOCKS_CONFIG = BlocksServerConfig.define(builder);
        ITEMS_CONFIG = ItemsServerConfig.define(builder);
        LOOT_CONFIG = LootServerConfig.define(builder);
        SPELLS_CONFIG = SpellsServerConfig.define(builder);
        SPEC = builder.build();
    }

    private ApprenticeCodexServerConfig() {
    }

    public static float damageMultiplier(DamageMultiplierKey key) {
        return DAMAGE_MULTIPLIER_CONFIG.value(key);
    }

    public static boolean limitArcaneCinderSpeedupToVanillaFurnaces() {
        return ITEMS_CONFIG.limitArcaneCinderSpeedupToVanillaFurnaces();
    }

    public static int arcanumInAJarTicksPerStoredParameter() {
        return BLOCKS_CONFIG.arcanumInAJarTicksPerStoredParameter();
    }

    public static boolean spellDispenserIgnoreSpellProfileAndDenylistFiles() {
        return BLOCKS_CONFIG.spellDispenserIgnoreSpellProfileAndDenylistFiles();
    }

    public static boolean apprenticeDeskEnableSpellCraftBlacklist() {
        return BLOCKS_CONFIG.apprenticeDeskEnableSpellCraftBlacklist();
    }

    public static boolean apprenticeDeskDisableNonJobSiteFeatures() {
        return BLOCKS_CONFIG.apprenticeDeskDisableNonJobSiteFeatures();
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

    public static int absorptionAmplifyAmuletRecoveryDelayTicks() {
        return ITEMS_CONFIG.absorptionAmplifyAmuletRecoveryDelayTicks();
    }

    public static float absorptionAmplifyAmuletBaseAbsorptionTarget() {
        return ITEMS_CONFIG.absorptionAmplifyAmuletBaseAbsorptionTarget();
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

    public static double apprenticeMageRobeSpellPowerBonusPerPiece() {
        return ITEMS_CONFIG.apprenticeMageRobeSpellPowerBonusPerPiece();
    }

    public static double enchantressRobeSpellPowerBonusPerPiece() {
        return ITEMS_CONFIG.enchantressRobeSpellPowerBonusPerPiece();
    }

    public static double chromaticMagiaDressSpellPowerBonusPerPiece() {
        return ITEMS_CONFIG.chromaticMagiaDressSpellPowerBonusPerPiece();
    }

    public static double stealthRuneArmorSpellPowerBonusPerPiece() {
        return ITEMS_CONFIG.stealthRuneArmorSpellPowerBonusPerPiece();
    }

    public static double chromaticMagiaDressSchoolSpellPowerBonusPerHistory() {
        return ITEMS_CONFIG.chromaticMagiaDressSchoolSpellPowerBonusPerHistory();
    }

    public static double pastelStaffAmplifyTintedMagicMultiplier() {
        return ITEMS_CONFIG.pastelStaffAmplifyTintedMagicMultiplier();
    }

    public static boolean showIsekaiTravelGuidebookTooltip() {
        return ITEMS_CONFIG.showIsekaiTravelGuidebookTooltip();
    }

    public static boolean enableIsekaiTravelGuidebookBonusChestLoot() {
        return ITEMS_CONFIG.enableIsekaiTravelGuidebookBonusChestLoot();
    }

    public static boolean enableApprenticeCurioLoot() {
        return LOOT_CONFIG.enableApprenticeCurioLoot();
    }

    public static double apprenticeCurioLootChanceMultiplier() {
        return LOOT_CONFIG.apprenticeCurioLootChanceMultiplier();
    }

    public static float manaForceBladeImbueDamageMultiplierScale() {
        return ITEMS_CONFIG.manaForceBladeImbueDamageMultiplierScale();
    }

    public static float manaForceBladeAttackManaCostMultiplier() {
        return ITEMS_CONFIG.manaForceBladeAttackManaCostMultiplier();
    }

    public static float manaForceBladeAttackManaSchoolMultiplierScale() {
        return ITEMS_CONFIG.manaForceBladeAttackManaSchoolMultiplierScale();
    }

    public static float manaForceBladeMeleeGuardManaCost() {
        return ITEMS_CONFIG.manaForceBladeMeleeGuardManaCost();
    }

    public static float manaForceBladeRangedGuardManaCost() {
        return ITEMS_CONFIG.manaForceBladeRangedGuardManaCost();
    }

    public static boolean manaForceBladeDisableManaRecoveryWhileGuarding() {
        return ITEMS_CONFIG.manaForceBladeDisableManaRecoveryWhileGuarding();
    }

    public static int manaForceBladePerfectGuardTicks() {
        return ITEMS_CONFIG.manaForceBladePerfectGuardTicks();
    }

    public static int multipurposeStaffrifleCooldownBypassThresholdTicks() {
        return ITEMS_CONFIG.multipurposeStaffrifleCooldownBypassThresholdTicks();
    }

    public static int multipurposeStaffrifleCooldownReductionTicks() {
        return ITEMS_CONFIG.multipurposeStaffrifleCooldownReductionTicks();
    }

    public static int multipurposeStaffrifleReducedCooldownMinimumTicks() {
        return ITEMS_CONFIG.multipurposeStaffrifleReducedCooldownMinimumTicks();
    }

    public static int multipurposeStaffrifleAdsFullAutoIntervalTicks() {
        return ITEMS_CONFIG.multipurposeStaffrifleAdsFullAutoIntervalTicks();
    }

    public static float forceFieldDrainManaBasePerHit() {
        return SPELLS_CONFIG.forceFieldDrainManaBasePerHit();
    }

    public static boolean autoMagnetDisableCollectManaCost() {
        return SPELLS_CONFIG.autoMagnetDisableCollectManaCost();
    }
}

