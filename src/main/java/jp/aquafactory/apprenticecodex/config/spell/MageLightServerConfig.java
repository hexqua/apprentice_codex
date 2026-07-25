package jp.aquafactory.apprenticecodex.config.spell;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MageLightServerConfig {
    public static final double DEFAULT_MAX_RANGE = 32.0D;
    public static final double MIN_MAX_RANGE = 1.0D;
    public static final double MAX_MAX_RANGE = 64.0D;

    private final ModConfigSpec.DoubleValue maxRange;
    private Double maxRangeOverride;

    private MageLightServerConfig(ModConfigSpec.DoubleValue maxRange) {
        this.maxRange = maxRange;
    }

    public static MageLightServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("MageLight");
        var maxRange = builder
                .comment("Maximum placement range of Mage Light.")
                .defineInRange("maxRange", DEFAULT_MAX_RANGE, MIN_MAX_RANGE, MAX_MAX_RANGE);
        builder.pop();
        return new MageLightServerConfig(maxRange);
    }

    public double maxRange() {
        return maxRangeOverride == null ? maxRange.get() : maxRangeOverride;
    }

    public void setForGameTest(double maxRange) {
        maxRangeOverride = maxRange;
    }
}
