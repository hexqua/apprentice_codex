package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.config.item.SpellStainedRunicTabletServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ArchivistsGrimoireServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellchargedGreatswordServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellgunServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellThrowableCardServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ChargecastCatalystbookServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.LinearBuildServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeSettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class ApprenticeCodexServerConfig {
    public interface GameTestConfigOverride extends AutoCloseable {
        @Override
        void close();
    }

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.IntValue SAVED_ABSOLUTE_TICK_CLAMP_MAX_TICKS;
    private static final DamageMultiplierServerConfig DAMAGE_MULTIPLIER_CONFIG;
    private static final AttributeEnchantmentsServerConfig ATTRIBUTE_ENCHANTMENTS_CONFIG;
    private static final BlocksServerConfig BLOCKS_CONFIG;
    private static final ItemsServerConfig ITEMS_CONFIG;
    private static final LootServerConfig LOOT_CONFIG;
    private static final SpellsServerConfig SPELLS_CONFIG;
    private static final WorldgenServerConfig WORLDGEN_CONFIG;
    private static final ProcessingServerConfig PROCESSING_CONFIG;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("Compatibility");
        SAVED_ABSOLUTE_TICK_CLAMP_MAX_TICKS = builder
                .comment("Maximum ticks kept when repairing persisted absolute game-time values that are far in the future.")
                .defineInRange("savedAbsoluteTickClampMaxTicks", 20 * 60 * 5, 0, Integer.MAX_VALUE);
        builder.pop();
        DAMAGE_MULTIPLIER_CONFIG = DamageMultiplierServerConfig.define(builder, DamageMultiplierKey.values());
        ATTRIBUTE_ENCHANTMENTS_CONFIG =
                AttributeEnchantmentsServerConfig.define(builder, AttributeEnchantmentType.values());
        BLOCKS_CONFIG = BlocksServerConfig.define(builder);
        ITEMS_CONFIG = ItemsServerConfig.define(builder);
        LOOT_CONFIG = LootServerConfig.define(builder);
        SPELLS_CONFIG = SpellsServerConfig.define(builder);
        WORLDGEN_CONFIG = WorldgenServerConfig.define(builder);
        PROCESSING_CONFIG = ProcessingServerConfig.define(builder);
        SPEC = builder.build();
    }

    private ApprenticeCodexServerConfig() {
    }

    public static int savedAbsoluteTickClampMaxTicks() {
        return SAVED_ABSOLUTE_TICK_CLAMP_MAX_TICKS.get();
    }

    public static float damageMultiplier(DamageMultiplierKey key) {
        return DAMAGE_MULTIPLIER_CONFIG.value(key);
    }

    public static GameTestConfigOverride useDamageMultiplierOverrideForGameTest(DamageMultiplierKey key, double value) {
        var previousValue = DAMAGE_MULTIPLIER_CONFIG.value(key);
        DAMAGE_MULTIPLIER_CONFIG.setValueForGameTest(key, value);
        return () -> DAMAGE_MULTIPLIER_CONFIG.setValueForGameTest(key, previousValue);
    }

    public static double attributeEnchantmentAmountPerLevel(AttributeEnchantmentType type) {
        return ATTRIBUTE_ENCHANTMENTS_CONFIG.amountPerLevel(type);
    }

    public static GameTestConfigOverride useAttributeEnchantmentAmountPerLevelOverrideForGameTest(
            AttributeEnchantmentType type,
            double value
    ) {
        var previousValue = ATTRIBUTE_ENCHANTMENTS_CONFIG.amountPerLevel(type);
        ATTRIBUTE_ENCHANTMENTS_CONFIG.setAmountPerLevelForGameTest(type, value);
        return () -> ATTRIBUTE_ENCHANTMENTS_CONFIG.setAmountPerLevelForGameTest(type, previousValue);
    }

    public static boolean isSpellcasterWorkbenchRecipeDenied(ResourceLocation recipeId) {
        return PROCESSING_CONFIG.isSpellcasterWorkbenchRecipeDenied(recipeId);
    }

    public static boolean isEssenceSmokerRecipeDenied(ResourceLocation recipeId) {
        return PROCESSING_CONFIG.isEssenceSmokerRecipeDenied(recipeId);
    }

    public static boolean isGrindRunnerRecipeDenied(ResourceLocation recipeId) {
        return PROCESSING_CONFIG.isGrindRunnerRecipeDenied(recipeId);
    }

    public static boolean isHeavenlyFistCreateRecipeDenied(ResourceLocation recipeId) {
        return PROCESSING_CONFIG.isHeavenlyFistCreateRecipeDenied(recipeId);
    }

    public static boolean isThermalProcessRecipeDenied(ResourceLocation recipeId) {
        return PROCESSING_CONFIG.isThermalProcessRecipeDenied(recipeId);
    }

    public static GameTestConfigOverride useProcessingRecipeDenylistOverrideForGameTest(
            List<String> spellcasterWorkbenchRecipeDenylist,
            List<String> essenceSmokerRecipeDenylist,
            List<String> grindRunnerRecipeDenylist,
            List<String> heavenlyFistCreateRecipeDenylist,
            List<String> thermalProcessRecipeDenylist
    ) {
        var previousSpellcasterWorkbenchRecipeDenylist = PROCESSING_CONFIG.spellcasterWorkbenchRecipeDenylist();
        var previousEssenceSmokerRecipeDenylist = PROCESSING_CONFIG.essenceSmokerRecipeDenylist();
        var previousGrindRunnerRecipeDenylist = PROCESSING_CONFIG.grindRunnerRecipeDenylist();
        var previousHeavenlyFistCreateRecipeDenylist = PROCESSING_CONFIG.heavenlyFistCreateRecipeDenylist();
        var previousThermalProcessRecipeDenylist = PROCESSING_CONFIG.thermalProcessRecipeDenylist();

        PROCESSING_CONFIG.setRecipeDenylistsForGameTest(
                spellcasterWorkbenchRecipeDenylist,
                essenceSmokerRecipeDenylist,
                grindRunnerRecipeDenylist,
                heavenlyFistCreateRecipeDenylist,
                thermalProcessRecipeDenylist
        );
        return () -> PROCESSING_CONFIG.setRecipeDenylistsForGameTest(
                previousSpellcasterWorkbenchRecipeDenylist,
                previousEssenceSmokerRecipeDenylist,
                previousGrindRunnerRecipeDenylist,
                previousHeavenlyFistCreateRecipeDenylist,
                previousThermalProcessRecipeDenylist
        );
    }

    public static boolean limitArcaneCinderSpeedupToVanillaFurnaces() {
        return ITEMS_CONFIG.limitArcaneCinderSpeedupToVanillaFurnaces();
    }

    public static int arcanumInAJarTicksPerStoredParameter() {
        return BLOCKS_CONFIG.arcanumInAJarTicksPerStoredParameter();
    }

    public static boolean spellDispenserEnable() {
        return BLOCKS_CONFIG.spellDispenserEnable();
    }

    public static boolean isSpellDispenserSpellAllowedByServerAllowlist(ResourceLocation spellId) {
        return BLOCKS_CONFIG.spellDispenserIsSpellAllowedByServerAllowlist(spellId);
    }

    public static double spellDispenserCooldownMultiplier() {
        return BLOCKS_CONFIG.spellDispenserCooldownMultiplier();
    }

    public static boolean creativeSpellDispenserManaConsumption() {
        return BLOCKS_CONFIG.creativeSpellDispenserManaConsumption();
    }

    public static double creativeSpellDispenserCooldownMultiplier() {
        return BLOCKS_CONFIG.creativeSpellDispenserCooldownMultiplier();
    }

    public static GameTestConfigOverride useSpellDispenserConfigOverrideForGameTest(
            boolean enable,
            boolean enableSpellAllowlist,
            List<String> spellAllowlist,
            double cooldownMultiplier
    ) {
        var previousEnable = BLOCKS_CONFIG.spellDispenserEnable();
        var previousEnableSpellAllowlist = BLOCKS_CONFIG.spellDispenserEnableSpellAllowlist();
        var previousSpellAllowlist = BLOCKS_CONFIG.spellDispenserSpellAllowlist();
        var previousCooldownMultiplier = BLOCKS_CONFIG.spellDispenserCooldownMultiplier();

        BLOCKS_CONFIG.setSpellDispenserConfigForGameTest(
                enable,
                enableSpellAllowlist,
                spellAllowlist,
                cooldownMultiplier
        );
        return () -> BLOCKS_CONFIG.setSpellDispenserConfigForGameTest(
                previousEnable,
                previousEnableSpellAllowlist,
                previousSpellAllowlist,
                previousCooldownMultiplier
        );
    }

    public static GameTestConfigOverride useCreativeSpellDispenserConfigOverrideForGameTest(
            boolean manaConsumption,
            double cooldownMultiplier
    ) {
        var previousManaConsumption = BLOCKS_CONFIG.creativeSpellDispenserManaConsumption();
        var previousCooldownMultiplier = BLOCKS_CONFIG.creativeSpellDispenserCooldownMultiplier();

        BLOCKS_CONFIG.setCreativeSpellDispenserConfigForGameTest(manaConsumption, cooldownMultiplier);
        return () -> BLOCKS_CONFIG.setCreativeSpellDispenserConfigForGameTest(
                previousManaConsumption,
                previousCooldownMultiplier
        );
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

    public static boolean isSatelliteFollowcastAmuletSpellDenied(ResourceLocation spellId) {
        return ITEMS_CONFIG.isSatelliteFollowcastAmuletSpellDenied(spellId);
    }

    public static List<String> satelliteFollowcastAmuletSpellDenylist() {
        return ITEMS_CONFIG.satelliteFollowcastAmuletSpellDenylist();
    }

    public static boolean remoteOwnerCastEnableRemotePlayerGeometry() {
        return ITEMS_CONFIG.remoteOwnerCastEnableRemotePlayerGeometry();
    }

    public static boolean isRemoteOwnerCastSpellDenied(ResourceLocation spellId) {
        return ITEMS_CONFIG.isRemoteOwnerCastSpellDenied(spellId);
    }

    public static List<String> remoteOwnerCastDenylist() {
        return ITEMS_CONFIG.remoteOwnerCastDenylist();
    }

    public static int luminousDeviceMaxStoredItems() {
        return ITEMS_CONFIG.luminousDeviceMaxStoredItems();
    }

    public static int luminousDeviceMaxStoredMana() {
        return ITEMS_CONFIG.luminousDeviceMaxStoredMana();
    }

    public static int luminousDeviceUpgradedMaxStoredMana() {
        return ITEMS_CONFIG.luminousDeviceUpgradedMaxStoredMana();
    }

    public static int luminousDeviceCleanRadius() {
        return ITEMS_CONFIG.luminousDeviceCleanRadius();
    }

    public static int luminousDeviceMageLightManaRecovery() {
        return ITEMS_CONFIG.luminousDeviceMageLightManaRecovery();
    }

    public static int luminousDeviceWizardlampManaRecovery() {
        return ITEMS_CONFIG.luminousDeviceWizardlampManaRecovery();
    }

    public static double luminousDeviceMageLightExtendedRange() {
        return ITEMS_CONFIG.luminousDeviceMageLightExtendedRange();
    }

    public static GameTestConfigOverride useLuminousDeviceMageLightExtendedRangeOverrideForGameTest(double range) {
        var previousRange = ITEMS_CONFIG.luminousDeviceMageLightExtendedRange();
        ITEMS_CONFIG.setLuminousDeviceMageLightExtendedRangeForGameTest(range);
        return () -> ITEMS_CONFIG.setLuminousDeviceMageLightExtendedRangeForGameTest(previousRange);
    }

    public static double mageLightMaxRange() {
        return SPELLS_CONFIG.mageLightMaxRange();
    }

    public static GameTestConfigOverride useMageLightMaxRangeOverrideForGameTest(double maxRange) {
        var previousMaxRange = SPELLS_CONFIG.mageLightMaxRange();
        SPELLS_CONFIG.setMageLightMaxRangeForGameTest(maxRange);
        return () -> SPELLS_CONFIG.setMageLightMaxRangeForGameTest(previousMaxRange);
    }

    public static GameTestConfigOverride useLuminousDeviceConfigOverrideForGameTest(
            int maxStoredItems,
            int maxStoredMana
    ) {
        var previousMaxStoredItems = ITEMS_CONFIG.luminousDeviceMaxStoredItems();
        var previousMaxStoredMana = ITEMS_CONFIG.luminousDeviceMaxStoredMana();
        ITEMS_CONFIG.setLuminousDeviceConfigForGameTest(maxStoredItems, maxStoredMana);
        return () -> ITEMS_CONFIG.setLuminousDeviceConfigForGameTest(
                previousMaxStoredItems,
                previousMaxStoredMana
        );
    }

    public static GameTestConfigOverride useLuminousDeviceCleanConfigOverrideForGameTest(
            int cleanRadius,
            int mageLightManaRecovery,
            int wizardlampManaRecovery
    ) {
        var previousCleanRadius = ITEMS_CONFIG.luminousDeviceCleanRadius();
        var previousMageLightManaRecovery = ITEMS_CONFIG.luminousDeviceMageLightManaRecovery();
        var previousWizardlampManaRecovery = ITEMS_CONFIG.luminousDeviceWizardlampManaRecovery();
        ITEMS_CONFIG.setLuminousDeviceCleanConfigForGameTest(
                cleanRadius,
                mageLightManaRecovery,
                wizardlampManaRecovery
        );
        return () -> ITEMS_CONFIG.setLuminousDeviceCleanConfigForGameTest(
                previousCleanRadius,
                previousMageLightManaRecovery,
                previousWizardlampManaRecovery
        );
    }

    public static GameTestConfigOverride useSatelliteFollowcastAmuletSpellDenylistOverrideForGameTest(
            List<String> spellDenylist
    ) {
        var previousSpellDenylist = ITEMS_CONFIG.satelliteFollowcastAmuletSpellDenylist();
        ITEMS_CONFIG.setSatelliteFollowcastAmuletSpellDenylistForGameTest(spellDenylist);
        return () -> ITEMS_CONFIG.setSatelliteFollowcastAmuletSpellDenylistForGameTest(previousSpellDenylist);
    }

    public static GameTestConfigOverride useRemoteOwnerCastConfigOverrideForGameTest(
            boolean enableRemotePlayerGeometry,
            List<String> remoteOwnerCastDenylist
    ) {
        var previousEnableRemotePlayerGeometry = ITEMS_CONFIG.remoteOwnerCastEnableRemotePlayerGeometry();
        var previousRemoteOwnerCastDenylist = ITEMS_CONFIG.remoteOwnerCastDenylist();

        ITEMS_CONFIG.setRemoteOwnerCastConfigForGameTest(
                enableRemotePlayerGeometry,
                remoteOwnerCastDenylist
        );
        return () -> ITEMS_CONFIG.setRemoteOwnerCastConfigForGameTest(
                previousEnableRemotePlayerGeometry,
                previousRemoteOwnerCastDenylist
        );
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

    public static GameTestConfigOverride useAbsorptionAmplifyAmuletConfigOverrideForGameTest(
            double baseAbsorptionTarget,
            int recoveryDelayTicks
    ) {
        var previousBaseAbsorptionTarget = ITEMS_CONFIG.absorptionAmplifyAmuletBaseAbsorptionTarget();
        var previousRecoveryDelayTicks = ITEMS_CONFIG.absorptionAmplifyAmuletRecoveryDelayTicks();

        ITEMS_CONFIG.setAbsorptionAmplifyAmuletConfigForGameTest(baseAbsorptionTarget, recoveryDelayTicks);
        return () -> ITEMS_CONFIG.setAbsorptionAmplifyAmuletConfigForGameTest(
                previousBaseAbsorptionTarget,
                previousRecoveryDelayTicks
        );
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

    public static double craftsmansDelightCooldownMultiplier() {
        return ITEMS_CONFIG.craftsmansDelightCooldownMultiplier();
    }

    public static boolean isCraftsmansDelightGracedRainGrowthDenied(ResourceLocation entityTypeId) {
        return ITEMS_CONFIG.isCraftsmansDelightGracedRainGrowthDenied(entityTypeId);
    }

    public static boolean isCraftsmansDelightGracedRainBreedingCooldownDenied(ResourceLocation entityTypeId) {
        return ITEMS_CONFIG.isCraftsmansDelightGracedRainBreedingCooldownDenied(entityTypeId);
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

    public static double elementMaidenRobeSpellPowerBonus() {
        return ITEMS_CONFIG.elementMaidenRobeSpellPowerBonus();
    }

    public static double elementMaidenRobeSchoolSpellPowerBonus() {
        return ITEMS_CONFIG.elementMaidenRobeSchoolSpellPowerBonus();
    }

    public static double magiAgentSuitSpellPowerBonus() {
        return ITEMS_CONFIG.magiAgentSuitSpellPowerBonus();
    }

    public static double magiAgentSuitSchoolSpellPowerBonus() {
        return ITEMS_CONFIG.magiAgentSuitSchoolSpellPowerBonus();
    }

    public static double magiAgentSuitBootsCooldownMultiplier() {
        return ITEMS_CONFIG.magiAgentSuitBootsCooldownMultiplier();
    }

    public static double magiAgentSuitBootsCastTimeMultiplier() {
        return ITEMS_CONFIG.magiAgentSuitBootsCastTimeMultiplier();
    }

    public static double magiAgentSuitAmmoNoConsumeChance() {
        return ITEMS_CONFIG.magiAgentSuitAmmoNoConsumeChance();
    }

    public static boolean magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed() {
        return ITEMS_CONFIG.magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed();
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

    public static float zenithStaffManaCostMultiplier() {
        return ITEMS_CONFIG.zenithStaffManaCostMultiplier();
    }

    public static boolean showIsekaiTravelGuidebookTooltip() {
        return ITEMS_CONFIG.showIsekaiTravelGuidebookTooltip();
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

    public static int manaForceBladeReleaseCooldownTicks() {
        return ITEMS_CONFIG.manaForceBladeReleaseCooldownTicks();
    }

    public static int manaForceBladePerfectGuardReleaseCooldownGraceTicks() {
        return ITEMS_CONFIG.manaForceBladePerfectGuardReleaseCooldownGraceTicks();
    }

    public static int manaForceBladePerfectGuardReleaseCooldownGraceUses() {
        return ITEMS_CONFIG.manaForceBladePerfectGuardReleaseCooldownGraceUses();
    }

    public static int parrycastBucklerReleaseCooldownTicks() {
        return ITEMS_CONFIG.parrycastBucklerReleaseCooldownTicks();
    }

    public static int parrycastBucklerPerfectGuardTicks() {
        return ITEMS_CONFIG.parrycastBucklerPerfectGuardTicks();
    }

    public static int parrycastBucklerPerfectGuardReleaseCooldownGraceTicks() {
        return ITEMS_CONFIG.parrycastBucklerPerfectGuardReleaseCooldownGraceTicks();
    }

    public static int parrycastBucklerPerfectGuardReleaseCooldownGraceUses() {
        return ITEMS_CONFIG.parrycastBucklerPerfectGuardReleaseCooldownGraceUses();
    }

    public static double bulwarkGreatshieldGenericSpellResist() {
        return ITEMS_CONFIG.bulwarkGreatshieldGenericSpellResist();
    }

    public static double bulwarkGreatshieldSchoolSpellResist() {
        return ITEMS_CONFIG.bulwarkGreatshieldSchoolSpellResist();
    }

    public static GameTestConfigOverride useBulwarkGreatshieldConfigOverrideForGameTest(
            double genericSpellResist,
            double schoolSpellResist
    ) {
        var previousGenericSpellResist = ITEMS_CONFIG.bulwarkGreatshieldGenericSpellResist();
        var previousSchoolSpellResist = ITEMS_CONFIG.bulwarkGreatshieldSchoolSpellResist();
        ITEMS_CONFIG.setBulwarkGreatshieldConfigForGameTest(genericSpellResist, schoolSpellResist);
        return () -> ITEMS_CONFIG.setBulwarkGreatshieldConfigForGameTest(
                previousGenericSpellResist,
                previousSchoolSpellResist
        );
    }

    public static float manaShieldCharmManaPerDamage() {
        return ITEMS_CONFIG.manaShieldCharmManaPerDamage();
    }

    public static float magiCompressorGadgetManaCostPerSecond() {
        return ITEMS_CONFIG.magiCompressorGadgetManaCostPerSecond();
    }

    public static float magiCompressorGadgetAirFillPerSecond() {
        return ITEMS_CONFIG.magiCompressorGadgetAirFillPerSecond();
    }

    public static float magiCompressorGadgetMaxAir() {
        return ITEMS_CONFIG.magiCompressorGadgetMaxAir();
    }

    public static int manaShieldCharmRecoveryThresholdMana() {
        return ITEMS_CONFIG.manaShieldCharmRecoveryThresholdMana();
    }

    public static float manaShieldCharmSynchronizationManaPerDamage() {
        return ITEMS_CONFIG.manaShieldCharmSynchronizationManaPerDamage();
    }

    public static float manaShieldCharmNeutralizationRecoverManaPerDamage() {
        return ITEMS_CONFIG.manaShieldCharmNeutralizationRecoverManaPerDamage();
    }

    public static int manaShieldCharmShellArmorDurabilityDamage() {
        return ITEMS_CONFIG.manaShieldCharmShellArmorDurabilityDamage();
    }

    public static int manaShieldCharmInvulnerableTimeTicks() {
        return ITEMS_CONFIG.manaShieldCharmInvulnerableTimeTicks();
    }

    public static float manaThrusterManaCostPerTick() {
        return ITEMS_CONFIG.manaThrusterManaCostPerTick();
    }

    public static int multicastEchoStaffDelayTicks() {
        return ITEMS_CONFIG.multicastEchoStaffDelayTicks();
    }

    public static int circuitHeatStaffAdditionalManaReferenceCooldownTicks() {
        return ITEMS_CONFIG.circuitHeatStaffAdditionalManaReferenceCooldownTicks();
    }

    public static float circuitHeatStaffAdditionalManaLinearMultiplier() {
        return ITEMS_CONFIG.circuitHeatStaffAdditionalManaLinearMultiplier();
    }

    public static float circuitHeatStaffAdditionalManaQuadraticMultiplier() {
        return ITEMS_CONFIG.circuitHeatStaffAdditionalManaQuadraticMultiplier();
    }

    public static int circuitHeatStaffCooldownBypassMaxRemainingTicks() {
        return ITEMS_CONFIG.circuitHeatStaffCooldownBypassMaxRemainingTicks();
    }

    public static int ironSpellgunMaxInstantImbueCooldownTicks() {
        return ITEMS_CONFIG.ironSpellgunMaxInstantImbueCooldownTicks();
    }

    public static int ironSpellgunOverriddenSpellCooldownTicks() {
        return ITEMS_CONFIG.ironSpellgunOverriddenSpellCooldownTicks();
    }

    public static int copperSpellgunMaxInstantImbueCooldownTicks() {
        return ITEMS_CONFIG.copperSpellgunMaxInstantImbueCooldownTicks();
    }

    public static int copperSpellgunOverriddenSpellCooldownTicks() {
        return ITEMS_CONFIG.copperSpellgunOverriddenSpellCooldownTicks();
    }

    public static int goldSpellgunReducedCooldownMinimumTicks() {
        return ITEMS_CONFIG.goldSpellgunReducedCooldownMinimumTicks();
    }

    public static int goldSpellgunCooldownReductionTicks() {
        return ITEMS_CONFIG.goldSpellgunCooldownReductionTicks();
    }

    public static boolean isCircuitHeatStaffSpellDenied(ResourceLocation spellId) {
        return ITEMS_CONFIG.isCircuitHeatStaffSpellDenied(spellId);
    }

    public static double circuitHeatStaffOverheatDurationMultiplier() {
        return ITEMS_CONFIG.circuitHeatStaffOverheatDurationMultiplier();
    }

    public static int circuitHeatStaffOverheatDurationMinTicks() {
        return ITEMS_CONFIG.circuitHeatStaffOverheatDurationMinTicks();
    }

    public static int circuitHeatStaffOverheatDurationCapTicks() {
        return ITEMS_CONFIG.circuitHeatStaffOverheatDurationCapTicks();
    }

    public static boolean circuitHeatStaffDropCoolingEnabled() {
        return ITEMS_CONFIG.circuitHeatStaffDropCoolingEnabled();
    }

    public static int circuitHeatStaffDropCoolingProcessIntervalTicks() {
        return ITEMS_CONFIG.circuitHeatStaffDropCoolingProcessIntervalTicks();
    }

    public static int circuitHeatStaffDropCoolingReductionTicks() {
        return ITEMS_CONFIG.circuitHeatStaffDropCoolingReductionTicks();
    }

    public static int circuitHeatStaffDropCoolingWaterConsumeProcessCount() {
        return ITEMS_CONFIG.circuitHeatStaffDropCoolingWaterConsumeProcessCount();
    }

    public static boolean circuitHeatStaffConsumeWaterSourceOnCooling() {
        return ITEMS_CONFIG.circuitHeatStaffConsumeWaterSourceOnCooling();
    }

    public static boolean circuitHeatStaffConsumeWaterCauldronOnCooling() {
        return ITEMS_CONFIG.circuitHeatStaffConsumeWaterCauldronOnCooling();
    }

    public static double multicastEchoStaffCooldownMultiplier() {
        return ITEMS_CONFIG.multicastEchoStaffCooldownMultiplier();
    }

    public static double multicastEchoStaffCastTimeCooldownMultiplier() {
        return ITEMS_CONFIG.multicastEchoStaffCastTimeCooldownMultiplier();
    }

    public static int multicastEchoStaffCooldownCapTicks() {
        return ITEMS_CONFIG.multicastEchoStaffCooldownCapTicks();
    }

    public static int multicastEchoStaffMaxMulticastCount() {
        return ITEMS_CONFIG.multicastEchoStaffMaxMulticastCount();
    }

    public static boolean multicastEchoStaffMobEffectProfilesEnabled() {
        return ITEMS_CONFIG.multicastEchoStaffMobEffectProfilesEnabled();
    }

    public static boolean multicastEchoStaffBeneficialMobEffectsEnabled() {
        return ITEMS_CONFIG.multicastEchoStaffBeneficialMobEffectsEnabled();
    }

    public static boolean multicastEchoStaffHarmfulMobEffectsEnabled() {
        return ITEMS_CONFIG.multicastEchoStaffHarmfulMobEffectsEnabled();
    }

    public static boolean multicastEchoStaffNeutralMobEffectsEnabled() {
        return ITEMS_CONFIG.multicastEchoStaffNeutralMobEffectsEnabled();
    }

    public static boolean multicastEchoStaffDurationServerCapEnabled() {
        return ITEMS_CONFIG.multicastEchoStaffDurationServerCapEnabled();
    }

    public static int multicastEchoStaffDurationServerCapTicks() {
        return ITEMS_CONFIG.multicastEchoStaffDurationServerCapTicks();
    }

    public static boolean multicastEchoStaffAmplifierServerCapEnabled() {
        return ITEMS_CONFIG.multicastEchoStaffAmplifierServerCapEnabled();
    }

    public static int multicastEchoStaffAmplifierServerCap() {
        return ITEMS_CONFIG.multicastEchoStaffAmplifierServerCap();
    }

    public static boolean multicastEchoStaffAttackProfilesEnabled() {
        return ITEMS_CONFIG.multicastEchoStaffAttackProfilesEnabled();
    }

    public static double multicastEchoStaffRepeatDamageMultiplier() {
        return ITEMS_CONFIG.multicastEchoStaffRepeatDamageMultiplier();
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

    public static boolean isMultipurposeStaffrifleSpellDenied(ResourceLocation spellId) {
        return ITEMS_CONFIG.isMultipurposeStaffrifleSpellDenied(spellId);
    }

    public static boolean focusStaffbowEnableContinuousFocusedCast() {
        return ITEMS_CONFIG.focusStaffbowEnableContinuousFocusedCast();
    }

    public static boolean focusStaffbowEnableManaLoan() {
        return ITEMS_CONFIG.focusStaffbowEnableManaLoan();
    }

    public static boolean focusStaffbowEnableArrowCatalystRequirement() {
        return ITEMS_CONFIG.focusStaffbowEnableArrowCatalystRequirement();
    }

    public static List<ResourceLocation> focusStaffbowArrowCatalystItemIds() {
        return ITEMS_CONFIG.focusStaffbowArrowCatalystItemIds();
    }

    public static double focusStaffbowPendingMaxLoanManaRatio() {
        return ITEMS_CONFIG.focusStaffbowPendingMaxLoanManaRatio();
    }

    public static boolean isFocusStaffbowSpellDenied(ResourceLocation spellId) {
        return ITEMS_CONFIG.isFocusStaffbowSpellDenied(spellId);
    }

    public static boolean isFocusStaffbowSpellAllowed(ResourceLocation spellId) {
        return ITEMS_CONFIG.isFocusStaffbowSpellAllowed(spellId);
    }

    public static boolean focusStaffbowEnableSpellAllowlist() {
        return ITEMS_CONFIG.focusStaffbowEnableSpellAllowlist();
    }

    public static FocusStaffbowChargeSettings focusStaffbowChargeSettings() {
        return ITEMS_CONFIG.focusStaffbowChargeSettings();
    }

    public static ChargecastCatalystbookServerConfig.Values chargecastCatalystbookConfig() {
        return ITEMS_CONFIG.chargecastCatalystbookConfig();
    }

    public static double elementalBowMagicReadyDrawTicksMultiplier() {
        return ITEMS_CONFIG.elementalBowMagicReadyDrawTicksMultiplier();
    }

    public static List<ResourceLocation> elementalBowMagicArrowCatalystItemIds() {
        return ITEMS_CONFIG.elementalBowMagicArrowCatalystItemIds();
    }

    public static float elementalBowOverheatAdditionalManaLinearMultiplier() {
        return ITEMS_CONFIG.elementalBowOverheatAdditionalManaLinearMultiplier();
    }

    public static float elementalBowOverheatAdditionalManaQuadraticMultiplier() {
        return ITEMS_CONFIG.elementalBowOverheatAdditionalManaQuadraticMultiplier();
    }

    public static double elementalBowOverheatDurationMultiplier() {
        return ITEMS_CONFIG.elementalBowOverheatDurationMultiplier();
    }

    public static int elementalBowOverheatDurationMinTicks() {
        return ITEMS_CONFIG.elementalBowOverheatDurationMinTicks();
    }

    public static int elementalBowOverheatDurationCapTicks() {
        return ITEMS_CONFIG.elementalBowOverheatDurationCapTicks();
    }

    public static double elementalBowPowerArrowSpellLevelBonusPerLevel() {
        return ITEMS_CONFIG.elementalBowPowerArrowSpellLevelBonusPerLevel();
    }

    public static boolean isScrollcasterGauntletEnchantmentDenied(ResourceLocation enchantmentId) {
        return ITEMS_CONFIG.isScrollcasterGauntletEnchantmentDenied(enchantmentId);
    }

    public static boolean isScrollcasterGauntletCompatAdditionalAllowedEnchantment(ResourceLocation enchantmentId) {
        return ITEMS_CONFIG.isScrollcasterGauntletCompatAdditionalAllowedEnchantment(enchantmentId);
    }

    public static SpellStainedRunicTabletServerConfig.Values spellStainedRunicTabletConfig() {
        return ITEMS_CONFIG.spellStainedRunicTabletConfig();
    }

    public static GameTestConfigOverride useFocusStaffbowConfigOverrideForGameTest(
            boolean enableContinuousFocusedCast,
            boolean enableManaLoan,
            boolean enableArrowCatalystRequirement,
            List<String> arrowCatalystItems,
            double pendingMaxChargeMultiplier,
            double continuousMaxChargeMultiplier,
            int minimumOverchargeBaselineTicks,
            double chargeManaCostExponent,
            double chargeManaCostMultiplier,
            double pendingMaxLoanManaRatio,
            List<String> spellDenylist,
            boolean enableSpellAllowlist,
            List<String> spellAllowlist
    ) {
        var previousEnableContinuousFocusedCast = ITEMS_CONFIG.focusStaffbowEnableContinuousFocusedCast();
        var previousEnableManaLoan = ITEMS_CONFIG.focusStaffbowEnableManaLoan();
        var previousEnableArrowCatalystRequirement = ITEMS_CONFIG.focusStaffbowEnableArrowCatalystRequirement();
        var previousArrowCatalystItems = ITEMS_CONFIG.focusStaffbowArrowCatalystItems();
        var previousChargeSettings = ITEMS_CONFIG.focusStaffbowChargeSettings();
        var previousPendingMaxLoanManaRatio = ITEMS_CONFIG.focusStaffbowPendingMaxLoanManaRatio();
        var previousSpellDenylist = ITEMS_CONFIG.focusStaffbowSpellDenylist();
        var previousEnableSpellAllowlist = ITEMS_CONFIG.focusStaffbowEnableSpellAllowlist();
        var previousSpellAllowlist = ITEMS_CONFIG.focusStaffbowSpellAllowlist();

        ITEMS_CONFIG.setFocusStaffbowConfigForGameTest(
                enableContinuousFocusedCast,
                enableManaLoan,
                enableArrowCatalystRequirement,
                arrowCatalystItems,
                pendingMaxChargeMultiplier,
                continuousMaxChargeMultiplier,
                minimumOverchargeBaselineTicks,
                chargeManaCostExponent,
                chargeManaCostMultiplier,
                pendingMaxLoanManaRatio,
                spellDenylist,
                enableSpellAllowlist,
                spellAllowlist
        );
        return () -> ITEMS_CONFIG.setFocusStaffbowConfigForGameTest(
                previousEnableContinuousFocusedCast,
                previousEnableManaLoan,
                previousEnableArrowCatalystRequirement,
                previousArrowCatalystItems,
                previousChargeSettings.pendingMaxChargeMultiplier(),
                previousChargeSettings.continuousMaxChargeMultiplier(),
                previousChargeSettings.minimumOverchargeBaselineTicks(),
                previousChargeSettings.chargeManaCostExponent(),
                previousChargeSettings.chargeManaCostMultiplier(),
                previousPendingMaxLoanManaRatio,
                previousSpellDenylist,
                previousEnableSpellAllowlist,
                previousSpellAllowlist
        );
    }

    public static GameTestConfigOverride useElementalBowConfigOverrideForGameTest(
            double magicReadyDrawTicksMultiplier,
            double overheatAdditionalManaLinearMultiplier,
            double overheatAdditionalManaQuadraticMultiplier,
            double overheatDurationMultiplier,
            int overheatDurationMinTicks,
            int overheatDurationCapTicks,
            double powerArrowSpellLevelBonusPerLevel
    ) {
        var previousMagicReadyDrawTicksMultiplier = ITEMS_CONFIG.elementalBowMagicReadyDrawTicksMultiplier();
        var previousOverheatAdditionalManaLinearMultiplier = ITEMS_CONFIG.elementalBowOverheatAdditionalManaLinearMultiplier();
        var previousOverheatAdditionalManaQuadraticMultiplier = ITEMS_CONFIG.elementalBowOverheatAdditionalManaQuadraticMultiplier();
        var previousOverheatDurationMultiplier = ITEMS_CONFIG.elementalBowOverheatDurationMultiplier();
        var previousOverheatDurationMinTicks = ITEMS_CONFIG.elementalBowOverheatDurationMinTicks();
        var previousOverheatDurationCapTicks = ITEMS_CONFIG.elementalBowOverheatDurationCapTicks();
        var previousPowerArrowSpellLevelBonusPerLevel = ITEMS_CONFIG.elementalBowPowerArrowSpellLevelBonusPerLevel();

        ITEMS_CONFIG.setElementalBowConfigForGameTest(
                magicReadyDrawTicksMultiplier,
                overheatAdditionalManaLinearMultiplier,
                overheatAdditionalManaQuadraticMultiplier,
                overheatDurationMultiplier,
                overheatDurationMinTicks,
                overheatDurationCapTicks,
                powerArrowSpellLevelBonusPerLevel
        );
        return () -> ITEMS_CONFIG.setElementalBowConfigForGameTest(
                previousMagicReadyDrawTicksMultiplier,
                previousOverheatAdditionalManaLinearMultiplier,
                previousOverheatAdditionalManaQuadraticMultiplier,
                previousOverheatDurationMultiplier,
                previousOverheatDurationMinTicks,
                previousOverheatDurationCapTicks,
                previousPowerArrowSpellLevelBonusPerLevel
        );
    }

    public static GameTestConfigOverride useElementalBowMagicArrowCatalystItemsOverrideForGameTest(
            List<String> magicArrowCatalystItems
    ) {
        var previousMagicArrowCatalystItems = ITEMS_CONFIG.elementalBowMagicArrowCatalystItems();
        ITEMS_CONFIG.setElementalBowMagicArrowCatalystItemsForGameTest(magicArrowCatalystItems);
        return () -> ITEMS_CONFIG.setElementalBowMagicArrowCatalystItemsForGameTest(previousMagicArrowCatalystItems);
    }

    public static GameTestConfigOverride useMultipurposeStaffrifleSpellDenylistOverrideForGameTest(
            List<String> spellDenylist
    ) {
        var previousSpellDenylist = ITEMS_CONFIG.multipurposeStaffrifleSpellDenylist();
        ITEMS_CONFIG.setMultipurposeStaffrifleSpellDenylistForGameTest(spellDenylist);
        return () -> ITEMS_CONFIG.setMultipurposeStaffrifleSpellDenylistForGameTest(previousSpellDenylist);
    }

    public static GameTestConfigOverride useCraftsmansDelightGracedRainDenylistOverrideForGameTest(
            List<String> gracedRainGrowthDenylist,
            List<String> gracedRainBreedingCooldownDenylist
    ) {
        var previousGrowthDenylist = ITEMS_CONFIG.craftsmansDelightGracedRainGrowthDenylist();
        var previousBreedingCooldownDenylist = ITEMS_CONFIG.craftsmansDelightGracedRainBreedingCooldownDenylist();

        ITEMS_CONFIG.setCraftsmansDelightGracedRainDenylistsForGameTest(
                gracedRainGrowthDenylist,
                gracedRainBreedingCooldownDenylist
        );
        return () -> ITEMS_CONFIG.setCraftsmansDelightGracedRainDenylistsForGameTest(
                previousGrowthDenylist,
                previousBreedingCooldownDenylist
        );
    }

    public static GameTestConfigOverride useEquipmentSpellTimingMultipliersOverrideForGameTest(
            double craftsmansDelightCooldownMultiplier,
            double magiAgentSuitBootsCooldownMultiplier,
            double magiAgentSuitBootsCastTimeMultiplier
    ) {
        var previousCraftsmansDelightCooldownMultiplier = ITEMS_CONFIG.craftsmansDelightCooldownMultiplier();
        var previousMagiAgentSuitBootsCooldownMultiplier = ITEMS_CONFIG.magiAgentSuitBootsCooldownMultiplier();
        var previousMagiAgentSuitBootsCastTimeMultiplier = ITEMS_CONFIG.magiAgentSuitBootsCastTimeMultiplier();
        ITEMS_CONFIG.setEquipmentSpellTimingMultipliersForGameTest(
                craftsmansDelightCooldownMultiplier,
                magiAgentSuitBootsCooldownMultiplier,
                magiAgentSuitBootsCastTimeMultiplier
        );
        return () -> ITEMS_CONFIG.setEquipmentSpellTimingMultipliersForGameTest(
                previousCraftsmansDelightCooldownMultiplier,
                previousMagiAgentSuitBootsCooldownMultiplier,
                previousMagiAgentSuitBootsCastTimeMultiplier
        );
    }

    public static GameTestConfigOverride useSpellgunConfigOverrideForGameTest(SpellgunServerConfig.Values values) {
        var previousValues = ITEMS_CONFIG.spellgunConfig();
        ITEMS_CONFIG.setSpellgunConfigForGameTest(values);
        return () -> ITEMS_CONFIG.setSpellgunConfigForGameTest(previousValues);
    }

    public static GameTestConfigOverride useManaShieldCharmConfigOverrideForGameTest(
            double manaPerDamage,
            int recoveryThresholdMana,
            double synchronizationManaPerDamage,
            double neutralizationRecoverManaPerDamage,
            int shellArmorDurabilityDamage,
            int invulnerableTimeTicks
    ) {
        var previousManaPerDamage = ITEMS_CONFIG.manaShieldCharmManaPerDamage();
        var previousRecoveryThresholdMana = ITEMS_CONFIG.manaShieldCharmRecoveryThresholdMana();
        var previousSynchronizationManaPerDamage = ITEMS_CONFIG.manaShieldCharmSynchronizationManaPerDamage();
        var previousNeutralizationRecoverManaPerDamage = ITEMS_CONFIG.manaShieldCharmNeutralizationRecoverManaPerDamage();
        var previousShellArmorDurabilityDamage = ITEMS_CONFIG.manaShieldCharmShellArmorDurabilityDamage();
        var previousInvulnerableTimeTicks = ITEMS_CONFIG.manaShieldCharmInvulnerableTimeTicks();

        ITEMS_CONFIG.setManaShieldCharmConfigForGameTest(
                manaPerDamage,
                recoveryThresholdMana,
                synchronizationManaPerDamage,
                neutralizationRecoverManaPerDamage,
                shellArmorDurabilityDamage,
                invulnerableTimeTicks
        );
        return () -> ITEMS_CONFIG.setManaShieldCharmConfigForGameTest(
                previousManaPerDamage,
                previousRecoveryThresholdMana,
                previousSynchronizationManaPerDamage,
                previousNeutralizationRecoverManaPerDamage,
                previousShellArmorDurabilityDamage,
                previousInvulnerableTimeTicks
        );
    }

    public static GameTestConfigOverride useManaThrusterConfigOverrideForGameTest(double manaCostPerTick) {
        var previousManaCostPerTick = ITEMS_CONFIG.manaThrusterManaCostPerTick();

        ITEMS_CONFIG.setManaThrusterConfigForGameTest(manaCostPerTick);
        return () -> ITEMS_CONFIG.setManaThrusterConfigForGameTest(previousManaCostPerTick);
    }

    public static GameTestConfigOverride useMagiCompressorGadgetConfigOverrideForGameTest(
            double manaCostPerSecond,
            double airFillPerSecond,
            double maxAir
    ) {
        var previousManaCostPerSecond = ITEMS_CONFIG.magiCompressorGadgetManaCostPerSecond();
        var previousAirFillPerSecond = ITEMS_CONFIG.magiCompressorGadgetAirFillPerSecond();
        var previousMaxAir = ITEMS_CONFIG.magiCompressorGadgetMaxAir();

        ITEMS_CONFIG.setMagiCompressorGadgetConfigForGameTest(manaCostPerSecond, airFillPerSecond, maxAir);
        return () -> ITEMS_CONFIG.setMagiCompressorGadgetConfigForGameTest(
                previousManaCostPerSecond,
                previousAirFillPerSecond,
                previousMaxAir
        );
    }

    public static GameTestConfigOverride useManaForceBladeCooldownConfigOverrideForGameTest(
            int releaseCooldownTicks,
            int perfectGuardReleaseCooldownGraceTicks,
            int perfectGuardReleaseCooldownGraceUses
    ) {
        var previousReleaseCooldownTicks = ITEMS_CONFIG.manaForceBladeReleaseCooldownTicks();
        var previousPerfectGuardReleaseCooldownGraceTicks =
                ITEMS_CONFIG.manaForceBladePerfectGuardReleaseCooldownGraceTicks();
        var previousPerfectGuardReleaseCooldownGraceUses =
                ITEMS_CONFIG.manaForceBladePerfectGuardReleaseCooldownGraceUses();

        ITEMS_CONFIG.setManaForceBladeCooldownConfigForGameTest(
                releaseCooldownTicks,
                perfectGuardReleaseCooldownGraceTicks,
                perfectGuardReleaseCooldownGraceUses
        );
        return () -> ITEMS_CONFIG.setManaForceBladeCooldownConfigForGameTest(
                previousReleaseCooldownTicks,
                previousPerfectGuardReleaseCooldownGraceTicks,
                previousPerfectGuardReleaseCooldownGraceUses
        );
    }

    public static GameTestConfigOverride useCircuitHeatStaffConfigOverrideForGameTest(
            int additionalManaReferenceCooldownTicks,
            double additionalManaLinearMultiplier,
            double additionalManaQuadraticMultiplier,
            int cooldownBypassMaxRemainingTicks,
            List<String> spellDenylist,
            double staffOverheatDurationMultiplier,
            int staffOverheatDurationMinTicks,
            int staffOverheatDurationCapTicks,
            boolean dropCoolingEnabled,
            int dropCoolingProcessIntervalTicks,
            int dropCoolingReductionTicks,
            int dropCoolingWaterConsumeProcessCount,
            boolean consumeWaterSourceOnCooling,
            boolean consumeWaterCauldronOnCooling
    ) {
        var previousAdditionalManaReferenceCooldownTicks = ITEMS_CONFIG.circuitHeatStaffAdditionalManaReferenceCooldownTicks();
        var previousAdditionalManaLinearMultiplier = ITEMS_CONFIG.circuitHeatStaffAdditionalManaLinearMultiplier();
        var previousAdditionalManaQuadraticMultiplier = ITEMS_CONFIG.circuitHeatStaffAdditionalManaQuadraticMultiplier();
        var previousCooldownBypassMaxRemainingTicks = ITEMS_CONFIG.circuitHeatStaffCooldownBypassMaxRemainingTicks();
        var previousSpellDenylist = ITEMS_CONFIG.circuitHeatStaffSpellDenylist();
        var previousStaffOverheatDurationMultiplier = ITEMS_CONFIG.circuitHeatStaffOverheatDurationMultiplier();
        var previousStaffOverheatDurationMinTicks = ITEMS_CONFIG.circuitHeatStaffOverheatDurationMinTicks();
        var previousStaffOverheatDurationCapTicks = ITEMS_CONFIG.circuitHeatStaffOverheatDurationCapTicks();
        var previousDropCoolingEnabled = ITEMS_CONFIG.circuitHeatStaffDropCoolingEnabled();
        var previousDropCoolingProcessIntervalTicks = ITEMS_CONFIG.circuitHeatStaffDropCoolingProcessIntervalTicks();
        var previousDropCoolingReductionTicks = ITEMS_CONFIG.circuitHeatStaffDropCoolingReductionTicks();
        var previousDropCoolingWaterConsumeProcessCount = ITEMS_CONFIG.circuitHeatStaffDropCoolingWaterConsumeProcessCount();
        var previousConsumeWaterSourceOnCooling = ITEMS_CONFIG.circuitHeatStaffConsumeWaterSourceOnCooling();
        var previousConsumeWaterCauldronOnCooling = ITEMS_CONFIG.circuitHeatStaffConsumeWaterCauldronOnCooling();

        ITEMS_CONFIG.setCircuitHeatStaffConfigForGameTest(
                additionalManaReferenceCooldownTicks,
                additionalManaLinearMultiplier,
                additionalManaQuadraticMultiplier,
                cooldownBypassMaxRemainingTicks,
                spellDenylist,
                staffOverheatDurationMultiplier,
                staffOverheatDurationMinTicks,
                staffOverheatDurationCapTicks,
                dropCoolingEnabled,
                dropCoolingProcessIntervalTicks,
                dropCoolingReductionTicks,
                dropCoolingWaterConsumeProcessCount,
                consumeWaterSourceOnCooling,
                consumeWaterCauldronOnCooling
        );
        return () -> ITEMS_CONFIG.setCircuitHeatStaffConfigForGameTest(
                previousAdditionalManaReferenceCooldownTicks,
                previousAdditionalManaLinearMultiplier,
                previousAdditionalManaQuadraticMultiplier,
                previousCooldownBypassMaxRemainingTicks,
                previousSpellDenylist,
                previousStaffOverheatDurationMultiplier,
                previousStaffOverheatDurationMinTicks,
                previousStaffOverheatDurationCapTicks,
                previousDropCoolingEnabled,
                previousDropCoolingProcessIntervalTicks,
                previousDropCoolingReductionTicks,
                previousDropCoolingWaterConsumeProcessCount,
                previousConsumeWaterSourceOnCooling,
                previousConsumeWaterCauldronOnCooling
        );
    }

    public static GameTestConfigOverride useMulticastEchoStaffConfigOverrideForGameTest(
            int multicastDelayTicks,
            double cooldownMultiplier,
            double castTimeCooldownMultiplier,
            int cooldownCapTicks,
            int maxMulticastCount
    ) {
        var previousMulticastDelayTicks = ITEMS_CONFIG.multicastEchoStaffDelayTicks();
        var previousCooldownMultiplier = ITEMS_CONFIG.multicastEchoStaffCooldownMultiplier();
        var previousCastTimeCooldownMultiplier = ITEMS_CONFIG.multicastEchoStaffCastTimeCooldownMultiplier();
        var previousCooldownCapTicks = ITEMS_CONFIG.multicastEchoStaffCooldownCapTicks();
        var previousMaxMulticastCount = ITEMS_CONFIG.multicastEchoStaffMaxMulticastCount();

        ITEMS_CONFIG.setMulticastEchoStaffConfigForGameTest(
                multicastDelayTicks,
                cooldownMultiplier,
                castTimeCooldownMultiplier,
                cooldownCapTicks,
                maxMulticastCount
        );
        return () -> ITEMS_CONFIG.setMulticastEchoStaffConfigForGameTest(
                previousMulticastDelayTicks,
                previousCooldownMultiplier,
                previousCastTimeCooldownMultiplier,
                previousCooldownCapTicks,
                previousMaxMulticastCount
        );
    }

    public static GameTestConfigOverride useMulticastEchoStaffMobEffectConfigOverrideForGameTest(
            boolean mobEffectProfilesEnabled,
            boolean beneficialMobEffectsEnabled,
            boolean harmfulMobEffectsEnabled,
            boolean neutralMobEffectsEnabled,
            boolean durationServerCapEnabled,
            int durationServerCapTicks,
            boolean amplifierServerCapEnabled,
            int amplifierServerCap
    ) {
        var previousMobEffectProfilesEnabled = ITEMS_CONFIG.multicastEchoStaffMobEffectProfilesEnabled();
        var previousBeneficialMobEffectsEnabled = ITEMS_CONFIG.multicastEchoStaffBeneficialMobEffectsEnabled();
        var previousHarmfulMobEffectsEnabled = ITEMS_CONFIG.multicastEchoStaffHarmfulMobEffectsEnabled();
        var previousNeutralMobEffectsEnabled = ITEMS_CONFIG.multicastEchoStaffNeutralMobEffectsEnabled();
        var previousDurationServerCapEnabled = ITEMS_CONFIG.multicastEchoStaffDurationServerCapEnabled();
        var previousDurationServerCapTicks = ITEMS_CONFIG.multicastEchoStaffDurationServerCapTicks();
        var previousAmplifierServerCapEnabled = ITEMS_CONFIG.multicastEchoStaffAmplifierServerCapEnabled();
        var previousAmplifierServerCap = ITEMS_CONFIG.multicastEchoStaffAmplifierServerCap();

        ITEMS_CONFIG.setMulticastEchoStaffMobEffectConfigForGameTest(
                mobEffectProfilesEnabled,
                beneficialMobEffectsEnabled,
                harmfulMobEffectsEnabled,
                neutralMobEffectsEnabled,
                durationServerCapEnabled,
                durationServerCapTicks,
                amplifierServerCapEnabled,
                amplifierServerCap
        );
        return () -> ITEMS_CONFIG.setMulticastEchoStaffMobEffectConfigForGameTest(
                previousMobEffectProfilesEnabled,
                previousBeneficialMobEffectsEnabled,
                previousHarmfulMobEffectsEnabled,
                previousNeutralMobEffectsEnabled,
                previousDurationServerCapEnabled,
                previousDurationServerCapTicks,
                previousAmplifierServerCapEnabled,
                previousAmplifierServerCap
        );
    }

    public static GameTestConfigOverride useMulticastEchoStaffAttackConfigOverrideForGameTest(
            boolean attackProfilesEnabled,
            double repeatDamageMultiplier
    ) {
        var previousAttackProfilesEnabled = ITEMS_CONFIG.multicastEchoStaffAttackProfilesEnabled();
        var previousRepeatDamageMultiplier = ITEMS_CONFIG.multicastEchoStaffRepeatDamageMultiplier();

        ITEMS_CONFIG.setMulticastEchoStaffAttackConfigForGameTest(
                attackProfilesEnabled,
                repeatDamageMultiplier
        );
        return () -> ITEMS_CONFIG.setMulticastEchoStaffAttackConfigForGameTest(
                previousAttackProfilesEnabled,
                previousRepeatDamageMultiplier
        );
    }

    public static GameTestConfigOverride useScrollcasterGauntletConfigOverrideForGameTest(
            List<String> deniedEnchantments,
            List<String> compatAdditionalAllowedEnchantments
    ) {
        var previousDeniedEnchantments = ITEMS_CONFIG.scrollcasterGauntletDeniedEnchantments();
        var previousCompatAdditionalAllowedEnchantments = ITEMS_CONFIG.scrollcasterGauntletCompatAdditionalAllowedEnchantments();

        ITEMS_CONFIG.setScrollcasterGauntletConfigForGameTest(
                deniedEnchantments,
                compatAdditionalAllowedEnchantments
        );
        return () -> ITEMS_CONFIG.setScrollcasterGauntletConfigForGameTest(
                previousDeniedEnchantments,
                previousCompatAdditionalAllowedEnchantments
        );
    }

    public static GameTestConfigOverride useChargecastCatalystbookConfigOverrideForGameTest(
            ChargecastCatalystbookServerConfig.Values values
    ) {
        var previous = ITEMS_CONFIG.chargecastCatalystbookConfig();
        ITEMS_CONFIG.setChargecastCatalystbookConfigForGameTest(values);
        return () -> ITEMS_CONFIG.setChargecastCatalystbookConfigForGameTest(previous);
    }

    public static int spellCastParryingRingParryWindowTicks() {
        return ITEMS_CONFIG.spellCastParryingRingParryWindowTicks();
    }

    public static SpellchargedGreatswordServerConfig.Values spellchargedGreatswordConfig() {
        return ITEMS_CONFIG.spellchargedGreatswordConfig();
    }

    public static GameTestConfigOverride useSpellCastParryingRingParryWindowTicksOverrideForGameTest(int value) {
        var previousValue = ITEMS_CONFIG.spellCastParryingRingParryWindowTicks();
        ITEMS_CONFIG.setSpellCastParryingRingParryWindowTicksForGameTest(value);
        return () -> ITEMS_CONFIG.setSpellCastParryingRingParryWindowTicksForGameTest(previousValue);
    }

    public static GameTestConfigOverride useSpellchargedGreatswordConfigOverrideForGameTest(
            SpellchargedGreatswordServerConfig.Values values
    ) {
        var previousValues = ITEMS_CONFIG.spellchargedGreatswordConfig();
        ITEMS_CONFIG.setSpellchargedGreatswordConfigForGameTest(values);
        return () -> ITEMS_CONFIG.setSpellchargedGreatswordConfigForGameTest(previousValues);
    }

    public static GameTestConfigOverride useSpellStainedRunicTabletConfigOverrideForGameTest(
            SpellStainedRunicTabletServerConfig.Values values
    ) {
        var previousValues = ITEMS_CONFIG.spellStainedRunicTabletConfig();
        ITEMS_CONFIG.setSpellStainedRunicTabletConfigForGameTest(values);
        return () -> ITEMS_CONFIG.setSpellStainedRunicTabletConfigForGameTest(previousValues);
    }

    public static int spellInvokeCardCraftCount() {
        return ITEMS_CONFIG.spellInvokeCardCraftCount();
    }

    public static int spellAutonomyCardCraftCount() {
        return ITEMS_CONFIG.spellAutonomyCardCraftCount();
    }

    public static GameTestConfigOverride useSpellThrowableCardConfigOverrideForGameTest(
            SpellThrowableCardServerConfig.Values values
    ) {
        var previousValues = ITEMS_CONFIG.spellThrowableCardConfig();
        ITEMS_CONFIG.setSpellThrowableCardConfigForGameTest(values);
        return () -> ITEMS_CONFIG.setSpellThrowableCardConfigForGameTest(previousValues);
    }

    public static int archivistsGrimoireInitialRows() {
        return ITEMS_CONFIG.archivistsGrimoireInitialRows();
    }

    public static int archivistsGrimoireEffectiveMaxRows() {
        return ITEMS_CONFIG.archivistsGrimoireEffectiveMaxRows();
    }

    public static GameTestConfigOverride useArchivistsGrimoireConfigOverrideForGameTest(
            ArchivistsGrimoireServerConfig.Values values
    ) {
        var previousValues = new ArchivistsGrimoireServerConfig.Values(
                ITEMS_CONFIG.archivistsGrimoireInitialRows(),
                ITEMS_CONFIG.archivistsGrimoireEffectiveMaxRows()
        );
        ITEMS_CONFIG.setArchivistsGrimoireConfigForGameTest(values);
        return () -> ITEMS_CONFIG.setArchivistsGrimoireConfigForGameTest(previousValues);
    }

    public static GameTestConfigOverride useElementMaidenRobeSchoolSpellPowerBonusOverrideForGameTest(double value) {
        var previousValue = ITEMS_CONFIG.elementMaidenRobeSchoolSpellPowerBonus();
        ITEMS_CONFIG.setElementMaidenRobeSchoolSpellPowerBonusForGameTest(value);
        return () -> ITEMS_CONFIG.setElementMaidenRobeSchoolSpellPowerBonusForGameTest(previousValue);
    }

    public static GameTestConfigOverride useMagiAgentSuitAmmoConfigOverrideForGameTest(
            double ammoNoConsumeChance,
            boolean skipStaffrifleManaCostWhenAmmoNotConsumed
    ) {
        var previousAmmoNoConsumeChance = ITEMS_CONFIG.magiAgentSuitAmmoNoConsumeChance();
        var previousSkipStaffrifleManaCostWhenAmmoNotConsumed =
                ITEMS_CONFIG.magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed();

        ITEMS_CONFIG.setMagiAgentSuitAmmoConfigForGameTest(
                ammoNoConsumeChance,
                skipStaffrifleManaCostWhenAmmoNotConsumed
        );
        return () -> ITEMS_CONFIG.setMagiAgentSuitAmmoConfigForGameTest(
                previousAmmoNoConsumeChance,
                previousSkipStaffrifleManaCostWhenAmmoNotConsumed
        );
    }

    public static GameTestConfigOverride useZenithStaffManaCostMultiplierOverrideForGameTest(double value) {
        var previousValue = ITEMS_CONFIG.zenithStaffManaCostMultiplier();
        ITEMS_CONFIG.setZenithStaffManaCostMultiplierForGameTest(value);
        return () -> ITEMS_CONFIG.setZenithStaffManaCostMultiplierForGameTest(previousValue);
    }

    public static float forceFieldDrainManaBasePerHit() {
        return SPELLS_CONFIG.forceFieldDrainManaBasePerHit();
    }

    public static LinearBuildServerConfig.Values linearBuildConfig() {
        return SPELLS_CONFIG.linearBuildConfig();
    }

    public static GameTestConfigOverride useLinearBuildConfigOverrideForGameTest(
            LinearBuildServerConfig.Values values
    ) {
        var previousValues = SPELLS_CONFIG.linearBuildConfig();
        SPELLS_CONFIG.setLinearBuildConfigForGameTest(values);
        return () -> SPELLS_CONFIG.setLinearBuildConfigForGameTest(previousValues);
    }

    public static boolean isMistFormPassableBlockDenied(BlockState state) {
        return SPELLS_CONFIG.isMistFormPassableBlockDenied(state);
    }

    public static List<String> mistFormPassableBlockDenylist() {
        return SPELLS_CONFIG.mistFormPassableBlockDenylist();
    }

    public static GameTestConfigOverride useMistFormPassableBlockDenylistOverrideForGameTest(
            List<String> passableBlockDenylist
    ) {
        var previousPassableBlockDenylist = SPELLS_CONFIG.mistFormPassableBlockDenylist();
        SPELLS_CONFIG.setMistFormPassableBlockDenylistForGameTest(passableBlockDenylist);
        return () -> SPELLS_CONFIG.setMistFormPassableBlockDenylistForGameTest(previousPassableBlockDenylist);
    }

    public static boolean autoMagnetDisableCollectManaCost() {
        return SPELLS_CONFIG.autoMagnetDisableCollectManaCost();
    }

    public static int boundBowMaxPowerEnchantmentLevel() {
        return SPELLS_CONFIG.boundBowMaxPowerEnchantmentLevel();
    }

    public static float boundBowForgeArrowManaCost() {
        return SPELLS_CONFIG.boundBowForgeArrowManaCost();
    }

    public static GameTestConfigOverride useBoundBowConfigOverrideForGameTest(
            int maxPowerEnchantmentLevel,
            float forgeArrowManaCost
    ) {
        var previousMaxPowerEnchantmentLevel = SPELLS_CONFIG.boundBowMaxPowerEnchantmentLevel();
        var previousForgeArrowManaCost = SPELLS_CONFIG.boundBowForgeArrowManaCost();

        SPELLS_CONFIG.setBoundBowConfigForGameTest(maxPowerEnchantmentLevel, forgeArrowManaCost);
        return () -> SPELLS_CONFIG.setBoundBowConfigForGameTest(
                previousMaxPowerEnchantmentLevel,
                previousForgeArrowManaCost
        );
    }

    public static boolean isDemicreatorWingsDimensionAllowed(ResourceLocation dimensionId) {
        return SPELLS_CONFIG.isDemicreatorWingsDimensionAllowed(dimensionId);
    }

    public static GameTestConfigOverride useDemicreatorWingsConfigOverrideForGameTest(
            List<String> dimensionDenylist,
            boolean enableDimensionAllowlist,
            List<String> dimensionAllowlist
    ) {
        var previousDimensionDenylist = SPELLS_CONFIG.demicreatorWingsDimensionDenylist();
        var previousEnableDimensionAllowlist = SPELLS_CONFIG.demicreatorWingsEnableDimensionAllowlist();
        var previousDimensionAllowlist = SPELLS_CONFIG.demicreatorWingsDimensionAllowlist();

        SPELLS_CONFIG.setDemicreatorWingsConfigForGameTest(
                dimensionDenylist,
                enableDimensionAllowlist,
                dimensionAllowlist
        );
        return () -> SPELLS_CONFIG.setDemicreatorWingsConfigForGameTest(
                previousDimensionDenylist,
                previousEnableDimensionAllowlist,
                previousDimensionAllowlist
        );
    }

    public static boolean isRemoteEyeDimensionAllowed(ResourceLocation dimensionId) {
        return SPELLS_CONFIG.isRemoteEyeDimensionAllowed(dimensionId);
    }

    public static GameTestConfigOverride useRemoteEyeConfigOverrideForGameTest(
            List<String> dimensionDenylist,
            boolean enableDimensionAllowlist,
            List<String> dimensionAllowlist
    ) {
        var previousDimensionDenylist = SPELLS_CONFIG.remoteEyeDimensionDenylist();
        var previousEnableDimensionAllowlist = SPELLS_CONFIG.remoteEyeEnableDimensionAllowlist();
        var previousDimensionAllowlist = SPELLS_CONFIG.remoteEyeDimensionAllowlist();

        SPELLS_CONFIG.setRemoteEyeConfigForGameTest(
                dimensionDenylist,
                enableDimensionAllowlist,
                dimensionAllowlist
        );
        return () -> SPELLS_CONFIG.setRemoteEyeConfigForGameTest(
                previousDimensionDenylist,
                previousEnableDimensionAllowlist,
                previousDimensionAllowlist
        );
    }

    public static boolean isRiftHoleDimensionAllowed(ResourceLocation dimensionId) {
        return SPELLS_CONFIG.isRiftHoleDimensionAllowed(dimensionId);
    }

    public static GameTestConfigOverride useRiftHoleConfigOverrideForGameTest(
            List<String> dimensionDenylist,
            boolean enableDimensionAllowlist,
            List<String> dimensionAllowlist
    ) {
        var previousDimensionDenylist = SPELLS_CONFIG.riftHoleDimensionDenylist();
        var previousEnableDimensionAllowlist = SPELLS_CONFIG.riftHoleEnableDimensionAllowlist();
        var previousDimensionAllowlist = SPELLS_CONFIG.riftHoleDimensionAllowlist();

        SPELLS_CONFIG.setRiftHoleConfigForGameTest(
                dimensionDenylist,
                enableDimensionAllowlist,
                dimensionAllowlist
        );
        return () -> SPELLS_CONFIG.setRiftHoleConfigForGameTest(
                previousDimensionDenylist,
                previousEnableDimensionAllowlist,
                previousDimensionAllowlist
        );
    }

    public static boolean enableErrandMageVillageHouseInjection() {
        return WORLDGEN_CONFIG.enableErrandMageVillageHouseInjection();
    }
}
