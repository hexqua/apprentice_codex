package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MulticastEchoStaffServerConfig {
    private final ForgeConfigSpec.IntValue multicastDelayTicks;
    private final ForgeConfigSpec.DoubleValue cooldownMultiplier;
    private final ForgeConfigSpec.DoubleValue castTimeCooldownMultiplier;
    private final ForgeConfigSpec.IntValue cooldownCapTicks;
    private final ForgeConfigSpec.IntValue maxMulticastCount;
    private Integer multicastDelayTicksOverride;
    private Double cooldownMultiplierOverride;
    private Double castTimeCooldownMultiplierOverride;
    private Integer cooldownCapTicksOverride;
    private Integer maxMulticastCountOverride;

    private MulticastEchoStaffServerConfig(
            ForgeConfigSpec.IntValue multicastDelayTicks,
            ForgeConfigSpec.DoubleValue cooldownMultiplier,
            ForgeConfigSpec.DoubleValue castTimeCooldownMultiplier,
            ForgeConfigSpec.IntValue cooldownCapTicks,
            ForgeConfigSpec.IntValue maxMulticastCount
    ) {
        this.multicastDelayTicks = multicastDelayTicks;
        this.cooldownMultiplier = cooldownMultiplier;
        this.castTimeCooldownMultiplier = castTimeCooldownMultiplier;
        this.cooldownCapTicks = cooldownCapTicks;
        this.maxMulticastCount = maxMulticastCount;
    }

    public static MulticastEchoStaffServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("MulticastEchoStaff");
        var multicastDelayTicks = builder
                .comment("Delay in ticks between Multicast Echo Staff repeated casts. Must be at least 1 tick.")
                .defineInRange("multicastDelayTicks", 2, 1, 20);
        var cooldownMultiplier = builder
                .comment("Cooldown multiplier applied per EchoSpell stack when Multicast Echo Staff finishes.")
                .defineInRange("cooldownMultiplier", 1.2D, 0.0D, 100.0D);
        var castTimeCooldownMultiplier = builder
                .comment("Cooldown ticks added from skipped cast time per repeated Multicast Echo Staff cast.")
                .defineInRange("castTimeCooldownMultiplier", 1.0D, 0.0D, 100.0D);
        var cooldownCapTicks = builder
                .comment("Maximum adjusted Multicast Echo Staff cooldown in ticks. Original cooldowns above this value are preserved.")
                .defineInRange("cooldownCapTicks", 12000, 1, 72000);
        var maxMulticastCount = builder
                .comment("Maximum number of repeated casts stored by Echo Cast for Multicast Echo Staff.")
                .defineInRange("maxMulticastCount", 10, 1, 20);
        builder.pop();

        return new MulticastEchoStaffServerConfig(
                multicastDelayTicks,
                cooldownMultiplier,
                castTimeCooldownMultiplier,
                cooldownCapTicks,
                maxMulticastCount
        );
    }

    public int multicastDelayTicks() {
        return multicastDelayTicksOverride == null ? multicastDelayTicks.get() : multicastDelayTicksOverride;
    }

    public double cooldownMultiplier() {
        return cooldownMultiplierOverride == null ? cooldownMultiplier.get() : cooldownMultiplierOverride;
    }

    public double castTimeCooldownMultiplier() {
        return castTimeCooldownMultiplierOverride == null
                ? castTimeCooldownMultiplier.get()
                : castTimeCooldownMultiplierOverride;
    }

    public int cooldownCapTicks() {
        return cooldownCapTicksOverride == null ? cooldownCapTicks.get() : cooldownCapTicksOverride;
    }

    public int maxMulticastCount() {
        return maxMulticastCountOverride == null ? maxMulticastCount.get() : maxMulticastCountOverride;
    }

    public void setOverridesForGameTest(
            int multicastDelayTicks,
            double cooldownMultiplier,
            double castTimeCooldownMultiplier,
            int cooldownCapTicks,
            int maxMulticastCount
    ) {
        this.multicastDelayTicksOverride = multicastDelayTicks;
        this.cooldownMultiplierOverride = cooldownMultiplier;
        this.castTimeCooldownMultiplierOverride = castTimeCooldownMultiplier;
        this.cooldownCapTicksOverride = cooldownCapTicks;
        this.maxMulticastCountOverride = maxMulticastCount;
    }
}
