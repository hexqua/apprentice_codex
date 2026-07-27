package jp.aquafactory.apprenticecodex.config.block;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class ApprenticeDeskServerConfig {
    private static final int MAX_INK_USES = 1_000_000;

    private final ForgeConfigSpec.BooleanValue disableNonJobSiteFeatures;
    private final ForgeConfigSpec.BooleanValue enableSpellCraftBlacklist;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> spellCraftBlacklist;
    private final ForgeConfigSpec.IntValue commonInkMaxUses;
    private final ForgeConfigSpec.IntValue uncommonInkMaxUses;
    private final ForgeConfigSpec.IntValue rareInkMaxUses;
    private final ForgeConfigSpec.IntValue epicInkMaxUses;
    private final ForgeConfigSpec.IntValue legendaryInkMaxUses;
    private final ForgeConfigSpec.BooleanValue returnGlassBottleWhenInkDepleted;

    private ApprenticeDeskServerConfig(
            ForgeConfigSpec.BooleanValue disableNonJobSiteFeatures,
            ForgeConfigSpec.BooleanValue enableSpellCraftBlacklist,
            ForgeConfigSpec.ConfigValue<List<? extends String>> spellCraftBlacklist,
            ForgeConfigSpec.IntValue commonInkMaxUses,
            ForgeConfigSpec.IntValue uncommonInkMaxUses,
            ForgeConfigSpec.IntValue rareInkMaxUses,
            ForgeConfigSpec.IntValue epicInkMaxUses,
            ForgeConfigSpec.IntValue legendaryInkMaxUses,
            ForgeConfigSpec.BooleanValue returnGlassBottleWhenInkDepleted
    ) {
        this.disableNonJobSiteFeatures = disableNonJobSiteFeatures;
        this.enableSpellCraftBlacklist = enableSpellCraftBlacklist;
        this.spellCraftBlacklist = spellCraftBlacklist;
        this.commonInkMaxUses = commonInkMaxUses;
        this.uncommonInkMaxUses = uncommonInkMaxUses;
        this.rareInkMaxUses = rareInkMaxUses;
        this.epicInkMaxUses = epicInkMaxUses;
        this.legendaryInkMaxUses = legendaryInkMaxUses;
        this.returnGlassBottleWhenInkDepleted = returnGlassBottleWhenInkDepleted;
    }

    public static ApprenticeDeskServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.comment("Entries for spellCraftBlacklist use \"modid:spell_id\" (example: \"irons_spellbooks:black_hole\").")
                .push("ApprenticeDesk");

        var disableNonJobSiteFeatures = builder.define("disableNonJobSiteFeatures", false);
        var enableSpellCraftBlacklist = builder.define("enableSpellCraftBlacklist", false);
        var spellCraftBlacklist = builder.defineListAllowEmpty("spellCraftBlacklist", List.<String>of(),
                value -> value instanceof String text && !text.isBlank());
        var commonInkMaxUses = builder
                .comment("Total Apprentice Desk crafts provided by one Common ink.")
                .defineInRange("commonInkMaxUses", 5, 1, MAX_INK_USES);
        var uncommonInkMaxUses = builder
                .comment("Total Apprentice Desk crafts provided by one Uncommon ink.")
                .defineInRange("uncommonInkMaxUses", 4, 1, MAX_INK_USES);
        var rareInkMaxUses = builder
                .comment("Total Apprentice Desk crafts provided by one Rare ink.")
                .defineInRange("rareInkMaxUses", 3, 1, MAX_INK_USES);
        var epicInkMaxUses = builder
                .comment("Total Apprentice Desk crafts provided by one Epic ink.")
                .defineInRange("epicInkMaxUses", 3, 1, MAX_INK_USES);
        var legendaryInkMaxUses = builder
                .comment("Total Apprentice Desk crafts provided by one Legendary ink.")
                .defineInRange("legendaryInkMaxUses", 2, 1, MAX_INK_USES);
        var returnGlassBottleWhenInkDepleted = builder
                .comment("Return a glass bottle when a partially used ink runs out.")
                .define("returnGlassBottleWhenInkDepleted", true);

        builder.pop();
        return new ApprenticeDeskServerConfig(
                disableNonJobSiteFeatures,
                enableSpellCraftBlacklist,
                spellCraftBlacklist,
                commonInkMaxUses,
                uncommonInkMaxUses,
                rareInkMaxUses,
                epicInkMaxUses,
                legendaryInkMaxUses,
                returnGlassBottleWhenInkDepleted
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

    public int inkMaxUses(io.redspace.ironsspellbooks.api.spells.SpellRarity rarity) {
        return switch (rarity) {
            case COMMON -> commonInkMaxUses.get();
            case UNCOMMON -> uncommonInkMaxUses.get();
            case RARE -> rareInkMaxUses.get();
            case EPIC -> epicInkMaxUses.get();
            case LEGENDARY -> legendaryInkMaxUses.get();
        };
    }

    public boolean returnGlassBottleWhenInkDepleted() {
        return returnGlassBottleWhenInkDepleted.get();
    }

    public void setInkConfigForGameTest(
            int common,
            int uncommon,
            int rare,
            int epic,
            int legendary,
            boolean returnGlassBottle
    ) {
        commonInkMaxUses.set(common);
        uncommonInkMaxUses.set(uncommon);
        rareInkMaxUses.set(rare);
        epicInkMaxUses.set(epic);
        legendaryInkMaxUses.set(legendary);
        returnGlassBottleWhenInkDepleted.set(returnGlassBottle);
    }

}
