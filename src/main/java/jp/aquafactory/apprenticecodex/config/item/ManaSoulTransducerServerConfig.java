package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public record ManaSoulTransducerServerConfig(
        ForgeConfigSpec.DoubleValue castTimeReductionTransferRate,
        ForgeConfigSpec.IntValue manaCost) {
    public static ManaSoulTransducerServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("ManaSoulTransducer");
        var cast = builder.comment("Fraction of effective Iron's LONG cast time reduction applied to Malum staff charge duration. Values below the default attribute value do not apply penalties.")
                .defineInRange("castTimeReductionTransferRate", 0.8D, 0D, 1D);
        var cost = builder.comment("Mana spent per successful staff shot or existing staff cooldown removal.")
                .defineInRange("manaCost", 80, 0, 1000000);
        builder.pop();
        return new ManaSoulTransducerServerConfig(cast, cost);
    }
}
