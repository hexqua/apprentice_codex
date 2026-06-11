package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ManaThrusterServerConfig {
    private final ModConfigSpec.DoubleValue manaCostPerTick;
    private Double manaCostPerTickOverride;

    private ManaThrusterServerConfig(ModConfigSpec.DoubleValue manaCostPerTick) {
        this.manaCostPerTick = manaCostPerTick;
    }

    public static ManaThrusterServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("ManaThruster");

        var manaCostPerTick = builder
                .comment("Mana consumed each successful Mana Thruster tick.")
                .defineInRange("manaCostPerTick", 7.5D, 0.0D, 10000.0D);

        builder.pop();
        return new ManaThrusterServerConfig(manaCostPerTick);
    }

    public float manaCostPerTick() {
        return (manaCostPerTickOverride == null ? manaCostPerTick.get() : manaCostPerTickOverride).floatValue();
    }

    public void setForGameTest(double manaCostPerTick) {
        this.manaCostPerTickOverride = manaCostPerTick;
    }
}
