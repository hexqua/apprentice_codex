package jp.aquafactory.apprenticecodex.config.block;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class ApprenticeDeskServerConfig {
    private final ModConfigSpec.BooleanValue disableNonJobSiteFeatures;
    private final ModConfigSpec.BooleanValue enableSpellCraftBlacklist;
    private final ModConfigSpec.ConfigValue<List<? extends String>> spellCraftBlacklist;
    private final ModConfigSpec.BooleanValue requireSameSchool;
    private final ModConfigSpec.BooleanValue disableCommonRarityConversion;

    private ApprenticeDeskServerConfig(
            ModConfigSpec.BooleanValue disableNonJobSiteFeatures,
            ModConfigSpec.BooleanValue enableSpellCraftBlacklist,
            ModConfigSpec.ConfigValue<List<? extends String>> spellCraftBlacklist,
            ModConfigSpec.BooleanValue requireSameSchool,
            ModConfigSpec.BooleanValue disableCommonRarityConversion
    ) {
        this.disableNonJobSiteFeatures = disableNonJobSiteFeatures;
        this.enableSpellCraftBlacklist = enableSpellCraftBlacklist;
        this.spellCraftBlacklist = spellCraftBlacklist;
        this.requireSameSchool = requireSameSchool;
        this.disableCommonRarityConversion = disableCommonRarityConversion;
    }

    public static ApprenticeDeskServerConfig define(ModConfigSpec.Builder builder) {
        builder.comment("Entries for spellCraftBlacklist use \"modid:spell_id\" (example: \"irons_spellbooks:black_hole\").")
                .push("ApprenticeDesk");

        var disableNonJobSiteFeatures = builder.define("disableNonJobSiteFeatures", false);
        var enableSpellCraftBlacklist = builder.define("enableSpellCraftBlacklist", false);
        var spellCraftBlacklist = builder.defineListAllowEmpty("spellCraftBlacklist", List.<String>of(),
                value -> value instanceof String text && !text.isBlank());
        var requireSameSchool = builder.define("requireSameSchool", false);
        var disableCommonRarityConversion = builder.define("disableCommonRarityConversion", false);

        builder.pop();
        return new ApprenticeDeskServerConfig(
                disableNonJobSiteFeatures,
                enableSpellCraftBlacklist,
                spellCraftBlacklist,
                requireSameSchool,
                disableCommonRarityConversion
        );
    }

    public boolean disableNonJobSiteFeatures() {
        return disableNonJobSiteFeatures.get();
    }

    public boolean enableSpellCraftBlacklist() {
        return enableSpellCraftBlacklist.get();
    }

    public List<String> spellCraftBlacklist() {
        return spellCraftBlacklist.get().stream()
                .map(String::valueOf)
                .toList();
    }

    public boolean requireSameSchool() {
        return requireSameSchool.get();
    }

    public boolean disableCommonRarityConversion() {
        return disableCommonRarityConversion.get();
    }
}
