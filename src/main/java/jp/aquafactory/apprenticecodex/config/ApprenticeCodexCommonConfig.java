package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ApprenticeCodexCommonConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SCHOOL_AFFINITY_PRIORITY;
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SCHOOL_AFFINITY_DENY;

    static {
        var builder = new ModConfigSpec.Builder();
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
