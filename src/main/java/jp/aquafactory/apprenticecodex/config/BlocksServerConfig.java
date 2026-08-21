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

    int apprenticeDeskInkMaxUses(io.redspace.ironsspellbooks.api.spells.SpellRarity rarity) {
        return apprenticeDeskConfig.inkMaxUses(rarity);
    }

    boolean apprenticeDeskReturnGlassBottleWhenInkDepleted() {
        return apprenticeDeskConfig.returnGlassBottleWhenInkDepleted();
    }

    void setApprenticeDeskInkConfigForGameTest(
            int common,
            int uncommon,
            int rare,
            int epic,
            int legendary,
            boolean returnGlassBottle
    ) {
        apprenticeDeskConfig.setInkConfigForGameTest(
                common,
                uncommon,
                rare,
                epic,
                legendary,
                returnGlassBottle
        );
    }

    int arcanumInAJarTicksPerStoredParameter() {
        return arcanumInAJarConfig.ticksPerStoredParameter();
    }

    void setArcanumInAJarTicksPerStoredParameterForGameTest(int value) {
        arcanumInAJarConfig.setTicksPerStoredParameterForGameTest(value);
    }

    ArcanumInAJarServerConfig.ItemSettings arcanumInAJarItemSettings() {
        return arcanumInAJarConfig.itemSettings();
    }

    void captureArcanumInAJarItemSettingsOnConfigLoad() {
        arcanumInAJarConfig.captureItemSettingsOnConfigLoad();
    }

    boolean markArcanumInAJarInvalidItemSettingsWarningLogged() {
        return arcanumInAJarConfig.markInvalidItemSettingsWarningLogged();
    }

    void setArcanumInAJarItemSettingsForGameTest(ArcanumInAJarServerConfig.ItemSettings settings) {
        arcanumInAJarConfig.setItemSettingsForGameTest(settings);
    }

    boolean spellDispenserEnable() {
        return spellDispenserConfig.enable();
    }

    boolean spellDispenserIsSpellAllowedByServerAllowlist(net.minecraft.resources.ResourceLocation spellId) {
        return spellDispenserConfig.isSpellAllowedByServerAllowlist(spellId);
    }

    double spellDispenserCooldownMultiplier() {
        return spellDispenserConfig.cooldownMultiplier();
    }

    boolean creativeSpellDispenserManaConsumption() {
        return spellDispenserConfig.creativeManaConsumption();
    }

    double creativeSpellDispenserCooldownMultiplier() {
        return spellDispenserConfig.creativeCooldownMultiplier();
    }

    java.util.List<String> spellDispenserSpellAllowlist() {
        return spellDispenserConfig.spellAllowlist();
    }

    boolean spellDispenserEnableSpellAllowlist() {
        return spellDispenserConfig.enableSpellAllowlist();
    }

    void setSpellDispenserConfigForGameTest(
            boolean enable,
            boolean enableSpellAllowlist,
            java.util.List<String> spellAllowlist,
            double cooldownMultiplier
    ) {
        spellDispenserConfig.setForGameTest(enable, enableSpellAllowlist, spellAllowlist, cooldownMultiplier);
    }

    void setCreativeSpellDispenserConfigForGameTest(boolean manaConsumption, double cooldownMultiplier) {
        spellDispenserConfig.setCreativeForGameTest(manaConsumption, cooldownMultiplier);
    }
}
