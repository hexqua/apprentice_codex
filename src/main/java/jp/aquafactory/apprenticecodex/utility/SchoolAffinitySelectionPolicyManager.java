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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SchoolAffinitySelectionPolicyManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "school_affinity_policies";

    private static final Gson GSON = new GsonBuilder().create();
    private static final SchoolAffinitySelectionPolicyManager INSTANCE = new SchoolAffinitySelectionPolicyManager();

    private static volatile SchoolAffinitySelectionPolicy resolvedPolicy = SchoolAffinitySelectionPolicy.EMPTY;
    private static volatile boolean hasDatapackEntries;

    private SchoolAffinitySelectionPolicyManager() {
        super(GSON, DIRECTORY);
    }

    public static SchoolAffinitySelectionPolicy getResolvedPolicy() {
        return resolvedPolicy;
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
        var resolvedPriorities = new LinkedHashSet<ResourceLocation>();
        var resolvedDeny = new LinkedHashSet<ResourceLocation>();

        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergePolicy(entry.getKey(), entry.getValue(), resolvedPriorities, resolvedDeny));

        hasDatapackEntries = !resourceMap.isEmpty();
        resolvedPolicy = new SchoolAffinitySelectionPolicy(
                resolvedPriorities.stream().toList(),
                resolvedDeny.stream().toList()
        );
        SchoolAffinityRegistry.invalidateBindings();
    }

    private static void mergePolicy(
            ResourceLocation resourceId,
            JsonElement element,
            LinkedHashSet<ResourceLocation> resolvedPriorities,
            LinkedHashSet<ResourceLocation> resolvedDeny
    ) {
        var parseResult = SchoolAffinitySelectionPolicy.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse school affinity policy {}: {}", resourceId, message))
                .ifPresent(policy -> {
                    resolvedPriorities.addAll(policy.priorities());
                    resolvedDeny.addAll(policy.deny());
                });
    }
}
