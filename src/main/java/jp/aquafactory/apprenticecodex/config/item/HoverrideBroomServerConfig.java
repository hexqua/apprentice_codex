package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class HoverrideBroomServerConfig {
    private final ModConfigSpec.DoubleValue forwardManaCostPerTick;
    private final ModConfigSpec.DoubleValue inertiaGlideManaCostPerTick;
    private final ModConfigSpec.DoubleValue inertiaReleaseManaCost;
    private final ModConfigSpec.DoubleValue inertiaReleaseMinimumSpeedRatio;
    private Values override;

    private HoverrideBroomServerConfig(
            ModConfigSpec.DoubleValue forwardManaCostPerTick,
            ModConfigSpec.DoubleValue inertiaGlideManaCostPerTick,
            ModConfigSpec.DoubleValue inertiaReleaseManaCost,
            ModConfigSpec.DoubleValue inertiaReleaseMinimumSpeedRatio
    ) {
        this.forwardManaCostPerTick = forwardManaCostPerTick;
        this.inertiaGlideManaCostPerTick = inertiaGlideManaCostPerTick;
        this.inertiaReleaseManaCost = inertiaReleaseManaCost;
        this.inertiaReleaseMinimumSpeedRatio = inertiaReleaseMinimumSpeedRatio;
    }

    public static HoverrideBroomServerConfig define(ModConfigSpec.Builder builder) {
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

        builder.pop();
        return new HoverrideBroomServerConfig(
                forwardManaCostPerTick,
                inertiaGlideManaCostPerTick,
                inertiaReleaseManaCost,
                inertiaReleaseMinimumSpeedRatio
        );
    }

    public Values values() {
        return override == null
                ? new Values(
                        forwardManaCostPerTick.get(),
                        inertiaGlideManaCostPerTick.get(),
                        inertiaReleaseManaCost.get(),
                        inertiaReleaseMinimumSpeedRatio.get()
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
            double inertiaReleaseMinimumSpeedRatio
    ) {
    }
}
