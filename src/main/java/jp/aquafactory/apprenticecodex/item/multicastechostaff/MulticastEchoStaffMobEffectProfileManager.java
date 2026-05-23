package jp.aquafactory.apprenticecodex.item.multicastechostaff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MulticastEchoStaffMobEffectProfileManager extends SimpleJsonResourceReloadListener {
    public interface GameTestProfileOverride extends AutoCloseable {
        @Override
        void close();
    }

    public static final String DIRECTORY = "multicast_echo_staff_mob_effect_profiles";

    private static final Gson GSON = new GsonBuilder().create();
    private static final MulticastEchoStaffMobEffectProfileManager INSTANCE = new MulticastEchoStaffMobEffectProfileManager();
    private static volatile Map<ResourceLocation, MulticastEchoStaffMobEffectProfile> profiles = Map.of();

    private MulticastEchoStaffMobEffectProfileManager() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    public static Optional<MulticastEchoStaffMobEffectProfile> getProfile(AbstractSpell spell) {
        return spell == null ? Optional.empty() : Optional.ofNullable(profiles.get(spell.getSpellResource()));
    }

    public static GameTestProfileOverride useProfilesForGameTest(
            Map<ResourceLocation, MulticastEchoStaffMobEffectProfile> overrideProfiles
    ) {
        var previousProfiles = profiles;
        profiles = Map.copyOf(overrideProfiles);
        return () -> profiles = previousProfiles;
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resourceMap,
            @NotNull ResourceManager resourceManager,
            @NotNull ProfilerFiller profiler
    ) {
        var resolvedProfiles = new LinkedHashMap<ResourceLocation, MulticastEchoStaffMobEffectProfile>();

        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergeProfiles(entry.getKey(), entry.getValue(), resolvedProfiles));

        profiles = Map.copyOf(resolvedProfiles);
    }

    private static void mergeProfiles(
            ResourceLocation resourceId,
            JsonElement element,
            Map<ResourceLocation, MulticastEchoStaffMobEffectProfile> resolvedProfiles
    ) {
        var parseResult = MulticastEchoStaffMobEffectProfileList.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse Multicast Echo Staff mob effect profiles {}: {}", resourceId, message))
                .ifPresent(list -> {
                    for (var definition : list.values()) {
                        resolvedProfiles.put(definition.spell(), definition.profile());
                    }
                });
    }
}
