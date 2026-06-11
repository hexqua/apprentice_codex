package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ManaThrusterServerConfig {
    private final ForgeConfigSpec.DoubleValue manaCostPerTick;
    private Double manaCostPerTickOverride;

    private ManaThrusterServerConfig(ForgeConfigSpec.DoubleValue manaCostPerTick) {
        this.manaCostPerTick = manaCostPerTick;
    }

    public static ManaThrusterServerConfig define(ForgeConfigSpec.Builder builder) {
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
