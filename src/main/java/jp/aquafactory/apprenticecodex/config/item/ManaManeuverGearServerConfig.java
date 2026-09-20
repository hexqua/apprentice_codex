package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ManaManeuverGearServerConfig {
    private final ForgeConfigSpec.IntValue manaCostPerJump;
    private final ForgeConfigSpec.DoubleValue manaPerDamage;
    private Integer manaCostPerJumpOverride;
    private Double manaPerDamageOverride;

    private ManaManeuverGearServerConfig(
            ForgeConfigSpec.IntValue manaCostPerJump,
            ForgeConfigSpec.DoubleValue manaPerDamage
    ) {
        this.manaCostPerJump = manaCostPerJump;
        this.manaPerDamage = manaPerDamage;
    }

    public static ManaManeuverGearServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("ManaManeuverGear");

        var manaCostPerJump = builder
                .comment("Mana consumed by each successful Mana Maneuver Gear wall jump. 0 makes wall jumps free.")
                .defineInRange("manaCostPerJump", 10, 0, 10000);
        var manaPerDamage = builder
                .comment("Mana spent per fall damage point absorbed by Mana Maneuver Gear. 0 makes fall damage absorption free.")
                .defineInRange("manaPerDamage", 5.0D, 0.0D, 10000.0D);

        builder.pop();
        return new ManaManeuverGearServerConfig(manaCostPerJump, manaPerDamage);
    }

    public int manaCostPerJump() {
        return manaCostPerJumpOverride == null ? manaCostPerJump.get() : manaCostPerJumpOverride;
    }

    public float manaPerDamage() {
        return (manaPerDamageOverride == null ? manaPerDamage.get() : manaPerDamageOverride).floatValue();
    }

    public void setForGameTest(int manaCostPerJump, double manaPerDamage) {
        this.manaCostPerJumpOverride = manaCostPerJump;
        this.manaPerDamageOverride = manaPerDamage;
    }
}
