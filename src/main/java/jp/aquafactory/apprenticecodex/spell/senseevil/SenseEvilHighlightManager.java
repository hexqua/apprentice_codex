package jp.aquafactory.apprenticecodex.spell.senseevil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SenseEvilHighlightManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "sense_evil_highlights";

    private static final Gson GSON = new GsonBuilder().create();
    private static final SenseEvilHighlightManager INSTANCE = new SenseEvilHighlightManager();
    private static volatile Map<ResourceLocation, SenseEvilHighlightVariant> entityVariants = Map.of();

    private SenseEvilHighlightManager() {
        super(GSON, DIRECTORY);
    }

    public static SenseEvilHighlightVariant getConfiguredVariant(EntityType<?> entityType) {
        var entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        return entityVariants.get(entityId);
    }

    public static SenseEvilHighlightVariant getVariant(EntityType<?> entityType) {
        var configuredVariant = getConfiguredVariant(entityType);
        return configuredVariant != null ? configuredVariant : SenseEvilHighlightVariant.NORMAL;
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        var resolved = new LinkedHashMap<ResourceLocation, SenseEvilHighlightVariant>();

        // 複数ファイルに分けた datapack でも安定した結果になるよう、リソース ID 順でマージする。
        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergeEntityList(entry.getKey(), entry.getValue(), resolved));

        entityVariants = Map.copyOf(resolved);
    }

    private static void mergeEntityList(ResourceLocation resourceId, JsonElement element,
                                        Map<ResourceLocation, SenseEvilHighlightVariant> resolved) {
        var variant = SenseEvilHighlightVariant.fromDataFileName(resourceId.getPath());
        if (variant == null) {
            ApprenticeCodex.LOGGER.error("Unknown SenseEvil highlight variant file: {}", resourceId);
            return;
        }

        var parseResult = SenseEvilHighlightEntityList.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse SenseEvil highlight entity list {}: {}", resourceId, message))
                .ifPresent(entityList -> entityList.values().forEach(entityId -> {
                    var previous = resolved.put(entityId, variant);
                    if (previous != null && previous != variant) {
                        ApprenticeCodex.LOGGER.warn(
                                "SenseEvil highlight variant for {} overridden from {} to {} by {}",
                                entityId,
                                previous.getSerializedName(),
                                variant.getSerializedName(),
                                resourceId
                        );
                    }
                }));
    }
}
