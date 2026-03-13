package jp.aquafactory.apprenticecodex.config.spell;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AutoMagnetServerConfig {
    private final ModConfigSpec.BooleanValue disableCollectManaCost;

    private AutoMagnetServerConfig(ModConfigSpec.BooleanValue disableCollectManaCost) {
        this.disableCollectManaCost = disableCollectManaCost;
    }

    public static AutoMagnetServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("AutoMagnet");

        var disableCollectManaCost = builder.define("disableCollectManaCost", false);

        builder.pop();
        return new AutoMagnetServerConfig(disableCollectManaCost);
    }

    public boolean disableCollectManaCost() {
        return disableCollectManaCost.get();
    }
}
