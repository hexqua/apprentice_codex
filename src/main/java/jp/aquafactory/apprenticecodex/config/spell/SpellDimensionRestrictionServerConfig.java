package jp.aquafactory.apprenticecodex.config.spell;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Objects;

public final class SpellDimensionRestrictionServerConfig {
    private final ModConfigSpec.ConfigValue<List<? extends String>> dimensionDenylist;
    private final ModConfigSpec.BooleanValue enableDimensionAllowlist;
    private final ModConfigSpec.ConfigValue<List<? extends String>> dimensionAllowlist;
    private List<String> dimensionDenylistOverride;
    private Boolean enableDimensionAllowlistOverride;
    private List<String> dimensionAllowlistOverride;

    private SpellDimensionRestrictionServerConfig(
            ModConfigSpec.ConfigValue<List<? extends String>> dimensionDenylist,
            ModConfigSpec.BooleanValue enableDimensionAllowlist,
            ModConfigSpec.ConfigValue<List<? extends String>> dimensionAllowlist
    ) {
        this.dimensionDenylist = dimensionDenylist;
        this.enableDimensionAllowlist = enableDimensionAllowlist;
        this.dimensionAllowlist = dimensionAllowlist;
    }

    public static SpellDimensionRestrictionServerConfig define(
            ModConfigSpec.Builder builder,
            String spellName,
            String actionDescription
    ) {
        var dimensionDenylist = builder
                .comment("Dimension IDs where " + spellName + " cannot " + actionDescription + ". Entries use \"modid:path\".")
                .defineListAllowEmpty("dimensionDenylist", List.<String>of(), SpellDimensionRestrictionServerConfig::isDimensionId);
        var enableDimensionAllowlist = builder
                .comment(
                        "Enables the " + spellName + " dimension allowlist.",
                        "When enabled, an empty dimensionAllowlist blocks all dimensions."
                )
                .define("enableDimensionAllowlist", false);
        var dimensionAllowlist = builder
                .comment("Dimension IDs where " + spellName + " may " + actionDescription + " when enableDimensionAllowlist is true.")
                .defineListAllowEmpty("dimensionAllowlist", List.<String>of(), SpellDimensionRestrictionServerConfig::isDimensionId);

        return new SpellDimensionRestrictionServerConfig(
                dimensionDenylist,
                enableDimensionAllowlist,
                dimensionAllowlist
        );
    }

    public boolean isDimensionAllowed(ResourceLocation dimensionId) {
        if (containsDimension(dimensionDenylist(), dimensionId)) {
            return false;
        }
        if (!enableDimensionAllowlist()) {
            return true;
        }
        return containsDimension(dimensionAllowlist(), dimensionId);
    }

    public List<String> dimensionDenylist() {
        return Objects.requireNonNullElseGet(dimensionDenylistOverride, () -> stringList(dimensionDenylist));
    }

    public boolean enableDimensionAllowlist() {
        if (enableDimensionAllowlistOverride != null) {
            return enableDimensionAllowlistOverride;
        }
        return enableDimensionAllowlist.get();
    }

    public List<String> dimensionAllowlist() {
        return Objects.requireNonNullElseGet(dimensionAllowlistOverride, () -> stringList(dimensionAllowlist));
    }

    public void setForGameTest(
            List<String> dimensionDenylist,
            boolean enableDimensionAllowlist,
            List<String> dimensionAllowlist
    ) {
        dimensionDenylistOverride = List.copyOf(dimensionDenylist);
        enableDimensionAllowlistOverride = enableDimensionAllowlist;
        dimensionAllowlistOverride = List.copyOf(dimensionAllowlist);
    }

    private static boolean containsDimension(List<String> configuredIds, ResourceLocation dimensionId) {
        return configuredIds.stream()
                .map(ResourceLocation::tryParse)
                .anyMatch(dimensionId::equals);
    }

    private static boolean isDimensionId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }

    private static List<String> stringList(ModConfigSpec.ConfigValue<List<? extends String>> configValue) {
        return configValue.get().stream()
                .map(String::valueOf)
                .toList();
    }
}
