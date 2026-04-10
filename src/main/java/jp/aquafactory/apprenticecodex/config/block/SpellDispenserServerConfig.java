package jp.aquafactory.apprenticecodex.config.block;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SpellDispenserServerConfig {
    private final ForgeConfigSpec.BooleanValue relaxedSpellFilter;

    private SpellDispenserServerConfig(ForgeConfigSpec.BooleanValue relaxedSpellFilter) {
        this.relaxedSpellFilter = relaxedSpellFilter;
    }

    public static SpellDispenserServerConfig define(ForgeConfigSpec.Builder builder) {
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
