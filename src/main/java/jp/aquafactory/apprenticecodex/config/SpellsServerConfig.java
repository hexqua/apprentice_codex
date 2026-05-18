package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.config.spell.AutoMagnetServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.DemicreatorWingsServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.ForceFieldServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.RemoteEyeServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.RiftHoleServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class SpellsServerConfig {
    private final AutoMagnetServerConfig autoMagnetConfig;
    private final DemicreatorWingsServerConfig demicreatorWingsConfig;
    private final ForceFieldServerConfig forceFieldConfig;
    private final RemoteEyeServerConfig remoteEyeConfig;
    private final RiftHoleServerConfig riftHoleConfig;

    private SpellsServerConfig(
            AutoMagnetServerConfig autoMagnetConfig,
            DemicreatorWingsServerConfig demicreatorWingsConfig,
            ForceFieldServerConfig forceFieldConfig,
            RemoteEyeServerConfig remoteEyeConfig,
            RiftHoleServerConfig riftHoleConfig
    ) {
        this.autoMagnetConfig = autoMagnetConfig;
        this.demicreatorWingsConfig = demicreatorWingsConfig;
        this.forceFieldConfig = forceFieldConfig;
        this.remoteEyeConfig = remoteEyeConfig;
        this.riftHoleConfig = riftHoleConfig;
    }

    static SpellsServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("Spells");
        var autoMagnetConfig = AutoMagnetServerConfig.define(builder);
        var demicreatorWingsConfig = DemicreatorWingsServerConfig.define(builder);
        var forceFieldConfig = ForceFieldServerConfig.define(builder);
        var remoteEyeConfig = RemoteEyeServerConfig.define(builder);
        var riftHoleConfig = RiftHoleServerConfig.define(builder);
        builder.pop();

        return new SpellsServerConfig(
                autoMagnetConfig,
                demicreatorWingsConfig,
                forceFieldConfig,
                remoteEyeConfig,
                riftHoleConfig
        );
    }

    boolean autoMagnetDisableCollectManaCost() {
        return autoMagnetConfig.disableCollectManaCost();
    }

    boolean isDemicreatorWingsDimensionAllowed(ResourceLocation dimensionId) {
        return demicreatorWingsConfig.isDimensionAllowed(dimensionId);
    }

    List<String> demicreatorWingsDimensionDenylist() {
        return demicreatorWingsConfig.dimensionDenylist();
    }

    boolean demicreatorWingsEnableDimensionAllowlist() {
        return demicreatorWingsConfig.enableDimensionAllowlist();
    }

    List<String> demicreatorWingsDimensionAllowlist() {
        return demicreatorWingsConfig.dimensionAllowlist();
    }

    void setDemicreatorWingsConfigForGameTest(
            List<String> dimensionDenylist,
            boolean enableDimensionAllowlist,
            List<String> dimensionAllowlist
    ) {
        demicreatorWingsConfig.setForGameTest(dimensionDenylist, enableDimensionAllowlist, dimensionAllowlist);
    }

    float forceFieldDrainManaBasePerHit() {
        return forceFieldConfig.drainManaBasePerHit();
    }

    boolean isRemoteEyeDimensionAllowed(ResourceLocation dimensionId) {
        return remoteEyeConfig.isDimensionAllowed(dimensionId);
    }

    List<String> remoteEyeDimensionDenylist() {
        return remoteEyeConfig.dimensionDenylist();
    }

    boolean remoteEyeEnableDimensionAllowlist() {
        return remoteEyeConfig.enableDimensionAllowlist();
    }

    List<String> remoteEyeDimensionAllowlist() {
        return remoteEyeConfig.dimensionAllowlist();
    }

    void setRemoteEyeConfigForGameTest(
            List<String> dimensionDenylist,
            boolean enableDimensionAllowlist,
            List<String> dimensionAllowlist
    ) {
        remoteEyeConfig.setForGameTest(dimensionDenylist, enableDimensionAllowlist, dimensionAllowlist);
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
