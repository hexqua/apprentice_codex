package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class HoverrideBroomServerConfig {
    private final ForgeConfigSpec.DoubleValue forwardManaCostPerTick;
    private final ForgeConfigSpec.DoubleValue inertiaGlideManaCostPerTick;
    private final ForgeConfigSpec.DoubleValue inertiaReleaseManaCost;
    private final ForgeConfigSpec.DoubleValue inertiaReleaseMinimumSpeedRatio;
    private final ForgeConfigSpec.DoubleValue lowManaWarningThreshold;
    private Values override;

    private HoverrideBroomServerConfig(
            ForgeConfigSpec.DoubleValue forwardManaCostPerTick,
            ForgeConfigSpec.DoubleValue inertiaGlideManaCostPerTick,
            ForgeConfigSpec.DoubleValue inertiaReleaseManaCost,
            ForgeConfigSpec.DoubleValue inertiaReleaseMinimumSpeedRatio,
            ForgeConfigSpec.DoubleValue lowManaWarningThreshold
    ) {
        this.forwardManaCostPerTick = forwardManaCostPerTick;
        this.inertiaGlideManaCostPerTick = inertiaGlideManaCostPerTick;
        this.inertiaReleaseManaCost = inertiaReleaseManaCost;
        this.inertiaReleaseMinimumSpeedRatio = inertiaReleaseMinimumSpeedRatio;
        this.lowManaWarningThreshold = lowManaWarningThreshold;
    }

    public static HoverrideBroomServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("HoverrideBroom");

        var forwardManaCostPerTick = builder
                .comment("Mana consumed each tick while accelerating forward.")
                .defineInRange("forwardManaCostPerTick", 1.0D, 0.0D, 10000.0D);
        var inertiaGlideManaCostPerTick = builder
                .comment("Mana consumed each tick while inertia glide is active.")
                .defineInRange("inertiaGlideManaCostPerTick", 0.5D, 0.0D, 10000.0D);
        var inertiaReleaseManaCost = builder
                .comment("Mana consumed when releasing inertia glide. This value is also the mana threshold required to recover from depleted mode. Set to 0 to disable the release cost and recovery threshold.")
                .defineInRange("inertiaReleaseManaCost", 50.0D, 0.0D, 10000.0D);
        var inertiaReleaseMinimumSpeedRatio = builder
                .comment("Minimum horizontal speed after an accepted inertia glide release, expressed as a ratio of the broom's maximum speed.")
                .defineInRange("inertiaReleaseMinimumSpeedRatio", 0.5D, 0.0D, 1.0D);
        var lowManaWarningThreshold = builder
                .comment("Mana level at or below which the rider receives a low mana warning. The warning is rearmed after mana recovers to the inertia glide release cost.")
                .defineInRange("lowManaWarningThreshold", 20.0D, 0.0D, 10000.0D);

        builder.pop();
        return new HoverrideBroomServerConfig(
                forwardManaCostPerTick,
                inertiaGlideManaCostPerTick,
                inertiaReleaseManaCost,
                inertiaReleaseMinimumSpeedRatio,
                lowManaWarningThreshold
        );
    }

    public Values values() {
        return override == null
                ? new Values(
                        forwardManaCostPerTick.get(),
                        inertiaGlideManaCostPerTick.get(),
                        inertiaReleaseManaCost.get(),
                        inertiaReleaseMinimumSpeedRatio.get(),
                        lowManaWarningThreshold.get()
                )
                : override;
    }

    public void setForGameTest(Values values) {
        override = values;
    }

    public record Values(
            double forwardManaCostPerTick,
            double inertiaGlideManaCostPerTick,
            double inertiaReleaseManaCost,
            double inertiaReleaseMinimumSpeedRatio,
            double lowManaWarningThreshold
    ) {
    }
}
