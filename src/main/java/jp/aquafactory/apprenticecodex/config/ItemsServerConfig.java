package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.config.item.CraftsmansDelightServerConfig;
import jp.aquafactory.apprenticecodex.config.item.CircuitHeatStaffServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ChromaticMagiaDressServerConfig;
import jp.aquafactory.apprenticecodex.config.item.FocusStaffbowServerConfig;
import jp.aquafactory.apprenticecodex.config.item.IsekaiTravelGuidebookServerConfig;
import jp.aquafactory.apprenticecodex.config.item.MagicArmorServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ManaForceBladeServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ManaShieldCharmServerConfig;
import jp.aquafactory.apprenticecodex.config.item.MulticastEchoStaffServerConfig;
import jp.aquafactory.apprenticecodex.config.item.MultipurposeStaffrifleServerConfig;
import jp.aquafactory.apprenticecodex.config.item.PastelStaffServerConfig;
import jp.aquafactory.apprenticecodex.config.item.AbsorptionAmplifyAmuletServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ArcaneCinderServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ScarletThirstServerConfig;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeSettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

final class ItemsServerConfig {
    private final ArcaneCinderServerConfig arcaneCinderConfig;
    private final AbsorptionAmplifyAmuletServerConfig absorptionAmplifyAmuletConfig;
    private final ScarletThirstServerConfig scarletThirstConfig;
    private final CraftsmansDelightServerConfig craftsmansDelightConfig;
    private final MagicArmorServerConfig magicArmorConfig;
    private final ChromaticMagiaDressServerConfig chromaticMagiaDressConfig;
    private final PastelStaffServerConfig pastelStaffConfig;
    private final IsekaiTravelGuidebookServerConfig isekaiTravelGuidebookConfig;
    private final ManaForceBladeServerConfig manaForceBladeConfig;
    private final ManaShieldCharmServerConfig manaShieldCharmConfig;
    private final CircuitHeatStaffServerConfig circuitHeatStaffConfig;
    private final MulticastEchoStaffServerConfig multicastEchoStaffConfig;
    private final MultipurposeStaffrifleServerConfig multipurposeStaffrifleConfig;
    private final FocusStaffbowServerConfig focusStaffbowConfig;

    private ItemsServerConfig(
            ArcaneCinderServerConfig arcaneCinderConfig,
            AbsorptionAmplifyAmuletServerConfig absorptionAmplifyAmuletConfig,
            ScarletThirstServerConfig scarletThirstConfig,
            CraftsmansDelightServerConfig craftsmansDelightConfig,
            MagicArmorServerConfig magicArmorConfig,
            ChromaticMagiaDressServerConfig chromaticMagiaDressConfig,
            PastelStaffServerConfig pastelStaffConfig,
            IsekaiTravelGuidebookServerConfig isekaiTravelGuidebookConfig,
            ManaForceBladeServerConfig manaForceBladeConfig,
            ManaShieldCharmServerConfig manaShieldCharmConfig,
            CircuitHeatStaffServerConfig circuitHeatStaffConfig,
            MulticastEchoStaffServerConfig multicastEchoStaffConfig,
            MultipurposeStaffrifleServerConfig multipurposeStaffrifleConfig,
            FocusStaffbowServerConfig focusStaffbowConfig
    ) {
        this.arcaneCinderConfig = arcaneCinderConfig;
        this.absorptionAmplifyAmuletConfig = absorptionAmplifyAmuletConfig;
        this.scarletThirstConfig = scarletThirstConfig;
        this.craftsmansDelightConfig = craftsmansDelightConfig;
        this.magicArmorConfig = magicArmorConfig;
        this.chromaticMagiaDressConfig = chromaticMagiaDressConfig;
        this.pastelStaffConfig = pastelStaffConfig;
        this.isekaiTravelGuidebookConfig = isekaiTravelGuidebookConfig;
        this.manaForceBladeConfig = manaForceBladeConfig;
        this.manaShieldCharmConfig = manaShieldCharmConfig;
        this.circuitHeatStaffConfig = circuitHeatStaffConfig;
        this.multicastEchoStaffConfig = multicastEchoStaffConfig;
        this.multipurposeStaffrifleConfig = multipurposeStaffrifleConfig;
        this.focusStaffbowConfig = focusStaffbowConfig;
    }

    static ItemsServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("Items");
        var arcaneCinderConfig = ArcaneCinderServerConfig.define(builder);
        var absorptionAmplifyAmuletConfig = AbsorptionAmplifyAmuletServerConfig.define(builder);
        var scarletThirstConfig = ScarletThirstServerConfig.define(builder);
        var craftsmansDelightConfig = CraftsmansDelightServerConfig.define(builder);
        var magicArmorConfig = MagicArmorServerConfig.define(builder);
        var chromaticMagiaDressConfig = ChromaticMagiaDressServerConfig.define(builder);
        var pastelStaffConfig = PastelStaffServerConfig.define(builder);
        var isekaiTravelGuidebookConfig = IsekaiTravelGuidebookServerConfig.define(builder);
        var manaForceBladeConfig = ManaForceBladeServerConfig.define(builder);
        var manaShieldCharmConfig = ManaShieldCharmServerConfig.define(builder);
        var circuitHeatStaffConfig = CircuitHeatStaffServerConfig.define(builder);
        var multicastEchoStaffConfig = MulticastEchoStaffServerConfig.define(builder);
        var multipurposeStaffrifleConfig = MultipurposeStaffrifleServerConfig.define(builder);
        var focusStaffbowConfig = FocusStaffbowServerConfig.define(builder);
        builder.pop();

        return new ItemsServerConfig(
                arcaneCinderConfig,
                absorptionAmplifyAmuletConfig,
                scarletThirstConfig,
                craftsmansDelightConfig,
                magicArmorConfig,
                chromaticMagiaDressConfig,
                pastelStaffConfig,
                isekaiTravelGuidebookConfig,
                manaForceBladeConfig,
                manaShieldCharmConfig,
                circuitHeatStaffConfig,
                multicastEchoStaffConfig,
                multipurposeStaffrifleConfig,
                focusStaffbowConfig
        );
    }

    boolean limitArcaneCinderSpeedupToVanillaFurnaces() {
        return arcaneCinderConfig.limitArcaneCinderSpeedupToVanillaFurnaces();
    }

    int absorptionAmplifyAmuletRecoveryDelayTicks() {
        return absorptionAmplifyAmuletConfig.recoveryDelayTicks();
    }

    float absorptionAmplifyAmuletBaseAbsorptionTarget() {
        return absorptionAmplifyAmuletConfig.baseAbsorptionTarget();
    }

    float scarletThirstRequiredHealth() {
        return scarletThirstConfig.requiredHealth();
    }

    float scarletThirstDrainHealth() {
        return scarletThirstConfig.drainHealth();
    }

    float scarletThirstDrainEmergencyHealth() {
        return scarletThirstConfig.drainEmergencyHealth();
    }

    float scarletThirstRecoverMana() {
        return scarletThirstConfig.recoverMana();
    }

    float scarletThirstRecoverEmergencyMana() {
        return scarletThirstConfig.recoverEmergencyMana();
    }

    boolean craftsmansDelightCanImbueEnchantment() {
        return craftsmansDelightConfig.canImbueEnchantment();
    }

    float craftsmansDelightRequiredMana() {
        return craftsmansDelightConfig.requiredMana();
    }

    int craftsmansDelightFortuneLevel() {
        return craftsmansDelightConfig.fortuneLevel();
    }

    double apprenticeMageRobeSpellPowerBonusPerPiece() {
        return magicArmorConfig.apprenticeMageRobeSpellPowerBonusPerPiece();
    }

    double enchantressRobeSpellPowerBonusPerPiece() {
        return magicArmorConfig.enchantressRobeSpellPowerBonusPerPiece();
    }

    double chromaticMagiaDressSpellPowerBonusPerPiece() {
        return magicArmorConfig.chromaticMagiaDressSpellPowerBonusPerPiece();
    }

    double stealthRuneArmorSpellPowerBonusPerPiece() {
        return magicArmorConfig.stealthRuneArmorSpellPowerBonusPerPiece();
    }

    double chromaticMagiaDressSchoolSpellPowerBonusPerHistory() {
        return chromaticMagiaDressConfig.schoolSpellPowerBonusPerHistory();
    }

    double pastelStaffAmplifyTintedMagicMultiplier() {
        return pastelStaffConfig.amplifyTintedMagicMultiplier();
    }

    boolean showIsekaiTravelGuidebookTooltip() {
        return isekaiTravelGuidebookConfig.showTooltip();
    }

    float manaForceBladeImbueDamageMultiplierScale() {
        return manaForceBladeConfig.imbueDamageMultiplierScale();
    }

    float manaForceBladeAttackManaCostMultiplier() {
        return manaForceBladeConfig.attackManaCostMultiplier();
    }

    float manaForceBladeAttackManaSchoolMultiplierScale() {
        return manaForceBladeConfig.attackManaSchoolMultiplierScale();
    }

    float manaForceBladeMeleeGuardManaCost() {
        return manaForceBladeConfig.meleeGuardManaCost();
    }

    float manaForceBladeRangedGuardManaCost() {
        return manaForceBladeConfig.rangedGuardManaCost();
    }

    boolean manaForceBladeDisableManaRecoveryWhileGuarding() {
        return manaForceBladeConfig.disableManaRecoveryWhileGuarding();
    }

    int manaForceBladePerfectGuardTicks() {
        return manaForceBladeConfig.perfectGuardTicks();
    }

    float manaShieldCharmManaPerDamage() {
        return manaShieldCharmConfig.manaPerDamage();
    }

    int manaShieldCharmRecoveryThresholdMana() {
        return manaShieldCharmConfig.recoveryThresholdMana();
    }

    float manaShieldCharmSynchronizationManaPerDamage() {
        return manaShieldCharmConfig.synchronizationManaPerDamage();
    }

    float manaShieldCharmNeutralizationRecoverManaPerDamage() {
        return manaShieldCharmConfig.neutralizationRecoverManaPerDamage();
    }

    int manaShieldCharmShellArmorDurabilityDamage() {
        return manaShieldCharmConfig.shellArmorDurabilityDamage();
    }

    int manaShieldCharmInvulnerableTimeTicks() {
        return manaShieldCharmConfig.invulnerableTimeTicks();
    }

    int multipurposeStaffrifleCooldownBypassThresholdTicks() {
        return multipurposeStaffrifleConfig.cooldownBypassThresholdTicks();
    }

    int circuitHeatStaffAdditionalManaReferenceCooldownTicks() {
        return circuitHeatStaffConfig.additionalManaReferenceCooldownTicks();
    }

    float circuitHeatStaffAdditionalManaLinearMultiplier() {
        return circuitHeatStaffConfig.additionalManaLinearMultiplier();
    }

    float circuitHeatStaffAdditionalManaQuadraticMultiplier() {
        return circuitHeatStaffConfig.additionalManaQuadraticMultiplier();
    }

    int circuitHeatStaffCooldownBypassMaxRemainingTicks() {
        return circuitHeatStaffConfig.cooldownBypassMaxRemainingTicks();
    }

    boolean isCircuitHeatStaffSpellDenied(ResourceLocation spellId) {
        return circuitHeatStaffConfig.isSpellDenied(spellId);
    }

    List<String> circuitHeatStaffSpellDenylist() {
        return circuitHeatStaffConfig.spellDenylist();
    }

    double circuitHeatStaffOverheatDurationMultiplier() {
        return circuitHeatStaffConfig.staffOverheatDurationMultiplier();
    }

    int circuitHeatStaffOverheatDurationMinTicks() {
        return circuitHeatStaffConfig.staffOverheatDurationMinTicks();
    }

    int circuitHeatStaffOverheatDurationCapTicks() {
        return circuitHeatStaffConfig.staffOverheatDurationCapTicks();
    }

    boolean circuitHeatStaffDropCoolingEnabled() {
        return circuitHeatStaffConfig.dropCoolingEnabled();
    }

    int circuitHeatStaffDropCoolingProcessIntervalTicks() {
        return circuitHeatStaffConfig.dropCoolingProcessIntervalTicks();
    }

    int circuitHeatStaffDropCoolingReductionTicks() {
        return circuitHeatStaffConfig.dropCoolingReductionTicks();
    }

    int circuitHeatStaffDropCoolingWaterConsumeProcessCount() {
        return circuitHeatStaffConfig.dropCoolingWaterConsumeProcessCount();
    }

    boolean circuitHeatStaffConsumeWaterSourceOnCooling() {
        return circuitHeatStaffConfig.consumeWaterSourceOnCooling();
    }

    boolean circuitHeatStaffConsumeWaterCauldronOnCooling() {
        return circuitHeatStaffConfig.consumeWaterCauldronOnCooling();
    }

    int multicastEchoStaffDelayTicks() {
        return multicastEchoStaffConfig.multicastDelayTicks();
    }

    double multicastEchoStaffCooldownMultiplier() {
        return multicastEchoStaffConfig.cooldownMultiplier();
    }

    double multicastEchoStaffCastTimeCooldownMultiplier() {
        return multicastEchoStaffConfig.castTimeCooldownMultiplier();
    }

    int multicastEchoStaffCooldownCapTicks() {
        return multicastEchoStaffConfig.cooldownCapTicks();
    }

    int multicastEchoStaffMaxMulticastCount() {
        return multicastEchoStaffConfig.maxMulticastCount();
    }

    boolean multicastEchoStaffMobEffectProfilesEnabled() {
        return multicastEchoStaffConfig.mobEffectProfilesEnabled();
    }

    boolean multicastEchoStaffBeneficialMobEffectsEnabled() {
        return multicastEchoStaffConfig.beneficialMobEffectsEnabled();
    }

    boolean multicastEchoStaffHarmfulMobEffectsEnabled() {
        return multicastEchoStaffConfig.harmfulMobEffectsEnabled();
    }

    boolean multicastEchoStaffNeutralMobEffectsEnabled() {
        return multicastEchoStaffConfig.neutralMobEffectsEnabled();
    }

    boolean multicastEchoStaffDurationServerCapEnabled() {
        return multicastEchoStaffConfig.durationServerCapEnabled();
    }

    int multicastEchoStaffDurationServerCapTicks() {
        return multicastEchoStaffConfig.durationServerCapTicks();
    }

    boolean multicastEchoStaffAmplifierServerCapEnabled() {
        return multicastEchoStaffConfig.amplifierServerCapEnabled();
    }

    int multicastEchoStaffAmplifierServerCap() {
        return multicastEchoStaffConfig.amplifierServerCap();
    }

    boolean multicastEchoStaffAttackProfilesEnabled() {
        return multicastEchoStaffConfig.attackProfilesEnabled();
    }

    double multicastEchoStaffRepeatDamageMultiplier() {
        return multicastEchoStaffConfig.repeatDamageMultiplier();
    }

    int multipurposeStaffrifleCooldownReductionTicks() {
        return multipurposeStaffrifleConfig.cooldownReductionTicks();
    }

    int multipurposeStaffrifleReducedCooldownMinimumTicks() {
        return multipurposeStaffrifleConfig.reducedCooldownMinimumTicks();
    }

    int multipurposeStaffrifleAdsFullAutoIntervalTicks() {
        return multipurposeStaffrifleConfig.adsFullAutoIntervalTicks();
    }

    boolean isMultipurposeStaffrifleSpellDenied(ResourceLocation spellId) {
        return multipurposeStaffrifleConfig.isSpellDenied(spellId);
    }

    boolean focusStaffbowEnableContinuousFocusedCast() {
        return focusStaffbowConfig.enableContinuousFocusedCast();
    }

    boolean focusStaffbowEnableManaLoan() {
        return focusStaffbowConfig.enableManaLoan();
    }

    boolean focusStaffbowEnableArrowCatalystRequirement() {
        return focusStaffbowConfig.enableArrowCatalystRequirement();
    }

    List<ResourceLocation> focusStaffbowArrowCatalystItemIds() {
        return focusStaffbowConfig.arrowCatalystItemIds();
    }

    List<String> focusStaffbowArrowCatalystItems() {
        return focusStaffbowConfig.arrowCatalystItems();
    }

    double focusStaffbowPendingMaxLoanManaRatio() {
        return focusStaffbowConfig.pendingMaxLoanManaRatio();
    }

    boolean isFocusStaffbowSpellDenied(ResourceLocation spellId) {
        return focusStaffbowConfig.isSpellDenied(spellId);
    }

    boolean isFocusStaffbowSpellAllowed(ResourceLocation spellId) {
        return focusStaffbowConfig.isSpellAllowed(spellId);
    }

    List<String> focusStaffbowSpellDenylist() {
        return focusStaffbowConfig.spellDenylist();
    }

    boolean focusStaffbowEnableSpellAllowlist() {
        return focusStaffbowConfig.enableSpellAllowlist();
    }

    List<String> focusStaffbowSpellAllowlist() {
        return focusStaffbowConfig.spellAllowlist();
    }

    FocusStaffbowChargeSettings focusStaffbowChargeSettings() {
        return focusStaffbowConfig.chargeSettings();
    }

    List<String> multipurposeStaffrifleSpellDenylist() {
        return multipurposeStaffrifleConfig.spellDenylist();
    }

    void setMultipurposeStaffrifleSpellDenylistForGameTest(List<String> spellDenylist) {
        multipurposeStaffrifleConfig.setSpellDenylistForGameTest(spellDenylist);
    }

    void setManaShieldCharmConfigForGameTest(
            double manaPerDamage,
            int recoveryThresholdMana,
            double synchronizationManaPerDamage,
            double neutralizationRecoverManaPerDamage,
            int shellArmorDurabilityDamage,
            int invulnerableTimeTicks
    ) {
        manaShieldCharmConfig.setForGameTest(
                manaPerDamage,
                recoveryThresholdMana,
                synchronizationManaPerDamage,
                neutralizationRecoverManaPerDamage,
                shellArmorDurabilityDamage,
                invulnerableTimeTicks
        );
    }

    void setFocusStaffbowConfigForGameTest(
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
        focusStaffbowConfig.setForGameTest(
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
    }

    void setCircuitHeatStaffConfigForGameTest(
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
        circuitHeatStaffConfig.setForGameTest(
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
    }

    void setMulticastEchoStaffConfigForGameTest(
            int multicastDelayTicks,
            double cooldownMultiplier,
            double castTimeCooldownMultiplier,
            int cooldownCapTicks,
            int maxMulticastCount
    ) {
        multicastEchoStaffConfig.setOverridesForGameTest(
                multicastDelayTicks,
                cooldownMultiplier,
                castTimeCooldownMultiplier,
                cooldownCapTicks,
                maxMulticastCount
        );
    }

    void setMulticastEchoStaffMobEffectConfigForGameTest(
            boolean mobEffectProfilesEnabled,
            boolean beneficialMobEffectsEnabled,
            boolean harmfulMobEffectsEnabled,
            boolean neutralMobEffectsEnabled,
            boolean durationServerCapEnabled,
            int durationServerCapTicks,
            boolean amplifierServerCapEnabled,
            int amplifierServerCap
    ) {
        multicastEchoStaffConfig.setMobEffectOverridesForGameTest(
                mobEffectProfilesEnabled,
                beneficialMobEffectsEnabled,
                harmfulMobEffectsEnabled,
                neutralMobEffectsEnabled,
                durationServerCapEnabled,
                durationServerCapTicks,
                amplifierServerCapEnabled,
                amplifierServerCap
        );
    }

    void setMulticastEchoStaffAttackConfigForGameTest(
            boolean attackProfilesEnabled,
            double repeatDamageMultiplier
    ) {
        multicastEchoStaffConfig.setAttackOverridesForGameTest(
                attackProfilesEnabled,
                repeatDamageMultiplier
        );
    }
}
