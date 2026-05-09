package jp.aquafactory.apprenticecodex.compat.epicfight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class EpicFightSwingMagicScheduleManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "epicfight_swing_magic_schedules";

    private static final Gson GSON = new GsonBuilder().create();
    private static final EpicFightSwingMagicScheduleManager INSTANCE = new EpicFightSwingMagicScheduleManager();
    private static volatile Map<ResourceLocation, Schedule> schedules = Map.of();

    private EpicFightSwingMagicScheduleManager() {
        super(GSON, DIRECTORY);
    }

    public static Schedule getSchedule(ResourceLocation animationId) {
        return animationId != null ? schedules.get(animationId) : null;
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        var resolvedSchedules = new ConcurrentHashMap<ResourceLocation, Schedule>();
        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> readSchedule(entry.getKey(), entry.getValue(), resolvedSchedules));
        schedules = Map.copyOf(resolvedSchedules);
    }

    private static void readSchedule(
            ResourceLocation resourceId,
            JsonElement element,
            Map<ResourceLocation, Schedule> resolvedSchedules
    ) {
        if (!element.isJsonObject()) {
            ApprenticeCodex.LOGGER.error("Failed to parse Epic Fight swing magic schedule {}: root must be an object", resourceId);
            return;
        }

        try {
            var root = element.getAsJsonObject();
            var animationId = ResourceLocation.tryParse(GsonHelper.getAsString(root, "animation"));
            if (animationId == null) {
                ApprenticeCodex.LOGGER.error("Failed to parse Epic Fight swing magic schedule {}: invalid animation id", resourceId);
                return;
            }

            var triggers = GsonHelper.getAsJsonArray(root, "triggers").asList().stream()
                    .map(trigger -> readTrigger(resourceId, trigger))
                    .toList();
            resolvedSchedules.put(animationId, new Schedule(triggers));
        } catch (RuntimeException e) {
            ApprenticeCodex.LOGGER.error("Failed to parse Epic Fight swing magic schedule {}", resourceId, e);
        }
    }

    private static Trigger readTrigger(ResourceLocation resourceId, JsonElement element) {
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("trigger must be an object in " + resourceId);
        }

        var trigger = element.getAsJsonObject();
        var time = GsonHelper.getAsFloat(trigger, "time");
        if (time < 0.0F) {
            throw new IllegalArgumentException("trigger time must be non-negative in " + resourceId);
        }

        return new Trigger(time, readHand(trigger));
    }

    private static InteractionHand readHand(JsonObject trigger) {
        var handName = GsonHelper.getAsString(trigger, "hand", "mainhand");
        return switch (handName) {
            case "mainhand", "main_hand", "main" -> InteractionHand.MAIN_HAND;
            case "offhand", "off_hand", "off" -> InteractionHand.OFF_HAND;
            default -> throw new IllegalArgumentException("unknown hand: " + handName);
        };
    }

    public record Schedule(List<Trigger> triggers) {
    }

    public record Trigger(float time, InteractionHand hand) {
    }
}
