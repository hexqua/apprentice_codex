package jp.aquafactory.apprenticecodex.config.spell;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ForceFieldServerConfig {
    private final ForgeConfigSpec.DoubleValue drainManaBase;

    private ForceFieldServerConfig(
            ForgeConfigSpec.DoubleValue drainManaBase
    ) {
        this.drainManaBase = drainManaBase;
    }

    public static ForceFieldServerConfig define(ForgeConfigSpec.Builder builder) {
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
