package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public record MagiEngineersLegExoskeletonServerConfig(ModConfigSpec.IntValue lifterManaCost,
                                                    ModConfigSpec.IntValue striderManaCost) {
    public static MagiEngineersLegExoskeletonServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("MagiEngineersLegExoskeleton");
        var lifter = builder.comment("Iron's mana spent on every lifter activation, including the first. Zero makes it free.")
                .defineInRange("lifterManaCost", 15, 0, Integer.MAX_VALUE);
        var strider = builder.comment("Iron's mana spent on every strider activation, on the ground or in midair. Zero makes it free.")
                .defineInRange("striderManaCost", 20, 0, Integer.MAX_VALUE);
        builder.pop();
        return new MagiEngineersLegExoskeletonServerConfig(lifter, strider);
    }
}
