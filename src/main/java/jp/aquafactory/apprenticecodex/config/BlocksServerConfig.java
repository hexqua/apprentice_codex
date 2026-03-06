package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.config.item.ArcanumInAJarServerConfig;
import net.minecraftforge.common.ForgeConfigSpec;

final class BlocksServerConfig {
    private final ArcanumInAJarServerConfig arcanumInAJarConfig;

    private BlocksServerConfig(
            ArcanumInAJarServerConfig arcanumInAJarConfig
    ) {
        this.arcanumInAJarConfig = arcanumInAJarConfig;
    }

    static BlocksServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("Blocks");
        var arcanumInAJarConfig = ArcanumInAJarServerConfig.define(builder);
        builder.pop();

        return new BlocksServerConfig(
                arcanumInAJarConfig
        );
    }

    int arcanumInAJarTicksPerStoredParameter() {
        return arcanumInAJarConfig.ticksPerStoredParameter();
    }
}
