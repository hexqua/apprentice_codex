package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.config.spell.AutoMagnetServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.BoundBowServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.DemicreatorWingsServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.ForceFieldServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.LinearBuildServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.MistFormServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.RemoteEyeServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.RiftHoleServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class SpellsServerConfig {
    private final AutoMagnetServerConfig autoMagnetConfig;
    private final BoundBowServerConfig boundBowConfig;
    private final DemicreatorWingsServerConfig demicreatorWingsConfig;
    private final ForceFieldServerConfig forceFieldConfig;
    private final LinearBuildServerConfig linearBuildConfig;
    private final MistFormServerConfig mistFormConfig;
    private final RemoteEyeServerConfig remoteEyeConfig;
    private final RiftHoleServerConfig riftHoleConfig;

    private SpellsServerConfig(
            AutoMagnetServerConfig autoMagnetConfig,
            BoundBowServerConfig boundBowConfig,
            DemicreatorWingsServerConfig demicreatorWingsConfig,
            ForceFieldServerConfig forceFieldConfig,
            LinearBuildServerConfig linearBuildConfig,
            MistFormServerConfig mistFormConfig,
            RemoteEyeServerConfig remoteEyeConfig,
            RiftHoleServerConfig riftHoleConfig
    ) {
        this.autoMagnetConfig = autoMagnetConfig;
        this.boundBowConfig = boundBowConfig;
        this.demicreatorWingsConfig = demicreatorWingsConfig;
        this.forceFieldConfig = forceFieldConfig;
        this.linearBuildConfig = linearBuildConfig;
        this.mistFormConfig = mistFormConfig;
        this.remoteEyeConfig = remoteEyeConfig;
        this.riftHoleConfig = riftHoleConfig;
    }

    static SpellsServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("Spells");
        var autoMagnetConfig = AutoMagnetServerConfig.define(builder);
        var boundBowConfig = BoundBowServerConfig.define(builder);
        var demicreatorWingsConfig = DemicreatorWingsServerConfig.define(builder);
        var forceFieldConfig = ForceFieldServerConfig.define(builder);
        var linearBuildConfig = LinearBuildServerConfig.define(builder);
        var mistFormConfig = MistFormServerConfig.define(builder);
        var remoteEyeConfig = RemoteEyeServerConfig.define(builder);
        var riftHoleConfig = RiftHoleServerConfig.define(builder);
        builder.pop();

        return new SpellsServerConfig(
                autoMagnetConfig,
                boundBowConfig,
                demicreatorWingsConfig,
                forceFieldConfig,
                linearBuildConfig,
                mistFormConfig,
                remoteEyeConfig,
                riftHoleConfig
        );
    }

    boolean autoMagnetDisableCollectManaCost() {
        return autoMagnetConfig.disableCollectManaCost();
    }

    int boundBowMaxPowerEnchantmentLevel() {
        return boundBowConfig.maxPowerEnchantmentLevel();
    }

    float boundBowForgeArrowManaCost() {
        return boundBowConfig.forgeArrowManaCost();
    }

    void setBoundBowConfigForGameTest(int maxPowerEnchantmentLevel, float forgeArrowManaCost) {
        boundBowConfig.setForGameTest(maxPowerEnchantmentLevel, forgeArrowManaCost);
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

    LinearBuildServerConfig.Values linearBuildConfig() {
        return linearBuildConfig.values();
    }

    void setLinearBuildConfigForGameTest(LinearBuildServerConfig.Values values) {
        linearBuildConfig.setForGameTest(values);
    }

    boolean isMistFormPassableBlockDenied(BlockState state) {
        return mistFormConfig.isPassableBlockDenied(state);
    }

    List<String> mistFormPassableBlockDenylist() {
        return mistFormConfig.passableBlockDenylist();
    }

    void setMistFormPassableBlockDenylistForGameTest(List<String> passableBlockDenylist) {
        mistFormConfig.setPassableBlockDenylistForGameTest(passableBlockDenylist);
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
