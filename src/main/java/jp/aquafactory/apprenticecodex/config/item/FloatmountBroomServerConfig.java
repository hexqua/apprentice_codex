package jp.aquafactory.apprenticecodex.config.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class FloatmountBroomServerConfig {
    public static final int DEFAULT_NORMAL_FLIGHT_MANA_THRESHOLD = 100;
    public static final double DEFAULT_OVERDRIVE_HORIZONTAL_MANA_COST_PER_TICK = 1.75D;
    public static final double DEFAULT_OVERDRIVE_ASCENDING_MANA_COST_PER_TICK = 1.0D;
    public static final double DEFAULT_OVERDRIVE_HORIZONTAL_ASCENDING_MANA_COST_PER_TICK = 2.5D;

    private final ForgeConfigSpec.IntValue maxDamage;
    private final ForgeConfigSpec.IntValue damageRecoveryAmount;
    private final ForgeConfigSpec.IntValue damageIFrameTicks;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> iframeIgnoredDamageTypes;
    private final ForgeConfigSpec.IntValue normalFlightManaThreshold;
    private final ForgeConfigSpec.IntValue lowManaWarningThreshold;
    private final ForgeConfigSpec.DoubleValue horizontalManaCostPerTick;
    private final ForgeConfigSpec.DoubleValue ascendingManaCostPerTick;
    private final ForgeConfigSpec.DoubleValue horizontalAscendingManaCostPerTick;
    private final ForgeConfigSpec.DoubleValue overdriveHorizontalManaCostPerTick;
    private final ForgeConfigSpec.DoubleValue overdriveAscendingManaCostPerTick;
    private final ForgeConfigSpec.DoubleValue overdriveHorizontalAscendingManaCostPerTick;
    private Values override;

    private FloatmountBroomServerConfig(
            ForgeConfigSpec.IntValue maxDamage,
            ForgeConfigSpec.IntValue damageRecoveryAmount,
            ForgeConfigSpec.IntValue damageIFrameTicks,
            ForgeConfigSpec.ConfigValue<List<? extends String>> iframeIgnoredDamageTypes,
            ForgeConfigSpec.IntValue normalFlightManaThreshold,
            ForgeConfigSpec.IntValue lowManaWarningThreshold,
            ForgeConfigSpec.DoubleValue horizontalManaCostPerTick,
            ForgeConfigSpec.DoubleValue ascendingManaCostPerTick,
            ForgeConfigSpec.DoubleValue horizontalAscendingManaCostPerTick,
            ForgeConfigSpec.DoubleValue overdriveHorizontalManaCostPerTick,
            ForgeConfigSpec.DoubleValue overdriveAscendingManaCostPerTick,
            ForgeConfigSpec.DoubleValue overdriveHorizontalAscendingManaCostPerTick
    ) {
        this.maxDamage = maxDamage;
        this.damageRecoveryAmount = damageRecoveryAmount;
        this.damageIFrameTicks = damageIFrameTicks;
        this.iframeIgnoredDamageTypes = iframeIgnoredDamageTypes;
        this.normalFlightManaThreshold = normalFlightManaThreshold;
        this.lowManaWarningThreshold = lowManaWarningThreshold;
        this.horizontalManaCostPerTick = horizontalManaCostPerTick;
        this.ascendingManaCostPerTick = ascendingManaCostPerTick;
        this.horizontalAscendingManaCostPerTick = horizontalAscendingManaCostPerTick;
        this.overdriveHorizontalManaCostPerTick = overdriveHorizontalManaCostPerTick;
        this.overdriveAscendingManaCostPerTick = overdriveAscendingManaCostPerTick;
        this.overdriveHorizontalAscendingManaCostPerTick = overdriveHorizontalAscendingManaCostPerTick;
    }

    public static FloatmountBroomServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("FloatmountBroom");

        var maxDamage = builder
                .comment("Maximum accumulated damage before the broom becomes damaged. With the default maximum of 1000, 50 accumulated damage corresponds to one point of incoming damage (half a heart) and half of one durability HUD icon.")
                .defineInRange("maxDamage", 1000, 1, 1000000);
        var damageRecoveryAmount = builder
                .comment("Damage recovered every 10 ticks while the broom is not damaged. Set to 0 to disable natural recovery.")
                .defineInRange("damageRecoveryAmount", 50, 0, 1000000);
        var damageIFrameTicks = builder
                .comment("Ticks before the broom can accept another normal damage hit. Set to 0 to disable the broom-specific damage i-frame.")
                .defineInRange("damageIFrameTicks", 10, 0, 20);
        var iframeIgnoredDamageTypes = builder
                .comment("Damage type IDs that do not interact with the broom-specific damage i-frame. These hits pass through an active i-frame without starting, extending, or clearing it.")
                .defineListAllowEmpty("iframeIgnoredDamageTypes", List.<String>of(),
                        FloatmountBroomServerConfig::isDamageTypeId);
        var normalFlightManaThreshold = builder
                .comment("Mana required to mount the broom and to restore normal flight after a warning or emergency landing.")
                .defineInRange("normalFlightManaThreshold", DEFAULT_NORMAL_FLIGHT_MANA_THRESHOLD, 1, 10000);
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
        var overdriveHorizontalManaCostPerTick = builder
                .comment("Mana consumed each tick while moving forward or backward with an Overdrive Broom Engine installed.")
                .defineInRange("overdriveHorizontalManaCostPerTick",
                        DEFAULT_OVERDRIVE_HORIZONTAL_MANA_COST_PER_TICK, 0.0D, 10000.0D);
        var overdriveAscendingManaCostPerTick = builder
                .comment("Mana consumed each tick while ascending with an Overdrive Broom Engine installed.")
                .defineInRange("overdriveAscendingManaCostPerTick",
                        DEFAULT_OVERDRIVE_ASCENDING_MANA_COST_PER_TICK, 0.0D, 10000.0D);
        var overdriveHorizontalAscendingManaCostPerTick = builder
                .comment("Mana consumed each tick while moving horizontally and ascending with an Overdrive Broom Engine installed.")
                .defineInRange("overdriveHorizontalAscendingManaCostPerTick",
                        DEFAULT_OVERDRIVE_HORIZONTAL_ASCENDING_MANA_COST_PER_TICK, 0.0D, 10000.0D);

        builder.pop();
        return new FloatmountBroomServerConfig(
                maxDamage,
                damageRecoveryAmount,
                damageIFrameTicks,
                iframeIgnoredDamageTypes,
                normalFlightManaThreshold,
                lowManaWarningThreshold,
                horizontalManaCostPerTick,
                ascendingManaCostPerTick,
                horizontalAscendingManaCostPerTick,
                overdriveHorizontalManaCostPerTick,
                overdriveAscendingManaCostPerTick,
                overdriveHorizontalAscendingManaCostPerTick
        );
    }

    public Values values() {
        return override == null
                ? new Values(
                        maxDamage.get(),
                        damageRecoveryAmount.get(),
                        damageIFrameTicks.get(),
                        iframeIgnoredDamageTypes.get().stream()
                                .map(String::valueOf)
                                .map(String::trim)
                                .map(ResourceLocation::tryParse)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toUnmodifiableSet()),
                        normalFlightManaThreshold.get(),
                        lowManaWarningThreshold.get(),
                        horizontalManaCostPerTick.get(),
                        ascendingManaCostPerTick.get(),
                        horizontalAscendingManaCostPerTick.get(),
                        overdriveHorizontalManaCostPerTick.get(),
                        overdriveAscendingManaCostPerTick.get(),
                        overdriveHorizontalAscendingManaCostPerTick.get()
                )
                : override;
    }

    public void setForGameTest(Values values) {
        override = values;
    }

    private static boolean isDamageTypeId(Object value) {
        return value instanceof String text
                && text.contains(":")
                && ResourceLocation.tryParse(text.trim()) != null;
    }

    public record Values(
            int maxDamage,
            int damageRecoveryAmount,
            int damageIFrameTicks,
            Set<ResourceLocation> iframeIgnoredDamageTypes,
            int normalFlightManaThreshold,
            int lowManaWarningThreshold,
            double horizontalManaCostPerTick,
            double ascendingManaCostPerTick,
            double horizontalAscendingManaCostPerTick,
            double overdriveHorizontalManaCostPerTick,
            double overdriveAscendingManaCostPerTick,
            double overdriveHorizontalAscendingManaCostPerTick
    ) {
        public Values(
                int maxDamage, int damageRecoveryAmount, int damageIFrameTicks,
                Set<ResourceLocation> iframeIgnoredDamageTypes,
                int normalFlightManaThreshold, int lowManaWarningThreshold,
                double horizontalManaCostPerTick, double ascendingManaCostPerTick,
                double horizontalAscendingManaCostPerTick
        ) {
            this(maxDamage, damageRecoveryAmount, damageIFrameTicks, iframeIgnoredDamageTypes,
                    normalFlightManaThreshold, lowManaWarningThreshold,
                    horizontalManaCostPerTick, ascendingManaCostPerTick, horizontalAscendingManaCostPerTick,
                    DEFAULT_OVERDRIVE_HORIZONTAL_MANA_COST_PER_TICK,
                    DEFAULT_OVERDRIVE_ASCENDING_MANA_COST_PER_TICK,
                    DEFAULT_OVERDRIVE_HORIZONTAL_ASCENDING_MANA_COST_PER_TICK);
        }
    }
}
