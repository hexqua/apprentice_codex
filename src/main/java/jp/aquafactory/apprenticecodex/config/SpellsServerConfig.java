package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.config.spell.ForceFieldServerConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class SpellsServerConfig {
    private final ForceFieldServerConfig forceFieldConfig;

    private SpellsServerConfig(
            ForceFieldServerConfig forceFieldConfig
    ) {
        this.forceFieldConfig = forceFieldConfig;
    }

    static SpellsServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("Spells");
        var forceFieldConfig = ForceFieldServerConfig.define(builder);
        builder.pop();

        return new SpellsServerConfig(
                forceFieldConfig
        );
    }

    float forceFieldDrainManaBasePerHit() {
        return forceFieldConfig.drainManaBasePerHit();
    }
}
