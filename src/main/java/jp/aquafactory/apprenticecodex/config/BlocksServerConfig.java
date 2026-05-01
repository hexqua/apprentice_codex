package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.config.block.ApprenticeDeskServerConfig;
import jp.aquafactory.apprenticecodex.config.block.ArcanumInAJarServerConfig;
import jp.aquafactory.apprenticecodex.config.block.SpellDispenserServerConfig;
import net.minecraftforge.common.ForgeConfigSpec;

final class BlocksServerConfig {
    private final ApprenticeDeskServerConfig apprenticeDeskConfig;
    private final ArcanumInAJarServerConfig arcanumInAJarConfig;
    private final SpellDispenserServerConfig spellDispenserConfig;

    private BlocksServerConfig(
            ApprenticeDeskServerConfig apprenticeDeskConfig,
            ArcanumInAJarServerConfig arcanumInAJarConfig,
            SpellDispenserServerConfig spellDispenserConfig
    ) {
        this.apprenticeDeskConfig = apprenticeDeskConfig;
        this.arcanumInAJarConfig = arcanumInAJarConfig;
        this.spellDispenserConfig = spellDispenserConfig;
    }

    static BlocksServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("Blocks");
        var apprenticeDeskConfig = ApprenticeDeskServerConfig.define(builder);
        var arcanumInAJarConfig = ArcanumInAJarServerConfig.define(builder);
        var spellDispenserConfig = SpellDispenserServerConfig.define(builder);
        builder.pop();

        return new BlocksServerConfig(
                apprenticeDeskConfig,
                arcanumInAJarConfig,
                spellDispenserConfig
        );
    }

    boolean apprenticeDeskEnableSpellCraftBlacklist() {
        return apprenticeDeskConfig.enableSpellCraftBlacklist();
    }

    boolean apprenticeDeskDisableNonJobSiteFeatures() {
        return apprenticeDeskConfig.disableNonJobSiteFeatures();
    }

    java.util.List<String> apprenticeDeskSpellCraftBlacklist() {
        return apprenticeDeskConfig.spellCraftBlacklist();
    }

    boolean apprenticeDeskRequireSameSchool() {
        return apprenticeDeskConfig.requireSameSchool();
    }

    boolean apprenticeDeskDisableCommonRarityConversion() {
        return apprenticeDeskConfig.disableCommonRarityConversion();
    }

    int arcanumInAJarTicksPerStoredParameter() {
        return arcanumInAJarConfig.ticksPerStoredParameter();
    }

    boolean spellDispenserIgnoreSpellProfileAndDenylistFiles() {
        return spellDispenserConfig.ignoreSpellProfileAndDenylistFiles();
    }
}
