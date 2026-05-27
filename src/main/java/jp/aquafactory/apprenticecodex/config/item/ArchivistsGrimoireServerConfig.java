package jp.aquafactory.apprenticecodex.config.item;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ArchivistsGrimoireServerConfig {
    private static final int MIN_ROWS = 1;
    private static final int MAX_ROWS = 6;

    private final ForgeConfigSpec.IntValue initialRows;
    private final ForgeConfigSpec.IntValue maxRows;

    private Values override;
    private boolean warnedInvalidRowBounds;

    private ArchivistsGrimoireServerConfig(ForgeConfigSpec.IntValue initialRows, ForgeConfigSpec.IntValue maxRows) {
        this.initialRows = initialRows;
        this.maxRows = maxRows;
    }

    public static ArchivistsGrimoireServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("ArchivistsGrimoire");
        var initialRows = builder
                .comment("Initial unlocked row count for Archivist's Grimoire. Each row contains 9 scroll slots.")
                .defineInRange("initialRows", 1, MIN_ROWS, MAX_ROWS);
        var maxRows = builder
                .comment("Maximum unlocked row count for Archivist's Grimoire. Values lower than initialRows are treated as initialRows.")
                .defineInRange("maxRows", 6, MIN_ROWS, MAX_ROWS);
        builder.pop();

        return new ArchivistsGrimoireServerConfig(initialRows, maxRows);
    }

    public int initialRows() {
        return values().initialRows();
    }

    public int effectiveMaxRows() {
        var values = values();
        if (values.maxRows() >= values.initialRows()) {
            return values.maxRows();
        }

        if (!warnedInvalidRowBounds) {
            ApprenticeCodex.LOGGER.warn(
                    "ArchivistsGrimoire maxRows ({}) is lower than initialRows ({}). Treating maxRows as initialRows.",
                    values.maxRows(),
                    values.initialRows()
            );
            warnedInvalidRowBounds = true;
        }
        return values.initialRows();
    }

    public Values values() {
        if (override != null) {
            return override;
        }
        return new Values(initialRows.get(), maxRows.get());
    }

    public void setForGameTest(Values values) {
        this.override = values;
        warnedInvalidRowBounds = false;
    }

    public record Values(int initialRows, int maxRows) {
        public Values {
            initialRows = clampRows(initialRows);
            maxRows = clampRows(maxRows);
        }
    }

    private static int clampRows(int rows) {
        return Math.max(MIN_ROWS, Math.min(MAX_ROWS, rows));
    }
}
