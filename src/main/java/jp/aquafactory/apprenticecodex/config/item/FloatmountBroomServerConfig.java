package jp.aquafactory.apprenticecodex.config.item;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class FloatmountBroomServerConfig {
    public static final int DEFAULT_NORMAL_FLIGHT_MANA_THRESHOLD = 100;

    private final ModConfigSpec.IntValue maxDamage;
    private final ModConfigSpec.IntValue damageRecoveryAmount;
    private final ModConfigSpec.IntValue damageIFrameTicks;
    private final ModConfigSpec.ConfigValue<List<? extends String>> iframeIgnoredDamageTypes;
    private final ModConfigSpec.IntValue normalFlightManaThreshold;
    private final ModConfigSpec.IntValue lowManaWarningThreshold;
    private final ModConfigSpec.DoubleValue horizontalManaCostPerTick;
    private final ModConfigSpec.DoubleValue ascendingManaCostPerTick;
    private final ModConfigSpec.DoubleValue horizontalAscendingManaCostPerTick;
    private Values override;

    private FloatmountBroomServerConfig(
            ModConfigSpec.IntValue maxDamage,
            ModConfigSpec.IntValue damageRecoveryAmount,
            ModConfigSpec.IntValue damageIFrameTicks,
            ModConfigSpec.ConfigValue<List<? extends String>> iframeIgnoredDamageTypes,
            ModConfigSpec.IntValue normalFlightManaThreshold,
            ModConfigSpec.IntValue lowManaWarningThreshold,
            ModConfigSpec.DoubleValue horizontalManaCostPerTick,
            ModConfigSpec.DoubleValue ascendingManaCostPerTick,
            ModConfigSpec.DoubleValue horizontalAscendingManaCostPerTick
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
    }

    public static FloatmountBroomServerConfig define(ModConfigSpec.Builder builder) {
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
                horizontalAscendingManaCostPerTick
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
                        horizontalAscendingManaCostPerTick.get()
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
            double horizontalAscendingManaCostPerTick
    ) {
    }
}
