package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MultipurposeStaffrifleServerConfig {
    private final ModConfigSpec.IntValue cooldownBypassThresholdTicks;
    private final ModConfigSpec.IntValue cooldownReductionTicks;
    private final ModConfigSpec.IntValue reducedCooldownMinimumTicks;
    private final ModConfigSpec.IntValue adsFullAutoIntervalTicks;

    private MultipurposeStaffrifleServerConfig(
            ModConfigSpec.IntValue cooldownBypassThresholdTicks,
            ModConfigSpec.IntValue cooldownReductionTicks,
            ModConfigSpec.IntValue reducedCooldownMinimumTicks,
            ModConfigSpec.IntValue adsFullAutoIntervalTicks
    ) {
        this.cooldownBypassThresholdTicks = cooldownBypassThresholdTicks;
        this.cooldownReductionTicks = cooldownReductionTicks;
        this.reducedCooldownMinimumTicks = reducedCooldownMinimumTicks;
        this.adsFullAutoIntervalTicks = adsFullAutoIntervalTicks;
    }

    public static MultipurposeStaffrifleServerConfig define(ModConfigSpec.Builder builder) {
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
