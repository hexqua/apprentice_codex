package jp.aquafactory.apprenticecodex.config.spell;

import net.minecraftforge.common.ForgeConfigSpec;

public final class LinearBuildServerConfig {
    private final ForgeConfigSpec.BooleanValue abortOnFailedPlacement;
    private final ForgeConfigSpec.BooleanValue enableShulkerBoxSources;
    private final ForgeConfigSpec.BooleanValue enableBundleSources;
    private Boolean abortOnFailedPlacementOverride;
    private Boolean enableShulkerBoxSourcesOverride;
    private Boolean enableBundleSourcesOverride;

    private LinearBuildServerConfig(
            ForgeConfigSpec.BooleanValue abortOnFailedPlacement,
            ForgeConfigSpec.BooleanValue enableShulkerBoxSources,
            ForgeConfigSpec.BooleanValue enableBundleSources
    ) {
        this.abortOnFailedPlacement = abortOnFailedPlacement;
        this.enableShulkerBoxSources = enableShulkerBoxSources;
        this.enableBundleSources = enableBundleSources;
    }

    public static LinearBuildServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("LinearBuild");

        var abortOnFailedPlacement = builder
                .comment("When true, Linear Build stops at the first position where placement fails. When false, it skips failed positions and continues.")
                .define("abortOnFailedPlacement", false);
        var enableShulkerBoxSources = builder
                .comment("Allows Linear Build to retrieve matching blocks from Shulker Boxes carried by the caster.")
                .define("enableShulkerBoxSources", true);
        var enableBundleSources = builder
                .comment("Allows Linear Build to retrieve matching blocks from Bundles carried by the caster.")
                .define("enableBundleSources", true);

        builder.pop();
        return new LinearBuildServerConfig(abortOnFailedPlacement, enableShulkerBoxSources, enableBundleSources);
    }

    public boolean abortOnFailedPlacement() {
        if (abortOnFailedPlacementOverride != null) {
            return abortOnFailedPlacementOverride;
        }
        return abortOnFailedPlacement.get();
    }

    public boolean enableShulkerBoxSources() {
        if (enableShulkerBoxSourcesOverride != null) {
            return enableShulkerBoxSourcesOverride;
        }
        return enableShulkerBoxSources.get();
    }

    public boolean enableBundleSources() {
        if (enableBundleSourcesOverride != null) {
            return enableBundleSourcesOverride;
        }
        return enableBundleSources.get();
    }

    public Values values() {
        return new Values(abortOnFailedPlacement(), enableShulkerBoxSources(), enableBundleSources());
    }

    public void setForGameTest(Values values) {
        this.abortOnFailedPlacementOverride = values.abortOnFailedPlacement();
        this.enableShulkerBoxSourcesOverride = values.enableShulkerBoxSources();
        this.enableBundleSourcesOverride = values.enableBundleSources();
    }

    public record Values(boolean abortOnFailedPlacement, boolean enableShulkerBoxSources, boolean enableBundleSources) {
    }
}
