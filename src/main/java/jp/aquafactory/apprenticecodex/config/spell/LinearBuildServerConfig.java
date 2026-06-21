package jp.aquafactory.apprenticecodex.config.spell;

import net.minecraftforge.common.ForgeConfigSpec;

public final class LinearBuildServerConfig {
    private final ForgeConfigSpec.BooleanValue abortOnFailedPlacement;
    private Boolean abortOnFailedPlacementOverride;

    private LinearBuildServerConfig(ForgeConfigSpec.BooleanValue abortOnFailedPlacement) {
        this.abortOnFailedPlacement = abortOnFailedPlacement;
    }

    public static LinearBuildServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("LinearBuild");

        var abortOnFailedPlacement = builder
                .comment("When true, Linear Build stops at the first position where placement fails. When false, it skips failed positions and continues.")
                .define("abortOnFailedPlacement", false);

        builder.pop();
        return new LinearBuildServerConfig(abortOnFailedPlacement);
    }

    public boolean abortOnFailedPlacement() {
        if (abortOnFailedPlacementOverride != null) {
            return abortOnFailedPlacementOverride;
        }
        return abortOnFailedPlacement.get();
    }

    public Values values() {
        return new Values(abortOnFailedPlacement());
    }

    public void setForGameTest(Values values) {
        this.abortOnFailedPlacementOverride = values.abortOnFailedPlacement();
    }

    public record Values(boolean abortOnFailedPlacement) {
    }
}
