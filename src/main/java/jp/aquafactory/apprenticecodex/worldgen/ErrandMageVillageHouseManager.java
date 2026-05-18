package jp.aquafactory.apprenticecodex.worldgen;

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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ErrandMageVillageHouseManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "errand_mage_village_houses";

    private static final Gson GSON = new GsonBuilder().create();
    private static final ErrandMageVillageHouseManager INSTANCE = new ErrandMageVillageHouseManager();
    private static volatile List<ErrandMageVillageHouseDefinition> definitions = List.of();

    private ErrandMageVillageHouseManager() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    public static List<ErrandMageVillageHouseDefinition> definitions() {
        return definitions;
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resourceMap,
            @NotNull ResourceManager resourceManager,
            @NotNull ProfilerFiller profiler
    ) {
        var resolvedDefinitions = new ArrayList<ErrandMageVillageHouseDefinition>();

        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergeDefinitions(entry.getKey(), entry.getValue(), resolvedDefinitions));

        definitions = List.copyOf(resolvedDefinitions);
    }

    private static void mergeDefinitions(
            ResourceLocation resourceId,
            JsonElement element,
            List<ErrandMageVillageHouseDefinition> resolvedDefinitions
    ) {
        var parseResult = ErrandMageVillageHouseList.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse Errand Mage village house definitions {}: {}", resourceId, message))
                .ifPresent(list -> resolvedDefinitions.addAll(list.values()));
    }
}
