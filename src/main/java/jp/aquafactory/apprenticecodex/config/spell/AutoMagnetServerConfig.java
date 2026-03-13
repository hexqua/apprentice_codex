package jp.aquafactory.apprenticecodex.config.spell;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AutoMagnetServerConfig {
    private final ForgeConfigSpec.BooleanValue disableCollectManaCost;

    private AutoMagnetServerConfig(ForgeConfigSpec.BooleanValue disableCollectManaCost) {
        this.disableCollectManaCost = disableCollectManaCost;
    }

    public static AutoMagnetServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("AutoMagnet");

        var disableCollectManaCost = builder.define("disableCollectManaCost", false);

        builder.pop();
        return new AutoMagnetServerConfig(disableCollectManaCost);
    }

    public boolean disableCollectManaCost() {
        return disableCollectManaCost.get();
    }
}
