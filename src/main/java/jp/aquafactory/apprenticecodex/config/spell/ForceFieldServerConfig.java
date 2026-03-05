package jp.aquafactory.apprenticecodex.config.spell;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ForceFieldServerConfig {
    private final ModConfigSpec.DoubleValue drainManaBase;

    private ForceFieldServerConfig(
            ModConfigSpec.DoubleValue drainManaBase
    ) {
        this.drainManaBase = drainManaBase;
    }

    public static ForceFieldServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("ForceField");

        var drainManaBase = builder.defineInRange("drainManaBasePerHit", 150.0d, 0d, 10000.0d);

        builder.pop();
        return new ForceFieldServerConfig(
                drainManaBase
        );
    }

    public float drainManaBasePerHit() {
        return drainManaBase.get().floatValue();
    }
}
