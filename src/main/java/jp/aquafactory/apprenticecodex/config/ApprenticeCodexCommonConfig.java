package jp.aquafactory.apprenticecodex.config;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ApprenticeCodexCommonConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue DISABLE_ARCANUM_IN_A_JAR_RECIPE;
    private static final ForgeConfigSpec.BooleanValue DISABLE_EXPLORERS_CODEX_RECIPE;
    private static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> SCHOOL_AFFINITY_PRIORITY;
    private static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> SCHOOL_AFFINITY_DENY;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("Items");
        DISABLE_ARCANUM_IN_A_JAR_RECIPE = builder.define("disableArcanumInAJarRecipe", false);
        DISABLE_EXPLORERS_CODEX_RECIPE = builder.define("disableExplorersCodexRecipe", false);
        builder.pop();
        builder.comment(
                        "Entries for schoolAffinityPriority and schoolAffinityDeny use \"modid:school_id\".",
                        "If non-empty files exist under \"data/" + jp.aquafactory.apprenticecodex.ApprenticeCodex.MODID + "/school_affinity_policies/*.json\", those files take precedence."
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

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        invalidateSchoolAffinityBindings(event);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
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
