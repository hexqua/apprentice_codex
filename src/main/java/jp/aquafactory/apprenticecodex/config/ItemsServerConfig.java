package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.config.item.CraftsmansDelightServerConfig;
import jp.aquafactory.apprenticecodex.config.item.IsekaiTravelGuidebookServerConfig;
import jp.aquafactory.apprenticecodex.config.item.PastelStaffServerConfig;
import jp.aquafactory.apprenticecodex.config.item.AbsorptionAmplifyAmuletServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ArcaneCinderServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ScarletThirstServerConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

final class ItemsServerConfig {
    private final ArcaneCinderServerConfig arcaneCinderConfig;
    private final AbsorptionAmplifyAmuletServerConfig absorptionAmplifyAmuletConfig;
    private final ScarletThirstServerConfig scarletThirstConfig;
    private final CraftsmansDelightServerConfig craftsmansDelightConfig;
    private final PastelStaffServerConfig pastelStaffConfig;
    private final IsekaiTravelGuidebookServerConfig isekaiTravelGuidebookConfig;

    private ItemsServerConfig(
            ArcaneCinderServerConfig arcaneCinderConfig,
            AbsorptionAmplifyAmuletServerConfig absorptionAmplifyAmuletConfig,
            ScarletThirstServerConfig scarletThirstConfig,
            CraftsmansDelightServerConfig craftsmansDelightConfig,
            PastelStaffServerConfig pastelStaffConfig,
            IsekaiTravelGuidebookServerConfig isekaiTravelGuidebookConfig
    ) {
        this.arcaneCinderConfig = arcaneCinderConfig;
        this.absorptionAmplifyAmuletConfig = absorptionAmplifyAmuletConfig;
        this.scarletThirstConfig = scarletThirstConfig;
        this.craftsmansDelightConfig = craftsmansDelightConfig;
        this.pastelStaffConfig = pastelStaffConfig;
        this.isekaiTravelGuidebookConfig = isekaiTravelGuidebookConfig;
    }

    static ItemsServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("Items");
        var arcaneCinderConfig = ArcaneCinderServerConfig.define(builder);
        var absorptionAmplifyAmuletConfig = AbsorptionAmplifyAmuletServerConfig.define(builder);
        var scarletThirstConfig = ScarletThirstServerConfig.define(builder);
        var craftsmansDelightConfig = CraftsmansDelightServerConfig.define(builder);
        var pastelStaffConfig = PastelStaffServerConfig.define(builder);
        var isekaiTravelGuidebookConfig = IsekaiTravelGuidebookServerConfig.define(builder);
        builder.pop();

        return new ItemsServerConfig(
                arcaneCinderConfig,
                absorptionAmplifyAmuletConfig,
                scarletThirstConfig,
                craftsmansDelightConfig,
                pastelStaffConfig,
                isekaiTravelGuidebookConfig
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

    double pastelStaffAmplifyTintedMagicMultiplier() {
        return pastelStaffConfig.amplifyTintedMagicMultiplier();
    }

    boolean showIsekaiTravelGuidebookTooltip() {
        return isekaiTravelGuidebookConfig.showTooltip();
    }

    boolean enableIsekaiTravelGuidebookBonusChestLoot() {
        return isekaiTravelGuidebookConfig.enableBonusChestLoot();
    }
}

