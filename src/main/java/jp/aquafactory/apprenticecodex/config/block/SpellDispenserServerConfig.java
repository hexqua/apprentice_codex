package jp.aquafactory.apprenticecodex.config.block;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SpellDispenserServerConfig {
    private final ModConfigSpec.BooleanValue relaxedSpellFilter;

    private SpellDispenserServerConfig(ModConfigSpec.BooleanValue relaxedSpellFilter) {
        this.relaxedSpellFilter = relaxedSpellFilter;
    }

    public static SpellDispenserServerConfig define(ModConfigSpec.Builder builder) {
        builder.comment("true にすると Spell Dispenser の spell profile / denylist を無視し、基本条件だけで検証できるようにする")
                .push("SpellDispenser");

        var relaxedSpellFilter = builder.define("relaxedSpellFilter", false);

        builder.pop();
        return new SpellDispenserServerConfig(relaxedSpellFilter);
    }

    public boolean relaxedSpellFilter() {
        return relaxedSpellFilter.get();
    }
}
