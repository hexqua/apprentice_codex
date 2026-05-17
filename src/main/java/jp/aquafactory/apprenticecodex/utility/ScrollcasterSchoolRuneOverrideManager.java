package jp.aquafactory.apprenticecodex.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ScrollcasterSchoolRuneOverrideManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "scrollcaster_gauntlet_school_runes";

    private static final Gson GSON = new GsonBuilder().create();
    private static final ScrollcasterSchoolRuneOverrideManager INSTANCE = new ScrollcasterSchoolRuneOverrideManager();

    private static volatile Map<ResourceLocation, ResourceLocation> resolvedOverrides = Map.of();

    private ScrollcasterSchoolRuneOverrideManager() {
        super(GSON, DIRECTORY);
    }

    public static Map<ResourceLocation, ResourceLocation> getResolvedOverrides() {
        return resolvedOverrides;
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        var mergedOverrides = new LinkedHashMap<ResourceLocation, ResourceLocation>();

        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergeOverrides(entry.getKey(), entry.getValue(), mergedOverrides));

        resolvedOverrides = Collections.unmodifiableMap(new LinkedHashMap<>(mergedOverrides));
    }

    private static void mergeOverrides(
            ResourceLocation resourceId,
            JsonElement element,
            LinkedHashMap<ResourceLocation, ResourceLocation> mergedOverrides
    ) {
        var parseResult = ScrollcasterSchoolRuneOverrides.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse Scrollcaster Gauntlet school rune override {}: {}", resourceId, message))
                .ifPresent(overrides -> {
                    for (var entry : overrides.overrides()) {
                        if (!isResolvableOverride(resourceId, entry)) {
                            continue;
                        }

                        var previous = mergedOverrides.put(entry.item(), entry.school());
                        if (previous != null && !previous.equals(entry.school())) {
                            ApprenticeCodex.LOGGER.warn(
                                    "Scrollcaster Gauntlet school rune override for {} was overwritten from {} to {} by {}.",
                                    entry.item(),
                                    previous,
                                    entry.school(),
                                    resourceId
                            );
                        }
                    }
                });
    }

    private static boolean isResolvableOverride(ResourceLocation resourceId, ScrollcasterSchoolRuneOverrides.Entry entry) {
        var item = BuiltInRegistries.ITEM.get(entry.item());
        if (item == null || item == Items.AIR) {
            ApprenticeCodex.LOGGER.warn(
                    "Scrollcaster Gauntlet school rune override {} skipped missing item {}.",
                    resourceId,
                    entry.item()
            );
            return false;
        }

        if (SchoolRegistry.getSchool(entry.school()) == null) {
            ApprenticeCodex.LOGGER.warn(
                    "Scrollcaster Gauntlet school rune override {} skipped missing school {} for {}.",
                    resourceId,
                    entry.school(),
                    entry.item()
            );
            return false;
        }

        return true;
    }
}
