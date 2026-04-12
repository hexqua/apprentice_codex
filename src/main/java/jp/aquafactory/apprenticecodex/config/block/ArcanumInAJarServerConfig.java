package jp.aquafactory.apprenticecodex.config.block;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ArcanumInAJarServerConfig {
    private final ForgeConfigSpec.IntValue ticksPerStoredParameter;

    private ArcanumInAJarServerConfig(ForgeConfigSpec.IntValue ticksPerStoredParameter) {
        this.ticksPerStoredParameter = ticksPerStoredParameter;
    }

    public static ArcanumInAJarServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.comment("20 ticks = 1 second. Default = 1 minute.")
                .push("ArcanumInAJar");
        var ticksPerStoredParameter = builder.defineInRange("ticksPerStoredParameter", 3600, 1, Integer.MAX_VALUE);
        builder.pop();

        return new ArcanumInAJarServerConfig(ticksPerStoredParameter);
    }

    public int ticksPerStoredParameter() {
        return ticksPerStoredParameter.get();
    }
}
