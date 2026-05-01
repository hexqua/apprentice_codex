package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ApprenticeCodexCommonConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue DISABLE_ARCANUM_IN_A_JAR_RECIPE;
    private static final ModConfigSpec.BooleanValue DISABLE_EXPLORERS_CODEX_RECIPE;
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SCHOOL_AFFINITY_PRIORITY;
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SCHOOL_AFFINITY_DENY;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("Items");
        DISABLE_ARCANUM_IN_A_JAR_RECIPE = builder.define("disableArcanumInAJarRecipe", false);
        DISABLE_EXPLORERS_CODEX_RECIPE = builder.define("disableExplorersCodexRecipe", false);
        builder.pop();
        builder.comment(
                        "Entries for schoolAffinityPriority and schoolAffinityDeny use \"modid:school_id\".",
                        "If non-empty files exist under \"data/" + ApprenticeCodex.MODID + "/school_affinity_policies/*.json\", those files take precedence."
                )
                .push("SchoolAffinity");
        SCHOOL_AFFINITY_PRIORITY = builder.defineList("schoolAffinityPriority", java.util.List.<String>of(),
                value -> value instanceof String text && !text.isBlank());
        SCHOOL_AFFINITY_DENY = builder.defineList("schoolAffinityDeny", java.util.List.<String>of(),
                value -> value instanceof String text && !text.isBlank());
        builder.pop();
        SPEC = builder.build();
    }

    private ApprenticeCodexCommonConfig() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ApprenticeCodexCommonConfig::onConfigLoading);
        modEventBus.addListener(ApprenticeCodexCommonConfig::onConfigReloading);
    }

    public static boolean disableArcanumInAJarRecipe() {
        return DISABLE_ARCANUM_IN_A_JAR_RECIPE.get();
    }

    public static boolean disableExplorersCodexRecipe() {
        return DISABLE_EXPLORERS_CODEX_RECIPE.get();
    }

    public static java.util.List<String> schoolAffinityPriority() {
        return SCHOOL_AFFINITY_PRIORITY.get().stream()
                .map(String::valueOf)
                .toList();
    }

    public static java.util.List<String> schoolAffinityDeny() {
        return SCHOOL_AFFINITY_DENY.get().stream()
                .map(String::valueOf)
                .toList();
    }

    public static void onConfigLoading(ModConfigEvent.Loading event) {
        invalidateSchoolAffinityBindings(event);
    }

    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        invalidateSchoolAffinityBindings(event);
    }

    private static void invalidateSchoolAffinityBindings(ModConfigEvent event) {
        if (event.getConfig().getType() != ModConfig.Type.COMMON) {
            return;
        }
        if (!ApprenticeCodex.MODID.equals(event.getConfig().getModId())) {
            return;
        }

        SchoolAffinityRegistry.invalidateBindings();
    }
}
