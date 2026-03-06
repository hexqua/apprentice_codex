package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ArcanumInAJarServerConfig {
    private final ModConfigSpec.IntValue ticksPerStoredParameter;

    private ArcanumInAJarServerConfig(ModConfigSpec.IntValue ticksPerStoredParameter) {
        this.ticksPerStoredParameter = ticksPerStoredParameter;
    }

    public static ArcanumInAJarServerConfig define(ModConfigSpec.Builder builder) {
        builder.comment("20 tick = 1 sec, default = 1 min.")
                .push("ArcanumInAJar");
        var ticksPerStoredParameter = builder.defineInRange("ticksPerStoredParameter", 1200, 1, Integer.MAX_VALUE);
        builder.pop();

        return new ArcanumInAJarServerConfig(ticksPerStoredParameter);
    }

    public int ticksPerStoredParameter() {
        return ticksPerStoredParameter.get();
    }
}
