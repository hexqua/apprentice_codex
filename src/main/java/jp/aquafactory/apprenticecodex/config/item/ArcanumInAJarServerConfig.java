package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ArcanumInAJarServerConfig {
    private final ForgeConfigSpec.IntValue ticksPerStoredParameter;

    private ArcanumInAJarServerConfig(ForgeConfigSpec.IntValue ticksPerStoredParameter) {
        this.ticksPerStoredParameter = ticksPerStoredParameter;
    }

    public static ArcanumInAJarServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.comment("20tick = 1sec, default = 1min.")
                .push("ArcanumInAJar");
        var ticksPerStoredParameter = builder.defineInRange("ticksPerStoredParameter", 1200, 1, Integer.MAX_VALUE);
        builder.pop();

        return new ArcanumInAJarServerConfig(ticksPerStoredParameter);
    }

    public int ticksPerStoredParameter() {
        return ticksPerStoredParameter.get();
    }
}
