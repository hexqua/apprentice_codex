package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class UndyingEmblemServerConfig {
    private final ModConfigSpec.IntValue reconstructionSpeedMultiplier;
    private Integer reconstructionSpeedMultiplierOverride;

    private UndyingEmblemServerConfig(ModConfigSpec.IntValue reconstructionSpeedMultiplier) {
        this.reconstructionSpeedMultiplier = reconstructionSpeedMultiplier;
    }

    public static UndyingEmblemServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("UndyingEmblem");
        var reconstructionSpeedMultiplier = builder
                .comment("Cooldown speed multiplier while Idol Reconstruction is being channeled. 1 provides no acceleration.")
                .defineInRange("reconstructionSpeedMultiplier", 10, 1, 100);
        builder.pop();
        return new UndyingEmblemServerConfig(reconstructionSpeedMultiplier);
    }

    public int reconstructionSpeedMultiplier() {
        return reconstructionSpeedMultiplierOverride == null
                ? reconstructionSpeedMultiplier.get()
                : reconstructionSpeedMultiplierOverride;
    }

    public void setForGameTest(int reconstructionSpeedMultiplier) {
        reconstructionSpeedMultiplierOverride = reconstructionSpeedMultiplier;
    }
}
