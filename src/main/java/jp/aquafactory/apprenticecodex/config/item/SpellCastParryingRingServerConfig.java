package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SpellCastParryingRingServerConfig {
    private final ForgeConfigSpec.IntValue parryWindowTicks;
    private Integer parryWindowTicksOverride;

    private SpellCastParryingRingServerConfig(ForgeConfigSpec.IntValue parryWindowTicks) {
        this.parryWindowTicks = parryWindowTicks;
    }

    public static SpellCastParryingRingServerConfig define(ForgeConfigSpec.Builder builder) {
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
