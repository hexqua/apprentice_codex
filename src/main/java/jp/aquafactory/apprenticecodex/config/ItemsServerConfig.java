package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.config.item.CraftsmansDelightServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ChromaticMagiaDressServerConfig;
import jp.aquafactory.apprenticecodex.config.item.IsekaiTravelGuidebookServerConfig;
import jp.aquafactory.apprenticecodex.config.item.MagicArmorServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ManaForceBladeServerConfig;
import jp.aquafactory.apprenticecodex.config.item.MulticastEchoStaffServerConfig;
import jp.aquafactory.apprenticecodex.config.item.MultipurposeStaffrifleServerConfig;
import jp.aquafactory.apprenticecodex.config.item.PastelStaffServerConfig;
import jp.aquafactory.apprenticecodex.config.item.AbsorptionAmplifyAmuletServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ArcaneCinderServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ScarletThirstServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

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
    private final MulticastEchoStaffServerConfig multicastEchoStaffConfig;
    private final MultipurposeStaffrifleServerConfig multipurposeStaffrifleConfig;

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
            MulticastEchoStaffServerConfig multicastEchoStaffConfig,
            MultipurposeStaffrifleServerConfig multipurposeStaffrifleConfig
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
        this.multicastEchoStaffConfig = multicastEchoStaffConfig;
        this.multipurposeStaffrifleConfig = multipurposeStaffrifleConfig;
    }

    static ItemsServerConfig define(ModConfigSpec.Builder builder) {
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
        var multicastEchoStaffConfig = MulticastEchoStaffServerConfig.define(builder);
        var multipurposeStaffrifleConfig = MultipurposeStaffrifleServerConfig.define(builder);
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
                multicastEchoStaffConfig,
                multipurposeStaffrifleConfig
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

    int multipurposeStaffrifleCooldownBypassThresholdTicks() {
        return multipurposeStaffrifleConfig.cooldownBypassThresholdTicks();
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

    List<String> multipurposeStaffrifleSpellDenylist() {
        return multipurposeStaffrifleConfig.spellDenylist();
    }

    void setMultipurposeStaffrifleSpellDenylistForGameTest(List<String> spellDenylist) {
        multipurposeStaffrifleConfig.setSpellDenylistForGameTest(spellDenylist);
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

