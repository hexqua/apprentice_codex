package jp.aquafactory.apprenticecodex.config.block;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SpellDispenserServerConfig {
    private final ForgeConfigSpec.BooleanValue ignoreSpellProfileAndDenylistFiles;

    private SpellDispenserServerConfig(ForgeConfigSpec.BooleanValue ignoreSpellProfileAndDenylistFiles) {
        this.ignoreSpellProfileAndDenylistFiles = ignoreSpellProfileAndDenylistFiles;
    }

    public static SpellDispenserServerConfig define(ForgeConfigSpec.Builder builder) {
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
