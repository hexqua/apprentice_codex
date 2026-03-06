package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.config.item.ArcanumInAJarServerConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

final class BlocksServerConfig {
    private final ApprenticeDeskServerConfig apprenticeDeskConfig;
    private final ArcanumInAJarServerConfig arcanumInAJarConfig;

    private BlocksServerConfig(
            ApprenticeDeskServerConfig apprenticeDeskConfig,
            ArcanumInAJarServerConfig arcanumInAJarConfig
    ) {
        this.apprenticeDeskConfig = apprenticeDeskConfig;
        this.arcanumInAJarConfig = arcanumInAJarConfig;
    }

    static BlocksServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("Blocks");
        var apprenticeDeskConfig = ApprenticeDeskServerConfig.define(builder);
        var arcanumInAJarConfig = ArcanumInAJarServerConfig.define(builder);
        builder.pop();

        return new BlocksServerConfig(
                apprenticeDeskConfig,
                arcanumInAJarConfig
        );
    }

    boolean apprenticeDeskEnableSpellCraftBlacklist() {
        return apprenticeDeskConfig.enableSpellCraftBlacklist();
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
}
