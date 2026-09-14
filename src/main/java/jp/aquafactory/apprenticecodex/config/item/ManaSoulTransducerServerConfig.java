package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public record ManaSoulTransducerServerConfig(
        ModConfigSpec.DoubleValue castTimeReductionTransferRate,
        ModConfigSpec.DoubleValue cooldownReductionTransferRate) {
    public static ManaSoulTransducerServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("ManaSoulTransducer");
        var cast = builder.comment("Fraction of effective Iron's LONG cast time reduction applied to Malum staff charge duration. Values below the default attribute value do not apply penalties.")
                .defineInRange("castTimeReductionTransferRate", 0.8D, 0D, 1D);
        var recovery = builder.comment("Fraction of positive Iron's cooldown reduction attribute bonus added to Malum staff charge recovery rate.")
                .defineInRange("cooldownReductionTransferRate", 0.8D, 0D, 1D);
        builder.pop();
        return new ManaSoulTransducerServerConfig(cast, recovery);
    }
}
