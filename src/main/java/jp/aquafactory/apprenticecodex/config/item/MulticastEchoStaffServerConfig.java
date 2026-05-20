package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MulticastEchoStaffServerConfig {
    private final ForgeConfigSpec.IntValue multicastDelayTicks;

    private MulticastEchoStaffServerConfig(ForgeConfigSpec.IntValue multicastDelayTicks) {
        this.multicastDelayTicks = multicastDelayTicks;
    }

    public static MulticastEchoStaffServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("MulticastEchoStaff");
        var multicastDelayTicks = builder
                .comment("Delay in ticks between Multicast Echo Staff repeated casts. Must be at least 1 tick.")
                .defineInRange("multicastDelayTicks", 2, 1, 20);
        builder.pop();

        return new MulticastEchoStaffServerConfig(multicastDelayTicks);
    }

    public int multicastDelayTicks() {
        return multicastDelayTicks.get();
    }
}
