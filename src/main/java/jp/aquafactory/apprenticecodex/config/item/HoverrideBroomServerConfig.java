package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class HoverrideBroomServerConfig {
    // 枯渇・警告のepsilon判定で実質0として扱われない、十分に小さい正数を下限にする。
    private static final double MIN_INERTIA_RELEASE_MANA_COST = 1.0e-3D;
    public static final double DEFAULT_OVERDRIVE_FORWARD_MANA_COST_PER_TICK = 2.0D;
    public static final double DEFAULT_OVERDRIVE_INERTIA_GLIDE_MANA_COST_PER_TICK = 0.5D;
    public static final double DEFAULT_OVERDRIVE_INERTIA_RELEASE_MANA_COST = 100.0D;
    private final ForgeConfigSpec.DoubleValue forwardManaCostPerTick;
    private final ForgeConfigSpec.DoubleValue inertiaGlideManaCostPerTick;
    private final ForgeConfigSpec.DoubleValue inertiaReleaseManaCost;
    private final ForgeConfigSpec.DoubleValue overdriveForwardManaCostPerTick;
    private final ForgeConfigSpec.DoubleValue overdriveInertiaGlideManaCostPerTick;
    private final ForgeConfigSpec.DoubleValue overdriveInertiaReleaseManaCost;
    private final ForgeConfigSpec.DoubleValue inertiaReleaseMinimumSpeedRatio;
    private final ForgeConfigSpec.DoubleValue lowManaWarningThreshold;
    private Values override;

    private HoverrideBroomServerConfig(
            ForgeConfigSpec.DoubleValue forwardManaCostPerTick,
            ForgeConfigSpec.DoubleValue inertiaGlideManaCostPerTick,
            ForgeConfigSpec.DoubleValue inertiaReleaseManaCost,
            ForgeConfigSpec.DoubleValue overdriveForwardManaCostPerTick,
            ForgeConfigSpec.DoubleValue overdriveInertiaGlideManaCostPerTick,
            ForgeConfigSpec.DoubleValue overdriveInertiaReleaseManaCost,
            ForgeConfigSpec.DoubleValue inertiaReleaseMinimumSpeedRatio,
            ForgeConfigSpec.DoubleValue lowManaWarningThreshold
    ) {
        this.forwardManaCostPerTick = forwardManaCostPerTick;
        this.inertiaGlideManaCostPerTick = inertiaGlideManaCostPerTick;
        this.inertiaReleaseManaCost = inertiaReleaseManaCost;
        this.overdriveForwardManaCostPerTick = overdriveForwardManaCostPerTick;
        this.overdriveInertiaGlideManaCostPerTick = overdriveInertiaGlideManaCostPerTick;
        this.overdriveInertiaReleaseManaCost = overdriveInertiaReleaseManaCost;
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
                .comment("Mana consumed when releasing inertia glide. This positive value is also the mana threshold required to recover from depleted mode.")
                .defineInRange("inertiaReleaseManaCost", 50.0D,
                        MIN_INERTIA_RELEASE_MANA_COST, 10000.0D);
        var overdriveForwardManaCostPerTick = builder
                .comment("Mana consumed each tick while accelerating forward with an Overdrive Broom Engine installed.")
                .defineInRange("overdriveForwardManaCostPerTick",
                        DEFAULT_OVERDRIVE_FORWARD_MANA_COST_PER_TICK, 0.0D, 10000.0D);
        var overdriveInertiaGlideManaCostPerTick = builder
                .comment("Mana consumed each tick while inertia glide is active with an Overdrive Broom Engine installed.")
                .defineInRange("overdriveInertiaGlideManaCostPerTick",
                        DEFAULT_OVERDRIVE_INERTIA_GLIDE_MANA_COST_PER_TICK, 0.0D, 10000.0D);
        var overdriveInertiaReleaseManaCost = builder
                .comment("Mana consumed when releasing inertia glide with an Overdrive Broom Engine installed. This positive value is also the mana threshold required to recover from depleted mode.")
                .defineInRange("overdriveInertiaReleaseManaCost",
                        DEFAULT_OVERDRIVE_INERTIA_RELEASE_MANA_COST,
                        MIN_INERTIA_RELEASE_MANA_COST, 10000.0D);
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
                overdriveForwardManaCostPerTick,
                overdriveInertiaGlideManaCostPerTick,
                overdriveInertiaReleaseManaCost,
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
                        overdriveForwardManaCostPerTick.get(),
                        overdriveInertiaGlideManaCostPerTick.get(),
                        overdriveInertiaReleaseManaCost.get(),
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
            double overdriveForwardManaCostPerTick,
            double overdriveInertiaGlideManaCostPerTick,
            double overdriveInertiaReleaseManaCost,
            double inertiaReleaseMinimumSpeedRatio,
            double lowManaWarningThreshold
    ) {
        public Values {
            if (!Double.isFinite(inertiaReleaseManaCost)
                    || inertiaReleaseManaCost < MIN_INERTIA_RELEASE_MANA_COST) {
                throw new IllegalArgumentException("Inertia release mana cost must be positive");
            }
            if (!Double.isFinite(overdriveInertiaReleaseManaCost)
                    || overdriveInertiaReleaseManaCost < MIN_INERTIA_RELEASE_MANA_COST) {
                throw new IllegalArgumentException("Overdrive inertia release mana cost must be positive");
            }
        }

        public Values(
                double forwardManaCostPerTick,
                double inertiaGlideManaCostPerTick,
                double inertiaReleaseManaCost,
                double inertiaReleaseMinimumSpeedRatio,
                double lowManaWarningThreshold
        ) {
            this(forwardManaCostPerTick, inertiaGlideManaCostPerTick, inertiaReleaseManaCost,
                    DEFAULT_OVERDRIVE_FORWARD_MANA_COST_PER_TICK,
                    DEFAULT_OVERDRIVE_INERTIA_GLIDE_MANA_COST_PER_TICK,
                    DEFAULT_OVERDRIVE_INERTIA_RELEASE_MANA_COST,
                    inertiaReleaseMinimumSpeedRatio, lowManaWarningThreshold);
        }
    }
}
