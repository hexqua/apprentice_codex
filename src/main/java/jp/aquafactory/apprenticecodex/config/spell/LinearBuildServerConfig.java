package jp.aquafactory.apprenticecodex.config.spell;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class LinearBuildServerConfig {
    public static final int DEFAULT_MANA_COST_PER_BLOCK = 5;
    public static final int MIN_MANA_COST_PER_BLOCK = 0;
    public static final int MAX_MANA_COST_PER_BLOCK = 100000;

    private final ModConfigSpec.IntValue manaCostPerBlock;
    private final ModConfigSpec.BooleanValue abortOnFailedPlacement;
    private final ModConfigSpec.BooleanValue enableShulkerBoxSources;
    private final ModConfigSpec.BooleanValue enableBundleSources;
    private Integer manaCostPerBlockOverride;
    private Boolean abortOnFailedPlacementOverride;
    private Boolean enableShulkerBoxSourcesOverride;
    private Boolean enableBundleSourcesOverride;

    private LinearBuildServerConfig(
            ModConfigSpec.IntValue manaCostPerBlock,
            ModConfigSpec.BooleanValue abortOnFailedPlacement,
            ModConfigSpec.BooleanValue enableShulkerBoxSources,
            ModConfigSpec.BooleanValue enableBundleSources
    ) {
        this.manaCostPerBlock = manaCostPerBlock;
        this.abortOnFailedPlacement = abortOnFailedPlacement;
        this.enableShulkerBoxSources = enableShulkerBoxSources;
        this.enableBundleSources = enableBundleSources;
    }

    public static LinearBuildServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("LinearBuild");

        var manaCostPerBlock = builder
                .comment("Additional mana consumed for each block successfully placed by Linear Build.")
                .defineInRange(
                        "manaCostPerBlock",
                        DEFAULT_MANA_COST_PER_BLOCK,
                        MIN_MANA_COST_PER_BLOCK,
                        MAX_MANA_COST_PER_BLOCK
                );
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
        return new LinearBuildServerConfig(
                manaCostPerBlock,
                abortOnFailedPlacement,
                enableShulkerBoxSources,
                enableBundleSources
        );
    }

    public int manaCostPerBlock() {
        if (manaCostPerBlockOverride != null) {
            return manaCostPerBlockOverride;
        }
        return manaCostPerBlock.get();
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
        return new Values(manaCostPerBlock(), abortOnFailedPlacement(), enableShulkerBoxSources(), enableBundleSources());
    }

    public void setForGameTest(Values values) {
        this.manaCostPerBlockOverride = values.manaCostPerBlock();
        this.abortOnFailedPlacementOverride = values.abortOnFailedPlacement();
        this.enableShulkerBoxSourcesOverride = values.enableShulkerBoxSources();
        this.enableBundleSourcesOverride = values.enableBundleSources();
    }

    public record Values(
            int manaCostPerBlock,
            boolean abortOnFailedPlacement,
            boolean enableShulkerBoxSources,
            boolean enableBundleSources
    ) {
    }
}
