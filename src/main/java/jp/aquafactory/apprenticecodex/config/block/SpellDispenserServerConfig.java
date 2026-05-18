package jp.aquafactory.apprenticecodex.config.block;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Objects;

public final class SpellDispenserServerConfig {
    private final ModConfigSpec.BooleanValue enable;
    private final ModConfigSpec.BooleanValue enableSpellAllowlist;
    private final ModConfigSpec.ConfigValue<List<? extends String>> spellAllowlist;
    private final ModConfigSpec.DoubleValue cooldownMultiplier;
    private final ModConfigSpec.BooleanValue ignoreSpellProfileAndDenylistFiles;
    private Boolean enableOverride;
    private Boolean enableSpellAllowlistOverride;
    private List<String> spellAllowlistOverride;
    private Double cooldownMultiplierOverride;

    private SpellDispenserServerConfig(
            ModConfigSpec.BooleanValue enable,
            ModConfigSpec.BooleanValue enableSpellAllowlist,
            ModConfigSpec.ConfigValue<List<? extends String>> spellAllowlist,
            ModConfigSpec.DoubleValue cooldownMultiplier,
            ModConfigSpec.BooleanValue ignoreSpellProfileAndDenylistFiles
    ) {
        this.enable = enable;
        this.enableSpellAllowlist = enableSpellAllowlist;
        this.spellAllowlist = spellAllowlist;
        this.cooldownMultiplier = cooldownMultiplier;
        this.ignoreSpellProfileAndDenylistFiles = ignoreSpellProfileAndDenylistFiles;
    }

    public static SpellDispenserServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("SpellDispenser");

        var enable = builder
                .comment("Enables Spell Dispenser redstone and contraption casting.")
                .define("enable", true);
        var enableSpellAllowlist = builder
                .comment(
                        "Enables the server-side Spell Dispenser spell allowlist.",
                        "When enabled, an empty spellAllowlist blocks all spells."
                )
                .define("enableSpellAllowlist", false);
        var spellAllowlist = builder
                .comment("Namespaced spell IDs that Spell Dispenser may cast when enableSpellAllowlist is true.")
                .defineList("spellAllowlist", List.<String>of(), SpellDispenserServerConfig::isSpellId);
        var cooldownMultiplier = builder
                .comment("Multiplier applied to Spell Dispenser cooldown ticks. Minimum is 0.1.")
                .defineInRange("cooldownMultiplier", 1.0d, 0.1d, Double.MAX_VALUE);
        var ignoreSpellProfileAndDenylistFiles = builder
                .comment(
                        "WARNING: Ignores Spell Dispenser spell profile and denylist config files.",
                        "Use at your own risk. This can allow unsupported or intentionally blocked spells.",
                        "This does not bypass enable or the server-side spell allowlist."
                )
                .define("ignoreSpellProfileAndDenylistFiles", false);

        builder.pop();
        return new SpellDispenserServerConfig(
                enable,
                enableSpellAllowlist,
                spellAllowlist,
                cooldownMultiplier,
                ignoreSpellProfileAndDenylistFiles
        );
    }

    public boolean enable() {
        if (enableOverride != null) {
            return enableOverride;
        }
        return enable.get();
    }

    public boolean isSpellAllowedByServerAllowlist(ResourceLocation spellId) {
        if (!enableSpellAllowlist()) {
            return true;
        }

        return spellAllowlist().stream()
                .map(ResourceLocation::tryParse)
                .anyMatch(spellId::equals);
    }

    public boolean enableSpellAllowlist() {
        if (enableSpellAllowlistOverride != null) {
            return enableSpellAllowlistOverride;
        }
        return enableSpellAllowlist.get();
    }

    public List<String> spellAllowlist() {
        return Objects.requireNonNullElseGet(spellAllowlistOverride, () -> stringList(spellAllowlist));
    }

    public double cooldownMultiplier() {
        if (cooldownMultiplierOverride != null) {
            return cooldownMultiplierOverride;
        }
        return cooldownMultiplier.get();
    }

    public boolean ignoreSpellProfileAndDenylistFiles() {
        return ignoreSpellProfileAndDenylistFiles.get();
    }

    public void setForGameTest(
            boolean enable,
            boolean enableSpellAllowlist,
            List<String> spellAllowlist,
            double cooldownMultiplier
    ) {
        enableOverride = enable;
        enableSpellAllowlistOverride = enableSpellAllowlist;
        spellAllowlistOverride = List.copyOf(spellAllowlist);
        cooldownMultiplierOverride = Math.max(0.1d, cooldownMultiplier);
    }

    private static boolean isSpellId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }

    private static List<String> stringList(ModConfigSpec.ConfigValue<List<? extends String>> configValue) {
        return configValue.get().stream()
                .map(String::valueOf)
                .toList();
    }
}
