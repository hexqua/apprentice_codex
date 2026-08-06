package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FloatmountBroomServerConfig {
    private final ModConfigSpec.IntValue normalFlightManaThreshold;
    private final ModConfigSpec.IntValue lowManaWarningThreshold;
    private final ModConfigSpec.DoubleValue horizontalManaCostPerTick;
    private final ModConfigSpec.DoubleValue ascendingManaCostPerTick;
    private final ModConfigSpec.DoubleValue horizontalAscendingManaCostPerTick;
    private Values override;

    private FloatmountBroomServerConfig(
            ModConfigSpec.IntValue normalFlightManaThreshold,
            ModConfigSpec.IntValue lowManaWarningThreshold,
            ModConfigSpec.DoubleValue horizontalManaCostPerTick,
            ModConfigSpec.DoubleValue ascendingManaCostPerTick,
            ModConfigSpec.DoubleValue horizontalAscendingManaCostPerTick
    ) {
        this.normalFlightManaThreshold = normalFlightManaThreshold;
        this.lowManaWarningThreshold = lowManaWarningThreshold;
        this.horizontalManaCostPerTick = horizontalManaCostPerTick;
        this.ascendingManaCostPerTick = ascendingManaCostPerTick;
        this.horizontalAscendingManaCostPerTick = horizontalAscendingManaCostPerTick;
    }

    public static FloatmountBroomServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("FloatmountBroom");

        var normalFlightManaThreshold = builder
                .comment("Mana required to mount the broom and to restore normal flight after a warning or emergency landing.")
                .defineInRange("normalFlightManaThreshold", 100, 1, 10000);
        var lowManaWarningThreshold = builder
                .comment("Mana at or below which the broom shows its low mana warning. Values at or above the normal flight threshold are treated as below that threshold.")
                .defineInRange("lowManaWarningThreshold", 50, 0, 10000);
        var horizontalManaCostPerTick = builder
                .comment("Mana consumed each tick while moving the broom forward or backward without ascending.")
                .defineInRange("horizontalManaCostPerTick", 1.0D, 0.0D, 10000.0D);
        var ascendingManaCostPerTick = builder
                .comment("Mana consumed each tick while ascending without moving forward or backward.")
                .defineInRange("ascendingManaCostPerTick", 1.0D, 0.0D, 10000.0D);
        var horizontalAscendingManaCostPerTick = builder
                .comment("Mana consumed each tick while moving forward or backward and ascending at the same time.")
                .defineInRange("horizontalAscendingManaCostPerTick", 1.5D, 0.0D, 10000.0D);

        builder.pop();
        return new FloatmountBroomServerConfig(
                normalFlightManaThreshold,
                lowManaWarningThreshold,
                horizontalManaCostPerTick,
                ascendingManaCostPerTick,
                horizontalAscendingManaCostPerTick
        );
    }

    public Values values() {
        return override == null
                ? new Values(
                        normalFlightManaThreshold.get(),
                        lowManaWarningThreshold.get(),
                        horizontalManaCostPerTick.get(),
                        ascendingManaCostPerTick.get(),
                        horizontalAscendingManaCostPerTick.get()
                )
                : override;
    }

    public void setForGameTest(Values values) {
        override = values;
    }

    public record Values(
            int normalFlightManaThreshold,
            int lowManaWarningThreshold,
            double horizontalManaCostPerTick,
            double ascendingManaCostPerTick,
            double horizontalAscendingManaCostPerTick
    ) {
    }
}
