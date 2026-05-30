package jp.aquafactory.apprenticecodex.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Objects;

final class ProcessingServerConfig {
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> spellcasterWorkbenchRecipeDenylist;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> essenceSmokerRecipeDenylist;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> grindRunnerRecipeDenylist;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> thermalProcessRecipeDenylist;
    private List<String> spellcasterWorkbenchRecipeDenylistOverride;
    private List<String> essenceSmokerRecipeDenylistOverride;
    private List<String> grindRunnerRecipeDenylistOverride;
    private List<String> thermalProcessRecipeDenylistOverride;

    private ProcessingServerConfig(
            ForgeConfigSpec.ConfigValue<List<? extends String>> spellcasterWorkbenchRecipeDenylist,
            ForgeConfigSpec.ConfigValue<List<? extends String>> essenceSmokerRecipeDenylist,
            ForgeConfigSpec.ConfigValue<List<? extends String>> grindRunnerRecipeDenylist,
            ForgeConfigSpec.ConfigValue<List<? extends String>> thermalProcessRecipeDenylist
    ) {
        this.spellcasterWorkbenchRecipeDenylist = spellcasterWorkbenchRecipeDenylist;
        this.essenceSmokerRecipeDenylist = essenceSmokerRecipeDenylist;
        this.grindRunnerRecipeDenylist = grindRunnerRecipeDenylist;
        this.thermalProcessRecipeDenylist = thermalProcessRecipeDenylist;
    }

    static ProcessingServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.comment("Recipe ID denylists for Apprentice's Codex processing features. Entries use \"modid:path\".")
                .push("Processing");

        var spellcasterWorkbenchRecipeDenylist = builder
                .comment("Disable matching Spellcaster Workbench recipes by recipe ID.")
                .defineListAllowEmpty("spellcasterWorkbenchRecipeDenylist", List.<String>of(), ProcessingServerConfig::isRecipeId);
        var essenceSmokerRecipeDenylist = builder
                .comment("Disable matching Essence Smoker recipes by recipe ID.")
                .defineListAllowEmpty("essenceSmokerRecipeDenylist", List.<String>of(), ProcessingServerConfig::isRecipeId);
        var grindRunnerRecipeDenylist = builder
                .comment("Disable matching Grind Runner recipes by recipe ID.")
                .defineListAllowEmpty("grindRunnerRecipeDenylist", List.<String>of(), ProcessingServerConfig::isRecipeId);
        var thermalProcessRecipeDenylist = builder
                .comment("Disable matching Thermal Process cooking recipes by recipe ID.")
                .defineListAllowEmpty("thermalProcessRecipeDenylist", List.<String>of(), ProcessingServerConfig::isRecipeId);

        builder.pop();
        return new ProcessingServerConfig(
                spellcasterWorkbenchRecipeDenylist,
                essenceSmokerRecipeDenylist,
                grindRunnerRecipeDenylist,
                thermalProcessRecipeDenylist
        );
    }

    boolean isSpellcasterWorkbenchRecipeDenied(ResourceLocation recipeId) {
        return containsRecipeId(spellcasterWorkbenchRecipeDenylist(), recipeId);
    }

    boolean isEssenceSmokerRecipeDenied(ResourceLocation recipeId) {
        return containsRecipeId(essenceSmokerRecipeDenylist(), recipeId);
    }

    boolean isGrindRunnerRecipeDenied(ResourceLocation recipeId) {
        return containsRecipeId(grindRunnerRecipeDenylist(), recipeId);
    }

    boolean isThermalProcessRecipeDenied(ResourceLocation recipeId) {
        return containsRecipeId(thermalProcessRecipeDenylist(), recipeId);
    }

    void setRecipeDenylistsForGameTest(
            List<String> spellcasterWorkbenchRecipeDenylist,
            List<String> essenceSmokerRecipeDenylist,
            List<String> grindRunnerRecipeDenylist,
            List<String> thermalProcessRecipeDenylist
    ) {
        spellcasterWorkbenchRecipeDenylistOverride = List.copyOf(spellcasterWorkbenchRecipeDenylist);
        essenceSmokerRecipeDenylistOverride = List.copyOf(essenceSmokerRecipeDenylist);
        grindRunnerRecipeDenylistOverride = List.copyOf(grindRunnerRecipeDenylist);
        thermalProcessRecipeDenylistOverride = List.copyOf(thermalProcessRecipeDenylist);
    }

    List<String> spellcasterWorkbenchRecipeDenylist() {
        return Objects.requireNonNullElseGet(spellcasterWorkbenchRecipeDenylistOverride,
                () -> stringList(spellcasterWorkbenchRecipeDenylist));
    }

    List<String> essenceSmokerRecipeDenylist() {
        return Objects.requireNonNullElseGet(essenceSmokerRecipeDenylistOverride,
                () -> stringList(essenceSmokerRecipeDenylist));
    }

    List<String> grindRunnerRecipeDenylist() {
        return Objects.requireNonNullElseGet(grindRunnerRecipeDenylistOverride,
                () -> stringList(grindRunnerRecipeDenylist));
    }

    List<String> thermalProcessRecipeDenylist() {
        return Objects.requireNonNullElseGet(thermalProcessRecipeDenylistOverride,
                () -> stringList(thermalProcessRecipeDenylist));
    }

    private static boolean containsRecipeId(List<String> configuredIds, ResourceLocation recipeId) {
        for (var configuredId : configuredIds) {
            if (recipeId.equals(ResourceLocation.tryParse(String.valueOf(configuredId)))) {
                return true;
            }
        }
        return false;
    }

    private static List<String> stringList(ForgeConfigSpec.ConfigValue<List<? extends String>> configValue) {
        return configValue.get().stream()
                .map(String::valueOf)
                .toList();
    }

    private static boolean isRecipeId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }
}
