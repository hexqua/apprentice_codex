package jp.aquafactory.apprenticecodex.spell.searchbeacon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SearchBeaconTargetManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "search_beacon_targets";
    private static final int HINT_TARGET_LABEL_MAX_LENGTH = 32;

    private static final Gson GSON = new GsonBuilder().create();
    private static final SearchBeaconTargetManager INSTANCE = new SearchBeaconTargetManager();
    private static volatile Map<ResourceLocation, SearchBeaconTargetList.Definition> definitionsByItemId = Map.of();

    private SearchBeaconTargetManager() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    public static @Nullable SearchBeaconTargetList.Definition getDefinition(ItemStack stack) {
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null ? definitionsByItemId.get(itemId) : null;
    }

    public static String createDisplayLabel(SearchBeaconTargetList.Definition definition) {
        return definition.targets().stream()
                .map(SearchBeaconTargetList.TargetReference::toDisplayString)
                .collect(Collectors.joining(", "));
    }

    public static @Nullable HintCandidate getRandomHintCandidate(RandomSource random) {
        var candidates = definitionsByItemId.values().stream()
                .map(SearchBeaconTargetManager::createHintCandidate)
                .filter(Objects::nonNull)
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private static @Nullable HintCandidate createHintCandidate(SearchBeaconTargetList.Definition definition) {
        var item = ForgeRegistries.ITEMS.getValue(definition.item());
        if (item == null) {
            return null;
        }

        var itemStack = new ItemStack(item);
        if (itemStack.isEmpty()) {
            return null;
        }

        return definition.targets().stream()
                .findFirst()
                .map(SearchBeaconTargetManager::createHintTargetLabel)
                .map(targetLabel -> new HintCandidate(itemStack, targetLabel))
                .orElse(null);
    }

    private static String createHintTargetLabel(SearchBeaconTargetList.TargetReference target) {
        var display = target.toDisplayString();
        if (display.length() <= HINT_TARGET_LABEL_MAX_LENGTH) {
            return display;
        }
        return display.substring(0, HINT_TARGET_LABEL_MAX_LENGTH - 3) + "...";
    }

    public static List<Holder<Structure>> resolveTargets(ServerLevel level, SearchBeaconTargetList.Definition definition) {
        var structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var resolved = new LinkedHashMap<ResourceLocation, Holder<Structure>>();

        for (var reference : definition.targets()) {
            if (reference.tag()) {
                var tagKey = TagKey.create(Registries.STRUCTURE, reference.id());
                structureRegistry.getTag(tagKey).ifPresent(holders ->
                        holders.forEach(holder -> {
                            var id = structureRegistry.getKey(holder.value());
                            if (id != null) {
                                resolved.putIfAbsent(id, holder);
                            }
                        })
                );
                continue;
            }

            var resourceKey = ResourceKey.create(Registries.STRUCTURE, reference.id());
            structureRegistry.getHolder(resourceKey).ifPresent(holder -> resolved.putIfAbsent(reference.id(), holder));
        }

        return List.copyOf(resolved.values());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        var resolved = new LinkedHashMap<ResourceLocation, SearchBeaconTargetList.Definition>();

        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergeDefinitions(entry.getKey(), entry.getValue(), resolved));

        definitionsByItemId = Map.copyOf(resolved);
    }

    private static void mergeDefinitions(
            ResourceLocation resourceId,
            JsonElement element,
            Map<ResourceLocation, SearchBeaconTargetList.Definition> resolved
    ) {
        var parseResult = SearchBeaconTargetList.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse SearchBeacon targets {}: {}", resourceId, message))
                .ifPresent(list -> list.values().forEach(definition -> {
                    if (definition.targets().isEmpty()) {
                        ApprenticeCodex.LOGGER.warn("Skipping SearchBeacon target entry with no structures: {} in {}", definition.item(), resourceId);
                        return;
                    }

                    var previous = resolved.put(definition.item(), definition);
                    if (previous != null) {
                        ApprenticeCodex.LOGGER.warn(
                                "SearchBeacon target for {} overridden by {}",
                                definition.item(),
                                resourceId
                        );
                    }
                }));
    }

    public record HintCandidate(ItemStack itemStack, String targetLabel) {
        public HintCandidate {
            itemStack = itemStack.copy();
        }
    }
}
