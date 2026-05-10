package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MultipurposeStaffrifleServerConfig {
    private final ForgeConfigSpec.IntValue cooldownBypassThresholdTicks;
    private final ForgeConfigSpec.IntValue cooldownReductionTicks;
    private final ForgeConfigSpec.IntValue reducedCooldownMinimumTicks;
    private final ForgeConfigSpec.IntValue adsFullAutoIntervalTicks;

    private MultipurposeStaffrifleServerConfig(
            ForgeConfigSpec.IntValue cooldownBypassThresholdTicks,
            ForgeConfigSpec.IntValue cooldownReductionTicks,
            ForgeConfigSpec.IntValue reducedCooldownMinimumTicks,
            ForgeConfigSpec.IntValue adsFullAutoIntervalTicks
    ) {
        this.cooldownBypassThresholdTicks = cooldownBypassThresholdTicks;
        this.cooldownReductionTicks = cooldownReductionTicks;
        this.reducedCooldownMinimumTicks = reducedCooldownMinimumTicks;
        this.adsFullAutoIntervalTicks = adsFullAutoIntervalTicks;
    }

    public static MultipurposeStaffrifleServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("MultipurposeStaffrifle");
        var cooldownBypassThresholdTicks = builder
                .comment("Cooldowns at or below this value are removed for Multipurpose Staffrifle special casts. 200 ticks = 10 seconds.")
                .defineInRange("cooldownBypassThresholdTicks", 20 * 10, 0, 72000);
        var cooldownReductionTicks = builder
                .comment("Cooldown ticks subtracted from longer Multipurpose Staffrifle special casts. 600 ticks = 30 seconds.")
                .defineInRange("cooldownReductionTicks", 20 * 30, 0, 72000);
        var reducedCooldownMinimumTicks = builder
                .comment("Minimum cooldown after Multipurpose Staffrifle special cast reduction. 200 ticks = 10 seconds.")
                .defineInRange("reducedCooldownMinimumTicks", 20 * 10, 0, 72000);
        var adsFullAutoIntervalTicks = builder
                .comment("Minimum server-side interval between ADS full-auto special cast attempts.")
                .defineInRange("adsFullAutoIntervalTicks", 3, 1, 72000);
        builder.pop();

        return new MultipurposeStaffrifleServerConfig(
                cooldownBypassThresholdTicks,
                cooldownReductionTicks,
                reducedCooldownMinimumTicks,
                adsFullAutoIntervalTicks
        );
    }

    public int cooldownBypassThresholdTicks() {
        return cooldownBypassThresholdTicks.get();
    }

    public int cooldownReductionTicks() {
        return cooldownReductionTicks.get();
    }

    public int reducedCooldownMinimumTicks() {
        return reducedCooldownMinimumTicks.get();
    }

    public int adsFullAutoIntervalTicks() {
        return adsFullAutoIntervalTicks.get();
    }
}
