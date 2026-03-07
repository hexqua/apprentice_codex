package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AbsorptionAmplifyAmuletServerConfig {
    private final ModConfigSpec.DoubleValue baseAbsorptionTarget;
    private final ModConfigSpec.IntValue recoveryDelayTicks;

    private AbsorptionAmplifyAmuletServerConfig(
            ModConfigSpec.DoubleValue baseAbsorptionTarget,
            ModConfigSpec.IntValue recoveryDelayTicks
    ) {
        this.baseAbsorptionTarget = baseAbsorptionTarget;
        this.recoveryDelayTicks = recoveryDelayTicks;
    }

    public static AbsorptionAmplifyAmuletServerConfig define(ModConfigSpec.Builder builder) {
        builder.comment("Ticks before absorption recovery resumes after equipping or taking absorption damage.")
                .push("AbsorptionAmplifyAmulet");

        // Iron's Spells 'n Spellbooks 3.15.0 の Fortify Lv3 基礎値(倍率1.0時=8.0)に合わせた既定値.
        var baseAbsorptionTarget = builder.defineInRange("baseAbsorptionTarget", 8.0d, 0.0d, 1000.0d);
        var recoveryDelayTicks = builder.defineInRange("recoveryDelayTicks", 100, 0, 72000);

        builder.pop();
        return new AbsorptionAmplifyAmuletServerConfig(baseAbsorptionTarget, recoveryDelayTicks);
    }

    public float baseAbsorptionTarget() {
        return baseAbsorptionTarget.get().floatValue();
    }

    public int recoveryDelayTicks() {
        return recoveryDelayTicks.get();
    }
}
