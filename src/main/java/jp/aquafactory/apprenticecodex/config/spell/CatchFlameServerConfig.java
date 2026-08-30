package jp.aquafactory.apprenticecodex.config.spell;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CatchFlameServerConfig {
    private static final boolean DEFAULT_ALLOW_SPELL_DISPENSER_IGNITION = true;
    private static final boolean DEFAULT_ALLOW_NON_PLAYER_IGNITION = false;

    private final ForgeConfigSpec.BooleanValue allowSpellDispenserIgnition;
    private final ForgeConfigSpec.BooleanValue allowNonPlayerIgnition;
    private Values override;

    private CatchFlameServerConfig(
            ForgeConfigSpec.BooleanValue allowSpellDispenserIgnition,
            ForgeConfigSpec.BooleanValue allowNonPlayerIgnition
    ) {
        this.allowSpellDispenserIgnition = allowSpellDispenserIgnition;
        this.allowNonPlayerIgnition = allowNonPlayerIgnition;
    }

    public static CatchFlameServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("CatchFlame");
        var allowSpellDispenserIgnition = builder
                .comment("Whether Catch Flame cast by a Spell Dispenser may ignite blocks.")
                .define("allowSpellDispenserIgnition", DEFAULT_ALLOW_SPELL_DISPENSER_IGNITION);
        var allowNonPlayerIgnition = builder
                .comment("Whether Catch Flame cast by a non-player living entity may ignite blocks.")
                .define("allowNonPlayerIgnition", DEFAULT_ALLOW_NON_PLAYER_IGNITION);
        builder.pop();
        return new CatchFlameServerConfig(allowSpellDispenserIgnition, allowNonPlayerIgnition);
    }

    public Values values() {
        return override != null
                ? override
                : new Values(allowSpellDispenserIgnition.get(), allowNonPlayerIgnition.get());
    }

    public void setForGameTest(Values values) {
        override = values;
    }

    public record Values(boolean allowSpellDispenserIgnition, boolean allowNonPlayerIgnition) {
    }
}
