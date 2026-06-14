package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SpellCastParryingRingServerConfig {
    private final ModConfigSpec.IntValue parryWindowTicks;
    private Integer parryWindowTicksOverride;

    private SpellCastParryingRingServerConfig(ModConfigSpec.IntValue parryWindowTicks) {
        this.parryWindowTicks = parryWindowTicks;
    }

    public static SpellCastParryingRingServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("SpellCastParryingRing");
        var parryWindowTicks = builder
                .comment("Ticks after spell-cast start where Spell Cast Parrying Ring can block frontal damage.")
                .defineInRange("spellCastParryingRingParryWindowTicks", 10, 0, Integer.MAX_VALUE);
        builder.pop();

        return new SpellCastParryingRingServerConfig(parryWindowTicks);
    }

    public int parryWindowTicks() {
        return parryWindowTicksOverride == null ? parryWindowTicks.get() : parryWindowTicksOverride;
    }

    public void setParryWindowTicksForGameTest(int value) {
        parryWindowTicksOverride = Math.max(0, value);
    }
}
