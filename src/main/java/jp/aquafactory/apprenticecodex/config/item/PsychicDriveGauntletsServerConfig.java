package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public record PsychicDriveGauntletsServerConfig(ForgeConfigSpec.IntValue attackManaCost,
                                               ForgeConfigSpec.IntValue chargeManaCostPerTick) {
    public static PsychicDriveGauntletsServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("PsychicDriveGauntlets");
        var attack = builder.comment("Mana consumed immediately when a normal gauntlet attack starts.")
                .defineInRange("attackManaCost", 20, 0, 10000);
        var charge = builder.comment("Mana consumed per charging tick, for up to 20 ticks. Starting requires the full amount.")
                .defineInRange("chargeManaCostPerTick", 3, 0, 10000);
        builder.pop();
        return new PsychicDriveGauntletsServerConfig(attack, charge);
    }
}
