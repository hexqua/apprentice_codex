package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.config.spell.AutoMagnetServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.ForceFieldServerConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public final class SpellsServerConfig {
    private final AutoMagnetServerConfig autoMagnetConfig;
    private final ForceFieldServerConfig forceFieldConfig;

    private SpellsServerConfig(
            AutoMagnetServerConfig autoMagnetConfig,
            ForceFieldServerConfig forceFieldConfig
    ) {
        this.autoMagnetConfig = autoMagnetConfig;
        this.forceFieldConfig = forceFieldConfig;
    }

    static SpellsServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("Spells");
        var autoMagnetConfig = AutoMagnetServerConfig.define(builder);
        var forceFieldConfig = ForceFieldServerConfig.define(builder);
        builder.pop();

        return new SpellsServerConfig(
                autoMagnetConfig,
                forceFieldConfig
        );
    }

    boolean autoMagnetDisableCollectManaCost() {
        return autoMagnetConfig.disableCollectManaCost();
    }

    float forceFieldDrainManaBasePerHit() {
        return forceFieldConfig.drainManaBasePerHit();
    }
}
