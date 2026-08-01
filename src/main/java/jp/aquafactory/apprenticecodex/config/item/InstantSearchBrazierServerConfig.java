package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class InstantSearchBrazierServerConfig {
    public static final int DEFAULT_INITIAL_RANGE = 500;
    public static final int MAX_INITIAL_RANGE = 5000;

    private final ModConfigSpec.IntValue initialRange;
    private Integer initialRangeOverride;

    private InstantSearchBrazierServerConfig(ModConfigSpec.IntValue initialRange) {
        this.initialRange = initialRange;
    }

    public static InstantSearchBrazierServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("InstantSearchBrazier");
        var initialRange = builder
                .comment("Initial search range of an Instant Brazier of Search.")
                .defineInRange("initialRange", DEFAULT_INITIAL_RANGE, 0, MAX_INITIAL_RANGE);
        builder.pop();
        return new InstantSearchBrazierServerConfig(initialRange);
    }

    public int initialRange() {
        return initialRangeOverride == null ? initialRange.get() : initialRangeOverride;
    }

    public void setForGameTest(int initialRange) {
        initialRangeOverride = initialRange;
    }
}
