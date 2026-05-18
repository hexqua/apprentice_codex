package jp.aquafactory.apprenticecodex.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

final class ProcessingServerConfig {
    private final ModConfigSpec.ConfigValue<List<? extends String>> spellcasterWorkbenchRecipeDenylist;
    private final ModConfigSpec.ConfigValue<List<? extends String>> essenceSmokerRecipeDenylist;
    private final ModConfigSpec.ConfigValue<List<? extends String>> grindRunnerRecipeDenylist;
    private final ModConfigSpec.ConfigValue<List<? extends String>> thermalProcessRecipeDenylist;

    private ProcessingServerConfig(
            ModConfigSpec.ConfigValue<List<? extends String>> spellcasterWorkbenchRecipeDenylist,
            ModConfigSpec.ConfigValue<List<? extends String>> essenceSmokerRecipeDenylist,
            ModConfigSpec.ConfigValue<List<? extends String>> grindRunnerRecipeDenylist,
            ModConfigSpec.ConfigValue<List<? extends String>> thermalProcessRecipeDenylist
    ) {
        this.spellcasterWorkbenchRecipeDenylist = spellcasterWorkbenchRecipeDenylist;
        this.essenceSmokerRecipeDenylist = essenceSmokerRecipeDenylist;
        this.grindRunnerRecipeDenylist = grindRunnerRecipeDenylist;
        this.thermalProcessRecipeDenylist = thermalProcessRecipeDenylist;
    }

    static ProcessingServerConfig define(ModConfigSpec.Builder builder) {
        builder.comment("Recipe ID denylists for Apprentice's Codex processing features. Entries use \"modid:path\".")
                .push("Processing");

        var spellcasterWorkbenchRecipeDenylist = builder
                .comment("Disable matching Spellcaster Workbench recipes by recipe ID.")
                .defineList("spellcasterWorkbenchRecipeDenylist", List.<String>of(), ProcessingServerConfig::isRecipeId);
        var essenceSmokerRecipeDenylist = builder
                .comment("Disable matching Essence Smoker recipes by recipe ID.")
                .defineList("essenceSmokerRecipeDenylist", List.<String>of(), ProcessingServerConfig::isRecipeId);
        var grindRunnerRecipeDenylist = builder
                .comment("Disable matching Grind Runner recipes by recipe ID.")
                .defineList("grindRunnerRecipeDenylist", List.<String>of(), ProcessingServerConfig::isRecipeId);
        var thermalProcessRecipeDenylist = builder
                .comment("Disable matching Thermal Process cooking recipes by recipe ID.")
                .defineList("thermalProcessRecipeDenylist", List.<String>of(), ProcessingServerConfig::isRecipeId);

        builder.pop();
        return new ProcessingServerConfig(
                spellcasterWorkbenchRecipeDenylist,
                essenceSmokerRecipeDenylist,
                grindRunnerRecipeDenylist,
                thermalProcessRecipeDenylist
        );
    }

    boolean isSpellcasterWorkbenchRecipeDenied(ResourceLocation recipeId) {
        return containsRecipeId(spellcasterWorkbenchRecipeDenylist, recipeId);
    }

    boolean isEssenceSmokerRecipeDenied(ResourceLocation recipeId) {
        return containsRecipeId(essenceSmokerRecipeDenylist, recipeId);
    }

    boolean isGrindRunnerRecipeDenied(ResourceLocation recipeId) {
        return containsRecipeId(grindRunnerRecipeDenylist, recipeId);
    }

    boolean isThermalProcessRecipeDenied(ResourceLocation recipeId) {
        return containsRecipeId(thermalProcessRecipeDenylist, recipeId);
    }

    void setRecipeDenylistsForGameTest(
            List<String> spellcasterWorkbenchRecipeDenylist,
            List<String> essenceSmokerRecipeDenylist,
            List<String> grindRunnerRecipeDenylist,
            List<String> thermalProcessRecipeDenylist
    ) {
        this.spellcasterWorkbenchRecipeDenylist.set(List.copyOf(spellcasterWorkbenchRecipeDenylist));
        this.essenceSmokerRecipeDenylist.set(List.copyOf(essenceSmokerRecipeDenylist));
        this.grindRunnerRecipeDenylist.set(List.copyOf(grindRunnerRecipeDenylist));
        this.thermalProcessRecipeDenylist.set(List.copyOf(thermalProcessRecipeDenylist));
    }

    List<String> spellcasterWorkbenchRecipeDenylist() {
        return stringList(spellcasterWorkbenchRecipeDenylist);
    }

    List<String> essenceSmokerRecipeDenylist() {
        return stringList(essenceSmokerRecipeDenylist);
    }

    List<String> grindRunnerRecipeDenylist() {
        return stringList(grindRunnerRecipeDenylist);
    }

    List<String> thermalProcessRecipeDenylist() {
        return stringList(thermalProcessRecipeDenylist);
    }

    private static boolean containsRecipeId(
            ModConfigSpec.ConfigValue<List<? extends String>> configValue,
            ResourceLocation recipeId
    ) {
        for (var configuredId : configValue.get()) {
            if (recipeId.equals(ResourceLocation.tryParse(String.valueOf(configuredId)))) {
                return true;
            }
        }
        return false;
    }

    private static List<String> stringList(ModConfigSpec.ConfigValue<List<? extends String>> configValue) {
        return configValue.get().stream()
                .map(String::valueOf)
                .toList();
    }

    private static boolean isRecipeId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }
}
