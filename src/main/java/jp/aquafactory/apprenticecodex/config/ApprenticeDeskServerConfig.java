package jp.aquafactory.apprenticecodex.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

final class ApprenticeDeskServerConfig {
    private final ModConfigSpec.BooleanValue enableSpellCraftBlacklist;
    private final ModConfigSpec.ConfigValue<List<? extends String>> spellCraftBlacklist;
    private final ModConfigSpec.BooleanValue requireSameSchool;
    private final ModConfigSpec.BooleanValue disableCommonRarityConversion;

    private ApprenticeDeskServerConfig(
            ModConfigSpec.BooleanValue enableSpellCraftBlacklist,
            ModConfigSpec.ConfigValue<List<? extends String>> spellCraftBlacklist,
            ModConfigSpec.BooleanValue requireSameSchool,
            ModConfigSpec.BooleanValue disableCommonRarityConversion
    ) {
        this.enableSpellCraftBlacklist = enableSpellCraftBlacklist;
        this.spellCraftBlacklist = spellCraftBlacklist;
        this.requireSameSchool = requireSameSchool;
        this.disableCommonRarityConversion = disableCommonRarityConversion;
    }

    static ApprenticeDeskServerConfig define(ModConfigSpec.Builder builder) {
        builder.comment("spellCraftBlacklist : \"modid:spell_id\"(example: \"irons_spellbooks:black_hole\")")
                .push("ApprenticeDesk");

        var enableSpellCraftBlacklist = builder.define("enableSpellCraftBlacklist", false);
        var spellCraftBlacklist = builder.defineList("spellCraftBlacklist", List.<String>of(),
                value -> value instanceof String text && !text.isBlank());
        var requireSameSchool = builder.define("requireSameSchool", false);
        var disableCommonRarityConversion = builder.define("disableCommonRarityConversion", false);

        builder.pop();
        return new ApprenticeDeskServerConfig(
                enableSpellCraftBlacklist,
                spellCraftBlacklist,
                requireSameSchool,
                disableCommonRarityConversion
        );
    }

    boolean enableSpellCraftBlacklist() {
        return enableSpellCraftBlacklist.get();
    }

    List<String> spellCraftBlacklist() {
        return spellCraftBlacklist.get().stream()
                .map(String::valueOf)
                .toList();
    }

    boolean requireSameSchool() {
        return requireSameSchool.get();
    }

    boolean disableCommonRarityConversion() {
        return disableCommonRarityConversion.get();
    }
}
