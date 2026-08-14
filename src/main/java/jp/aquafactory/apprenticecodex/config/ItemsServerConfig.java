package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.config.item.CraftsmansDelightServerConfig;
import jp.aquafactory.apprenticecodex.config.item.CircuitHeatStaffServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ChromaticMagiaDressServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ChargecastCatalystbookServerConfig;
import jp.aquafactory.apprenticecodex.config.item.BulwarkGreatshieldServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ElementalBowServerConfig;
import jp.aquafactory.apprenticecodex.config.item.FocusStaffbowServerConfig;
import jp.aquafactory.apprenticecodex.config.item.FloatmountBroomServerConfig;
import jp.aquafactory.apprenticecodex.config.item.HighTierSwingcastStaffServerConfig;
import jp.aquafactory.apprenticecodex.config.item.IsekaiTravelGuidebookServerConfig;
import jp.aquafactory.apprenticecodex.config.item.IronSwingcastStaffServerConfig;
import jp.aquafactory.apprenticecodex.config.item.InstantSearchBrazierServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SoulstainedSteelSwingcastStaffServerConfig;
import jp.aquafactory.apprenticecodex.config.item.LuminousDeviceServerConfig;
import jp.aquafactory.apprenticecodex.config.item.MagicArmorServerConfig;
import jp.aquafactory.apprenticecodex.config.item.MagiCompressorGadgetServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ManaForceBladeServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ParrycastBucklerServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ManaShieldCharmServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ManaThrusterServerConfig;
import jp.aquafactory.apprenticecodex.config.item.MulticastEchoStaffServerConfig;
import jp.aquafactory.apprenticecodex.config.item.MultipurposeStaffrifleServerConfig;
import jp.aquafactory.apprenticecodex.config.item.PastelStaffServerConfig;
import jp.aquafactory.apprenticecodex.config.item.RemoteOwnerCastServerConfig;
import jp.aquafactory.apprenticecodex.config.item.AbsorptionAmplifyAmuletServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ArcaneCinderServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ArchivistsGrimoireServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SatelliteFollowcastAmuletServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ScarletThirstServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellgunServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellCastParryingRingServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellchargedGreatswordServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellStainedRunicTabletServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellThrowableCardServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ZenithStaffServerConfig;
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
    private final ZenithStaffServerConfig zenithStaffConfig;
    private final IsekaiTravelGuidebookServerConfig isekaiTravelGuidebookConfig;
    private final ManaForceBladeServerConfig manaForceBladeConfig;
    private final ParrycastBucklerServerConfig parrycastBucklerConfig;
    private final BulwarkGreatshieldServerConfig bulwarkGreatshieldConfig;
    private final MagiCompressorGadgetServerConfig magiCompressorGadgetConfig;
    private final ManaShieldCharmServerConfig manaShieldCharmConfig;
    private final ManaThrusterServerConfig manaThrusterConfig;
    private final FloatmountBroomServerConfig floatmountBroomConfig;
    private final IronSwingcastStaffServerConfig ironSwingcastStaffConfig;
    private final SoulstainedSteelSwingcastStaffServerConfig soulstainedSteelSwingcastStaffConfig;
    private final HighTierSwingcastStaffServerConfig highTierSwingcastStaffConfig;
    private final CircuitHeatStaffServerConfig circuitHeatStaffConfig;
    private final SpellgunServerConfig spellgunConfig;
    private final MulticastEchoStaffServerConfig multicastEchoStaffConfig;
    private final MultipurposeStaffrifleServerConfig multipurposeStaffrifleConfig;
    private final FocusStaffbowServerConfig focusStaffbowConfig;
    private final ChargecastCatalystbookServerConfig chargecastCatalystbookConfig;
    private final ElementalBowServerConfig elementalBowConfig;
    private final SpellCastParryingRingServerConfig spellCastParryingRingConfig;
    private final SpellchargedGreatswordServerConfig spellchargedGreatswordConfig;
    private final SpellStainedRunicTabletServerConfig spellStainedRunicTabletConfig;
    private final SpellThrowableCardServerConfig spellThrowableCardConfig;
    private final ArchivistsGrimoireServerConfig archivistsGrimoireConfig;
    private final SatelliteFollowcastAmuletServerConfig satelliteFollowcastAmuletConfig;
    private final RemoteOwnerCastServerConfig remoteOwnerCastConfig;
    private final LuminousDeviceServerConfig luminousDeviceConfig;
    private final InstantSearchBrazierServerConfig instantSearchBrazierConfig;

    private ItemsServerConfig(
            ArcaneCinderServerConfig arcaneCinderConfig,
            AbsorptionAmplifyAmuletServerConfig absorptionAmplifyAmuletConfig,
            ScarletThirstServerConfig scarletThirstConfig,
            CraftsmansDelightServerConfig craftsmansDelightConfig,
            MagicArmorServerConfig magicArmorConfig,
            ChromaticMagiaDressServerConfig chromaticMagiaDressConfig,
            PastelStaffServerConfig pastelStaffConfig,
            ZenithStaffServerConfig zenithStaffConfig,
            IsekaiTravelGuidebookServerConfig isekaiTravelGuidebookConfig,
            ManaForceBladeServerConfig manaForceBladeConfig,
            ParrycastBucklerServerConfig parrycastBucklerConfig,
            BulwarkGreatshieldServerConfig bulwarkGreatshieldConfig,
            MagiCompressorGadgetServerConfig magiCompressorGadgetConfig,
            ManaShieldCharmServerConfig manaShieldCharmConfig,
            ManaThrusterServerConfig manaThrusterConfig,
            FloatmountBroomServerConfig floatmountBroomConfig,
            IronSwingcastStaffServerConfig ironSwingcastStaffConfig,
            SoulstainedSteelSwingcastStaffServerConfig soulstainedSteelSwingcastStaffConfig,
            HighTierSwingcastStaffServerConfig highTierSwingcastStaffConfig,
            CircuitHeatStaffServerConfig circuitHeatStaffConfig,
            SpellgunServerConfig spellgunConfig,
            MulticastEchoStaffServerConfig multicastEchoStaffConfig,
            MultipurposeStaffrifleServerConfig multipurposeStaffrifleConfig,
            FocusStaffbowServerConfig focusStaffbowConfig,
            ChargecastCatalystbookServerConfig chargecastCatalystbookConfig,
            ElementalBowServerConfig elementalBowConfig,
            SpellCastParryingRingServerConfig spellCastParryingRingConfig,
            SpellchargedGreatswordServerConfig spellchargedGreatswordConfig,
            SpellStainedRunicTabletServerConfig spellStainedRunicTabletConfig,
            SpellThrowableCardServerConfig spellThrowableCardConfig,
            ArchivistsGrimoireServerConfig archivistsGrimoireConfig,
            SatelliteFollowcastAmuletServerConfig satelliteFollowcastAmuletConfig,
            RemoteOwnerCastServerConfig remoteOwnerCastConfig,
            LuminousDeviceServerConfig luminousDeviceConfig,
            InstantSearchBrazierServerConfig instantSearchBrazierConfig
    ) {
        this.arcaneCinderConfig = arcaneCinderConfig;
        this.absorptionAmplifyAmuletConfig = absorptionAmplifyAmuletConfig;
        this.scarletThirstConfig = scarletThirstConfig;
        this.craftsmansDelightConfig = craftsmansDelightConfig;
        this.magicArmorConfig = magicArmorConfig;
        this.chromaticMagiaDressConfig = chromaticMagiaDressConfig;
        this.pastelStaffConfig = pastelStaffConfig;
        this.zenithStaffConfig = zenithStaffConfig;
        this.isekaiTravelGuidebookConfig = isekaiTravelGuidebookConfig;
        this.manaForceBladeConfig = manaForceBladeConfig;
        this.parrycastBucklerConfig = parrycastBucklerConfig;
        this.bulwarkGreatshieldConfig = bulwarkGreatshieldConfig;
        this.magiCompressorGadgetConfig = magiCompressorGadgetConfig;
        this.manaShieldCharmConfig = manaShieldCharmConfig;
        this.manaThrusterConfig = manaThrusterConfig;
        this.floatmountBroomConfig = floatmountBroomConfig;
        this.ironSwingcastStaffConfig = ironSwingcastStaffConfig;
        this.soulstainedSteelSwingcastStaffConfig = soulstainedSteelSwingcastStaffConfig;
        this.highTierSwingcastStaffConfig = highTierSwingcastStaffConfig;
        this.circuitHeatStaffConfig = circuitHeatStaffConfig;
        this.spellgunConfig = spellgunConfig;
        this.multicastEchoStaffConfig = multicastEchoStaffConfig;
        this.multipurposeStaffrifleConfig = multipurposeStaffrifleConfig;
        this.focusStaffbowConfig = focusStaffbowConfig;
        this.chargecastCatalystbookConfig = chargecastCatalystbookConfig;
        this.elementalBowConfig = elementalBowConfig;
        this.spellCastParryingRingConfig = spellCastParryingRingConfig;
        this.spellchargedGreatswordConfig = spellchargedGreatswordConfig;
        this.spellStainedRunicTabletConfig = spellStainedRunicTabletConfig;
        this.spellThrowableCardConfig = spellThrowableCardConfig;
        this.archivistsGrimoireConfig = archivistsGrimoireConfig;
        this.satelliteFollowcastAmuletConfig = satelliteFollowcastAmuletConfig;
        this.remoteOwnerCastConfig = remoteOwnerCastConfig;
        this.luminousDeviceConfig = luminousDeviceConfig;
        this.instantSearchBrazierConfig = instantSearchBrazierConfig;
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
        var zenithStaffConfig = ZenithStaffServerConfig.define(builder);
        var isekaiTravelGuidebookConfig = IsekaiTravelGuidebookServerConfig.define(builder);
        var manaForceBladeConfig = ManaForceBladeServerConfig.define(builder);
        var parrycastBucklerConfig = ParrycastBucklerServerConfig.define(builder);
        var bulwarkGreatshieldConfig = BulwarkGreatshieldServerConfig.define(builder);
        var magiCompressorGadgetConfig = MagiCompressorGadgetServerConfig.define(builder);
        var manaShieldCharmConfig = ManaShieldCharmServerConfig.define(builder);
        var manaThrusterConfig = ManaThrusterServerConfig.define(builder);
        var floatmountBroomConfig = FloatmountBroomServerConfig.define(builder);
        var ironSwingcastStaffConfig = IronSwingcastStaffServerConfig.define(builder);
        var soulstainedSteelSwingcastStaffConfig = SoulstainedSteelSwingcastStaffServerConfig.define(builder);
        var highTierSwingcastStaffConfig = HighTierSwingcastStaffServerConfig.define(builder);
        var circuitHeatStaffConfig = CircuitHeatStaffServerConfig.define(builder);
        var spellgunConfig = SpellgunServerConfig.define(builder);
        var multicastEchoStaffConfig = MulticastEchoStaffServerConfig.define(builder);
        var multipurposeStaffrifleConfig = MultipurposeStaffrifleServerConfig.define(builder);
        var focusStaffbowConfig = FocusStaffbowServerConfig.define(builder);
        var chargecastCatalystbookConfig = ChargecastCatalystbookServerConfig.define(builder);
        var elementalBowConfig = ElementalBowServerConfig.define(builder);
        var spellCastParryingRingConfig = SpellCastParryingRingServerConfig.define(builder);
        var spellchargedGreatswordConfig = SpellchargedGreatswordServerConfig.define(builder);
        var spellStainedRunicTabletConfig = SpellStainedRunicTabletServerConfig.define(builder);
        var spellThrowableCardConfig = SpellThrowableCardServerConfig.define(builder);
        var archivistsGrimoireConfig = ArchivistsGrimoireServerConfig.define(builder);
        var satelliteFollowcastAmuletConfig = SatelliteFollowcastAmuletServerConfig.define(builder);
        var remoteOwnerCastConfig = RemoteOwnerCastServerConfig.define(builder);
        var luminousDeviceConfig = LuminousDeviceServerConfig.define(builder);
        var instantSearchBrazierConfig = InstantSearchBrazierServerConfig.define(builder);
        builder.pop();

        return new ItemsServerConfig(
                arcaneCinderConfig,
                absorptionAmplifyAmuletConfig,
                scarletThirstConfig,
                craftsmansDelightConfig,
                magicArmorConfig,
                chromaticMagiaDressConfig,
                pastelStaffConfig,
                zenithStaffConfig,
                isekaiTravelGuidebookConfig,
                manaForceBladeConfig,
                parrycastBucklerConfig,
                bulwarkGreatshieldConfig,
                magiCompressorGadgetConfig,
                manaShieldCharmConfig,
                manaThrusterConfig,
                floatmountBroomConfig,
                ironSwingcastStaffConfig,
                soulstainedSteelSwingcastStaffConfig,
                highTierSwingcastStaffConfig,
                circuitHeatStaffConfig,
                spellgunConfig,
                multicastEchoStaffConfig,
                multipurposeStaffrifleConfig,
                focusStaffbowConfig,
                chargecastCatalystbookConfig,
                elementalBowConfig,
                spellCastParryingRingConfig,
                spellchargedGreatswordConfig,
                spellStainedRunicTabletConfig,
                spellThrowableCardConfig,
                archivistsGrimoireConfig,
                satelliteFollowcastAmuletConfig,
                remoteOwnerCastConfig,
                luminousDeviceConfig,
                instantSearchBrazierConfig
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

    void setAbsorptionAmplifyAmuletConfigForGameTest(double baseAbsorptionTarget, int recoveryDelayTicks) {
        absorptionAmplifyAmuletConfig.setForGameTest(baseAbsorptionTarget, recoveryDelayTicks);
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

    double craftsmansDelightCooldownMultiplier() {
        return craftsmansDelightConfig.cooldownMultiplier();
    }

    boolean isCraftsmansDelightGracedRainGrowthDenied(ResourceLocation entityTypeId) {
        return craftsmansDelightConfig.isGracedRainGrowthDenied(entityTypeId);
    }

    boolean isCraftsmansDelightGracedRainBreedingCooldownDenied(ResourceLocation entityTypeId) {
        return craftsmansDelightConfig.isGracedRainBreedingCooldownDenied(entityTypeId);
    }

    List<String> craftsmansDelightGracedRainGrowthDenylist() {
        return craftsmansDelightConfig.gracedRainGrowthDenylist();
    }

    List<String> craftsmansDelightGracedRainBreedingCooldownDenylist() {
        return craftsmansDelightConfig.gracedRainBreedingCooldownDenylist();
    }

    double apprenticeMageRobeSpellPowerBonusPerPiece() {
        return magicArmorConfig.apprenticeMageRobeSpellPowerBonusPerPiece();
    }

    double enchantressRobeSpellPowerBonusPerPiece() {
        return magicArmorConfig.enchantressRobeSpellPowerBonusPerPiece();
    }

    double soulcollectorRobeSpellPowerBonusPerPiece() {
        return magicArmorConfig.soulcollectorRobeSpellPowerBonusPerPiece();
    }

    double soulcollectorRobeMagicProficiencyBonusPerPiece() {
        return magicArmorConfig.soulcollectorRobeMagicProficiencyBonusPerPiece();
    }

    double chromaticMagiaDressSpellPowerBonusPerPiece() {
        return magicArmorConfig.chromaticMagiaDressSpellPowerBonusPerPiece();
    }

    double elementMaidenRobeSpellPowerBonus() {
        return magicArmorConfig.elementMaidenRobeSpellPowerBonus();
    }

    double elementMaidenRobeSchoolSpellPowerBonus() {
        return magicArmorConfig.elementMaidenRobeSchoolSpellPowerBonus();
    }

    double magiAgentSuitSpellPowerBonus() {
        return magicArmorConfig.magiAgentSuitSpellPowerBonus();
    }

    double magiAgentSuitSchoolSpellPowerBonus() {
        return magicArmorConfig.magiAgentSuitSchoolSpellPowerBonus();
    }

    double magiAgentSuitBootsCooldownMultiplier() {
        return magicArmorConfig.magiAgentSuitBootsCooldownMultiplier();
    }

    double magiAgentSuitBootsCastTimeMultiplier() {
        return magicArmorConfig.magiAgentSuitBootsCastTimeMultiplier();
    }

    double magiAgentSuitAmmoNoConsumeChance() {
        return magicArmorConfig.magiAgentSuitAmmoNoConsumeChance();
    }

    boolean magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed() {
        return magicArmorConfig.magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed();
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

    float zenithStaffManaCostMultiplier() {
        return zenithStaffConfig.manaCostMultiplier();
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

    int manaForceBladeReleaseCooldownTicks() {
        return manaForceBladeConfig.releaseCooldownTicks();
    }

    int manaForceBladePerfectGuardReleaseCooldownGraceTicks() {
        return manaForceBladeConfig.perfectGuardReleaseCooldownGraceTicks();
    }

    int manaForceBladePerfectGuardReleaseCooldownGraceUses() {
        return manaForceBladeConfig.perfectGuardReleaseCooldownGraceUses();
    }

    int parrycastBucklerReleaseCooldownTicks() {
        return parrycastBucklerConfig.releaseCooldownTicks();
    }

    int parrycastBucklerPerfectGuardTicks() {
        return parrycastBucklerConfig.perfectGuardTicks();
    }

    int parrycastBucklerPerfectGuardReleaseCooldownGraceTicks() {
        return parrycastBucklerConfig.perfectGuardReleaseCooldownGraceTicks();
    }

    int parrycastBucklerPerfectGuardReleaseCooldownGraceUses() {
        return parrycastBucklerConfig.perfectGuardReleaseCooldownGraceUses();
    }

    double bulwarkGreatshieldGenericSpellResist() {
        return bulwarkGreatshieldConfig.genericSpellResist();
    }

    double bulwarkGreatshieldSchoolSpellResist() {
        return bulwarkGreatshieldConfig.schoolSpellResist();
    }

    float manaShieldCharmManaPerDamage() {
        return manaShieldCharmConfig.manaPerDamage();
    }

    float magiCompressorGadgetManaCostPerSecond() {
        return magiCompressorGadgetConfig.manaCostPerSecond();
    }

    float magiCompressorGadgetAirFillPerSecond() {
        return magiCompressorGadgetConfig.airFillPerSecond();
    }

    float magiCompressorGadgetMaxAir() {
        return magiCompressorGadgetConfig.maxAir();
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

    float manaThrusterManaCostPerTick() {
        return manaThrusterConfig.manaCostPerTick();
    }

    FloatmountBroomServerConfig.Values floatmountBroomConfig() {
        return floatmountBroomConfig.values();
    }

    int multipurposeStaffrifleCooldownBypassThresholdTicks() {
        return multipurposeStaffrifleConfig.cooldownBypassThresholdTicks();
    }

    int circuitHeatStaffAdditionalManaReferenceCooldownTicks() {
        return circuitHeatStaffConfig.additionalManaReferenceCooldownTicks();
    }

    double ironSwingcastStaffCrystallineArcaneShardDropChance() {
        return ironSwingcastStaffConfig.crystallineArcaneShardDropChance();
    }

    double soulstainedSteelSwingcastStaffManaCostPerBlade() {
        return soulstainedSteelSwingcastStaffConfig.manaCostPerBlade();
    }

    int diamondSwingcastStaffCooldownReductionTicks() {
        return highTierSwingcastStaffConfig.diamondCooldownReductionTicks();
    }

    int netheriteSwingcastStaffCooldownReductionTicks() {
        return highTierSwingcastStaffConfig.netheriteCooldownReductionTicks();
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

    int ironSpellgunMaxInstantImbueCooldownTicks() {
        return spellgunConfig.ironMaxInstantImbueCooldownTicks();
    }

    int ironSpellgunOverriddenSpellCooldownTicks() {
        return spellgunConfig.ironOverriddenSpellCooldownTicks();
    }

    int copperSpellgunMaxInstantImbueCooldownTicks() {
        return spellgunConfig.copperMaxInstantImbueCooldownTicks();
    }

    int copperSpellgunOverriddenSpellCooldownTicks() {
        return spellgunConfig.copperOverriddenSpellCooldownTicks();
    }

    int goldSpellgunReducedCooldownMinimumTicks() {
        return spellgunConfig.goldReducedCooldownMinimumTicks();
    }

    int goldSpellgunCooldownReductionTicks() {
        return spellgunConfig.goldCooldownReductionTicks();
    }

    boolean ironSpellgunIgnoreMaxMana() {
        return spellgunConfig.ironIgnoreMaxMana();
    }

    boolean copperSpellgunIgnoreMaxMana() {
        return spellgunConfig.copperIgnoreMaxMana();
    }

    boolean goldSpellgunIgnoreMaxMana() {
        return spellgunConfig.goldIgnoreMaxMana();
    }

    boolean diamondSpellgunIgnoreMaxMana() {
        return spellgunConfig.diamondIgnoreMaxMana();
    }

    double malignantSpellgunForcedSpellPower() {
        return spellgunConfig.malignantForcedSpellPower();
    }

    double malignantSpellgunForcedSchoolSpellPower() {
        return spellgunConfig.malignantForcedSchoolSpellPower();
    }

    double malignantSpellgunForcedSummonDamage() {
        return spellgunConfig.malignantForcedSummonDamage();
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

    ChargecastCatalystbookServerConfig.Values chargecastCatalystbookConfig() {
        return chargecastCatalystbookConfig.values();
    }

    void setChargecastCatalystbookConfigForGameTest(ChargecastCatalystbookServerConfig.Values values) {
        chargecastCatalystbookConfig.setForGameTest(values);
    }

    double elementalBowMagicReadyDrawTicksMultiplier() {
        return elementalBowConfig.magicReadyDrawTicksMultiplier();
    }

    List<ResourceLocation> elementalBowMagicArrowCatalystItemIds() {
        return elementalBowConfig.magicArrowCatalystItemIds();
    }

    List<String> elementalBowMagicArrowCatalystItems() {
        return elementalBowConfig.magicArrowCatalystItems();
    }

    float elementalBowOverheatAdditionalManaLinearMultiplier() {
        return elementalBowConfig.overheatAdditionalManaLinearMultiplier();
    }

    float elementalBowOverheatAdditionalManaQuadraticMultiplier() {
        return elementalBowConfig.overheatAdditionalManaQuadraticMultiplier();
    }

    double elementalBowOverheatDurationMultiplier() {
        return elementalBowConfig.overheatDurationMultiplier();
    }

    int elementalBowOverheatDurationMinTicks() {
        return elementalBowConfig.overheatDurationMinTicks();
    }

    int elementalBowOverheatDurationCapTicks() {
        return elementalBowConfig.overheatDurationCapTicks();
    }

    double elementalBowPowerArrowSpellLevelBonusPerLevel() {
        return elementalBowConfig.powerArrowSpellLevelBonusPerLevel();
    }

    SpellStainedRunicTabletServerConfig.Values spellStainedRunicTabletConfig() {
        return spellStainedRunicTabletConfig.values();
    }

    SpellThrowableCardServerConfig.Values spellThrowableCardConfig() {
        return spellThrowableCardConfig.values();
    }

    int spellInvokeCardCraftCount() {
        return spellThrowableCardConfig.spellInvokeCardCraftCount();
    }

    int spellAutonomyCardCraftCount() {
        return spellThrowableCardConfig.spellAutonomyCardCraftCount();
    }

    int archivistsGrimoireInitialRows() {
        return archivistsGrimoireConfig.initialRows();
    }

    int archivistsGrimoireEffectiveMaxRows() {
        return archivistsGrimoireConfig.effectiveMaxRows();
    }

    boolean isSatelliteFollowcastAmuletSpellDenied(ResourceLocation spellId) {
        return satelliteFollowcastAmuletConfig.isSpellDenied(spellId);
    }

    List<String> satelliteFollowcastAmuletSpellDenylist() {
        return satelliteFollowcastAmuletConfig.spellDenylist();
    }

    boolean remoteOwnerCastEnableRemotePlayerGeometry() {
        return remoteOwnerCastConfig.enableRemotePlayerGeometry();
    }

    boolean isRemoteOwnerCastSpellDenied(ResourceLocation spellId) {
        return remoteOwnerCastConfig.isRemoteOwnerCastSpellDenied(spellId);
    }

    List<String> remoteOwnerCastDenylist() {
        return remoteOwnerCastConfig.remoteOwnerCastDenylist();
    }

    int luminousDeviceMaxStoredItems() {
        return luminousDeviceConfig.maxStoredItems();
    }

    int luminousDeviceMaxStoredMana() {
        return luminousDeviceConfig.maxStoredMana();
    }

    int luminousDeviceUpgradedMaxStoredMana() {
        return luminousDeviceConfig.upgradedMaxStoredMana();
    }

    int luminousDeviceCleanRadius() {
        return luminousDeviceConfig.cleanRadius();
    }

    int luminousDeviceMageLightManaRecovery() {
        return luminousDeviceConfig.mageLightManaRecovery();
    }

    int luminousDeviceWizardlampManaRecovery() {
        return luminousDeviceConfig.wizardlampManaRecovery();
    }

    double luminousDeviceMageLightExtendedRange() {
        return luminousDeviceConfig.mageLightExtendedRange();
    }

    int instantSearchBrazierInitialRange() {
        return instantSearchBrazierConfig.initialRange();
    }

    List<String> multipurposeStaffrifleSpellDenylist() {
        return multipurposeStaffrifleConfig.spellDenylist();
    }

    SpellgunServerConfig.Values spellgunConfig() {
        return spellgunConfig.values();
    }

    void setSpellgunConfigForGameTest(SpellgunServerConfig.Values values) {
        spellgunConfig.setForGameTest(values);
    }

    void setCraftsmansDelightGracedRainDenylistsForGameTest(
            List<String> gracedRainGrowthDenylist,
            List<String> gracedRainBreedingCooldownDenylist
    ) {
        craftsmansDelightConfig.setGracedRainDenylistsForGameTest(
                gracedRainGrowthDenylist,
                gracedRainBreedingCooldownDenylist
        );
    }

    void setEquipmentSpellTimingMultipliersForGameTest(
            double craftsmansDelightCooldownMultiplier,
            double magiAgentSuitBootsCooldownMultiplier,
            double magiAgentSuitBootsCastTimeMultiplier
    ) {
        craftsmansDelightConfig.setCooldownMultiplierForGameTest(craftsmansDelightCooldownMultiplier);
        magicArmorConfig.setMagiAgentSuitBootsCooldownMultiplierForGameTest(magiAgentSuitBootsCooldownMultiplier);
        magicArmorConfig.setMagiAgentSuitBootsCastTimeMultiplierForGameTest(magiAgentSuitBootsCastTimeMultiplier);
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

    void setManaThrusterConfigForGameTest(double manaCostPerTick) {
        manaThrusterConfig.setForGameTest(manaCostPerTick);
    }

    void setFloatmountBroomConfigForGameTest(FloatmountBroomServerConfig.Values values) {
        floatmountBroomConfig.setForGameTest(values);
    }

    void setMagiCompressorGadgetConfigForGameTest(
            double manaCostPerSecond,
            double airFillPerSecond,
            double maxAir
    ) {
        magiCompressorGadgetConfig.setForGameTest(manaCostPerSecond, airFillPerSecond, maxAir);
    }

    void setBulwarkGreatshieldConfigForGameTest(double genericSpellResist, double schoolSpellResist) {
        bulwarkGreatshieldConfig.setForGameTest(genericSpellResist, schoolSpellResist);
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

    void setElementalBowConfigForGameTest(
            double magicReadyDrawTicksMultiplier,
            double overheatAdditionalManaLinearMultiplier,
            double overheatAdditionalManaQuadraticMultiplier,
            double overheatDurationMultiplier,
            int overheatDurationMinTicks,
            int overheatDurationCapTicks,
            double powerArrowSpellLevelBonusPerLevel
    ) {
        elementalBowConfig.setForGameTest(
                magicReadyDrawTicksMultiplier,
                overheatAdditionalManaLinearMultiplier,
                overheatAdditionalManaQuadraticMultiplier,
                overheatDurationMultiplier,
                overheatDurationMinTicks,
                overheatDurationCapTicks,
                powerArrowSpellLevelBonusPerLevel
        );
    }

    void setElementalBowMagicArrowCatalystItemsForGameTest(List<String> magicArrowCatalystItems) {
        elementalBowConfig.setMagicArrowCatalystItemsForGameTest(magicArrowCatalystItems);
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

    void setIronSwingcastStaffConfigForGameTest(double crystallineArcaneShardDropChance) {
        ironSwingcastStaffConfig.setForGameTest(crystallineArcaneShardDropChance);
    }

    void setSoulstainedSteelSwingcastStaffConfigForGameTest(double manaCostPerBlade) {
        soulstainedSteelSwingcastStaffConfig.setForGameTest(manaCostPerBlade);
    }

    void setHighTierSwingcastStaffConfigForGameTest(int diamondTicks, int netheriteTicks) {
        highTierSwingcastStaffConfig.setForGameTest(diamondTicks, netheriteTicks);
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

    void setManaForceBladeCooldownConfigForGameTest(
            int releaseCooldownTicks,
            int perfectGuardReleaseCooldownGraceTicks,
            int perfectGuardReleaseCooldownGraceUses
    ) {
        manaForceBladeConfig.setCooldownConfigForGameTest(
                releaseCooldownTicks,
                perfectGuardReleaseCooldownGraceTicks,
                perfectGuardReleaseCooldownGraceUses
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

    int spellCastParryingRingParryWindowTicks() {
        return spellCastParryingRingConfig.parryWindowTicks();
    }

    SpellchargedGreatswordServerConfig.Values spellchargedGreatswordConfig() {
        return spellchargedGreatswordConfig.values();
    }

    void setSpellCastParryingRingParryWindowTicksForGameTest(int value) {
        spellCastParryingRingConfig.setParryWindowTicksForGameTest(value);
    }

    void setSpellchargedGreatswordConfigForGameTest(SpellchargedGreatswordServerConfig.Values values) {
        spellchargedGreatswordConfig.setForGameTest(values);
    }

    void setSpellStainedRunicTabletConfigForGameTest(SpellStainedRunicTabletServerConfig.Values values) {
        spellStainedRunicTabletConfig.setForGameTest(values);
    }

    void setSpellThrowableCardConfigForGameTest(SpellThrowableCardServerConfig.Values values) {
        spellThrowableCardConfig.setForGameTest(values);
    }

    void setArchivistsGrimoireConfigForGameTest(ArchivistsGrimoireServerConfig.Values values) {
        archivistsGrimoireConfig.setForGameTest(values);
    }

    void setElementMaidenRobeSchoolSpellPowerBonusForGameTest(double value) {
        magicArmorConfig.setElementMaidenRobeSchoolSpellPowerBonusForGameTest(value);
    }

    void setMagiAgentSuitAmmoConfigForGameTest(
            double ammoNoConsumeChance,
            boolean skipStaffrifleManaCostWhenAmmoNotConsumed
    ) {
        magicArmorConfig.setMagiAgentSuitAmmoConfigForGameTest(
                ammoNoConsumeChance,
                skipStaffrifleManaCostWhenAmmoNotConsumed
        );
    }

    void setZenithStaffManaCostMultiplierForGameTest(double value) {
        zenithStaffConfig.setManaCostMultiplierForGameTest(value);
    }

    void setSatelliteFollowcastAmuletSpellDenylistForGameTest(List<String> spellDenylist) {
        satelliteFollowcastAmuletConfig.setSpellDenylistForGameTest(spellDenylist);
    }

    void setRemoteOwnerCastConfigForGameTest(
            boolean enableRemotePlayerGeometry,
            List<String> remoteOwnerCastDenylist
    ) {
        remoteOwnerCastConfig.setForGameTest(
                enableRemotePlayerGeometry,
                remoteOwnerCastDenylist
        );
    }

    void setLuminousDeviceConfigForGameTest(int maxStoredItems, int maxStoredMana) {
        luminousDeviceConfig.setForGameTest(maxStoredItems, maxStoredMana);
    }

    void setLuminousDeviceUpgradedMaxStoredManaForGameTest(int maxStoredMana) {
        luminousDeviceConfig.setUpgradedMaxStoredManaForGameTest(maxStoredMana);
    }

    void setLuminousDeviceMageLightExtendedRangeForGameTest(double range) {
        luminousDeviceConfig.setMageLightExtendedRangeForGameTest(range);
    }

    void setInstantSearchBrazierInitialRangeForGameTest(int initialRange) {
        instantSearchBrazierConfig.setForGameTest(initialRange);
    }

    void setLuminousDeviceCleanConfigForGameTest(
            int cleanRadius,
            int mageLightManaRecovery,
            int wizardlampManaRecovery
    ) {
        luminousDeviceConfig.setCleanForGameTest(
                cleanRadius,
                mageLightManaRecovery,
                wizardlampManaRecovery
        );
    }
}
