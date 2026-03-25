package jp.aquafactory.apprenticecodex.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SchoolAffinityCatalystOverrideManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "school_affinity_catalysts";

    private static final Gson GSON = new GsonBuilder().create();
    private static final SchoolAffinityCatalystOverrideManager INSTANCE = new SchoolAffinityCatalystOverrideManager();

    private static volatile Map<ResourceLocation, ResourceLocation> resolvedOverrides = Map.of();
    private static volatile boolean hasDatapackEntries;

    private SchoolAffinityCatalystOverrideManager() {
        super(GSON, DIRECTORY);
    }

    public static Map<ResourceLocation, ResourceLocation> getResolvedOverrides() {
        return resolvedOverrides;
    }

    public static boolean hasDatapackEntries() {
        return hasDatapackEntries;
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        var mergedOverrides = new LinkedHashMap<ResourceLocation, ResourceLocation>();

        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergeOverrides(entry.getKey(), entry.getValue(), mergedOverrides));

        resolvedOverrides = Collections.unmodifiableMap(new LinkedHashMap<>(mergedOverrides));
        hasDatapackEntries = !resolvedOverrides.isEmpty();
        SchoolAffinityRegistry.invalidateBindings();
    }

    private static void mergeOverrides(
            ResourceLocation resourceId,
            JsonElement element,
            LinkedHashMap<ResourceLocation, ResourceLocation> mergedOverrides
    ) {
        var parseResult = SchoolAffinityCatalystOverrides.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse school affinity catalyst override {}: {}", resourceId, message))
                .ifPresent(overrides -> {
                    for (var entry : overrides.overrides()) {
                        var previous = mergedOverrides.put(entry.school(), entry.item());
                        if (previous != null && !previous.equals(entry.item())) {
                            ApprenticeCodex.LOGGER.warn(
                                    "School Affinity catalyst override for {} was overwritten from {} to {} by {}.",
                                    entry.school(),
                                    previous,
                                    entry.item(),
                                    resourceId
                            );
                        }
                    }
                });
    }
}
