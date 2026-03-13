package jp.aquafactory.apprenticecodex.config.block;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class ApprenticeDeskServerConfig {
    private final ForgeConfigSpec.BooleanValue enableSpellCraftBlacklist;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> spellCraftBlacklist;
    private final ForgeConfigSpec.BooleanValue requireSameSchool;
    private final ForgeConfigSpec.BooleanValue disableCommonRarityConversion;

    private ApprenticeDeskServerConfig(
            ForgeConfigSpec.BooleanValue enableSpellCraftBlacklist,
            ForgeConfigSpec.ConfigValue<List<? extends String>> spellCraftBlacklist,
            ForgeConfigSpec.BooleanValue requireSameSchool,
            ForgeConfigSpec.BooleanValue disableCommonRarityConversion
    ) {
        this.enableSpellCraftBlacklist = enableSpellCraftBlacklist;
        this.spellCraftBlacklist = spellCraftBlacklist;
        this.requireSameSchool = requireSameSchool;
        this.disableCommonRarityConversion = disableCommonRarityConversion;
    }

    public static ApprenticeDeskServerConfig define(ForgeConfigSpec.Builder builder) {
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
