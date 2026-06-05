package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.config.block.ApprenticeDeskServerConfig;
import jp.aquafactory.apprenticecodex.config.block.ArcanumInAJarServerConfig;
import jp.aquafactory.apprenticecodex.config.block.SpellDispenserServerConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

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

    static BlocksServerConfig define(ModConfigSpec.Builder builder) {
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
