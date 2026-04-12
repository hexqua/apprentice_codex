package jp.aquafactory.apprenticecodex.config.block;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SpellDispenserServerConfig {
    private final ModConfigSpec.BooleanValue ignoreSpellProfileAndDenylistFiles;

    private SpellDispenserServerConfig(ModConfigSpec.BooleanValue ignoreSpellProfileAndDenylistFiles) {
        this.ignoreSpellProfileAndDenylistFiles = ignoreSpellProfileAndDenylistFiles;
    }

    public static SpellDispenserServerConfig define(ModConfigSpec.Builder builder) {
        builder.comment(
                        "WARNING: Ignores Spell Dispenser spell profile and denylist config files.",
                        "Use at your own risk. This can allow unsupported or intentionally blocked spells."
                )
                .push("SpellDispenser");

        var ignoreSpellProfileAndDenylistFiles = builder.define("ignoreSpellProfileAndDenylistFiles", false);

        builder.pop();
        return new SpellDispenserServerConfig(ignoreSpellProfileAndDenylistFiles);
    }

    public boolean ignoreSpellProfileAndDenylistFiles() {
        return ignoreSpellProfileAndDenylistFiles.get();
    }
}
