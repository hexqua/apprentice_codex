package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.config.item.ArcanumInAJarServerConfig;
import jp.aquafactory.apprenticecodex.config.item.CraftsmansDelightServerConfig;
import jp.aquafactory.apprenticecodex.config.item.PastelStaffServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ScarletThirstServerConfig;
import net.minecraftforge.common.ForgeConfigSpec;

final class ItemsServerConfig {
    private final ArcanumInAJarServerConfig arcanumInAJarConfig;
    private final ScarletThirstServerConfig scarletThirstConfig;
    private final CraftsmansDelightServerConfig craftsmansDelightConfig;
    private final PastelStaffServerConfig pastelStaffConfig;

    private ItemsServerConfig(
            ArcanumInAJarServerConfig arcanumInAJarConfig,
            ScarletThirstServerConfig scarletThirstConfig,
            CraftsmansDelightServerConfig craftsmansDelightConfig,
            PastelStaffServerConfig pastelStaffConfig
    ) {
        this.arcanumInAJarConfig = arcanumInAJarConfig;
        this.scarletThirstConfig = scarletThirstConfig;
        this.craftsmansDelightConfig = craftsmansDelightConfig;
        this.pastelStaffConfig = pastelStaffConfig;
    }

    static ItemsServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("Items");
        var arcanumInAJarConfig = ArcanumInAJarServerConfig.define(builder);
        var scarletThirstConfig = ScarletThirstServerConfig.define(builder);
        var craftsmansDelightConfig = CraftsmansDelightServerConfig.define(builder);
        var pastelStaffConfig = PastelStaffServerConfig.define(builder);
        builder.pop();

        return new ItemsServerConfig(
                arcanumInAJarConfig,
                scarletThirstConfig,
                craftsmansDelightConfig,
                pastelStaffConfig
        );
    }

    int arcanumInAJarTicksPerStoredParameter() {
        return arcanumInAJarConfig.ticksPerStoredParameter();
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
}
