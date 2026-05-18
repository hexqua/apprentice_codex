package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.config.spell.AutoMagnetServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.ForceFieldServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.RiftHoleServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class SpellsServerConfig {
    private final AutoMagnetServerConfig autoMagnetConfig;
    private final ForceFieldServerConfig forceFieldConfig;
    private final RiftHoleServerConfig riftHoleConfig;

    private SpellsServerConfig(
            AutoMagnetServerConfig autoMagnetConfig,
            ForceFieldServerConfig forceFieldConfig,
            RiftHoleServerConfig riftHoleConfig
    ) {
        this.autoMagnetConfig = autoMagnetConfig;
        this.forceFieldConfig = forceFieldConfig;
        this.riftHoleConfig = riftHoleConfig;
    }

    static SpellsServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("Spells");
        var autoMagnetConfig = AutoMagnetServerConfig.define(builder);
        var forceFieldConfig = ForceFieldServerConfig.define(builder);
        var riftHoleConfig = RiftHoleServerConfig.define(builder);
        builder.pop();

        return new SpellsServerConfig(
                autoMagnetConfig,
                forceFieldConfig,
                riftHoleConfig
        );
    }

    boolean autoMagnetDisableCollectManaCost() {
        return autoMagnetConfig.disableCollectManaCost();
    }

    float forceFieldDrainManaBasePerHit() {
        return forceFieldConfig.drainManaBasePerHit();
    }

    boolean isRiftHoleDimensionAllowed(ResourceLocation dimensionId) {
        return riftHoleConfig.isDimensionAllowed(dimensionId);
    }

    List<String> riftHoleDimensionDenylist() {
        return riftHoleConfig.dimensionDenylist();
    }

    boolean riftHoleEnableDimensionAllowlist() {
        return riftHoleConfig.enableDimensionAllowlist();
    }

    List<String> riftHoleDimensionAllowlist() {
        return riftHoleConfig.dimensionAllowlist();
    }

    void setRiftHoleConfigForGameTest(
            List<String> dimensionDenylist,
            boolean enableDimensionAllowlist,
            List<String> dimensionAllowlist
    ) {
        riftHoleConfig.setForGameTest(dimensionDenylist, enableDimensionAllowlist, dimensionAllowlist);
    }
}
