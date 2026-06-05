package jp.aquafactory.apprenticecodex.remoteownercast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
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
public final class RemoteOwnerCastProfileManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "remote_owner_cast_spell_profiles";

    private static final Gson GSON = new GsonBuilder().create();
    private static final RemoteOwnerCastProfileManager INSTANCE = new RemoteOwnerCastProfileManager();
    private static volatile Map<ResourceLocation, RemoteOwnerCastProfile> profiles = Map.of();

    private RemoteOwnerCastProfileManager() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    public static Optional<RemoteOwnerCastProfile> getProfile(AbstractSpell spell) {
        return spell == null ? Optional.empty() : Optional.ofNullable(profiles.get(spell.getSpellResource()));
    }

    public static Optional<RemoteOwnerCastProfile> getUsableProfile(AbstractSpell spell, RemoteOwnerCastOrigin origin) {
        if (spell == null || spell.getSpellResource() == null) {
            return Optional.empty();
        }
        if (ApprenticeCodexServerConfig.isRemoteOwnerCastSpellDenied(spell.getSpellResource())) {
            return Optional.empty();
        }
        return getProfile(spell)
                .filter(profile -> profile.allowsOrigin(origin));
    }

    public static boolean isSupportedByRemoteOwnerCast(AbstractSpell spell, RemoteOwnerCastOrigin origin) {
        if (spell == null || spell.getSpellResource() == null) {
            return false;
        }
        return getProfile(spell)
                .filter(profile -> profile.allowsOrigin(origin))
                .isPresent();
    }

    public static GameTestProfileOverride useProfilesForGameTest(Map<ResourceLocation, RemoteOwnerCastProfile> overrideProfiles) {
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
        var resolvedProfiles = new LinkedHashMap<ResourceLocation, RemoteOwnerCastProfile>();

        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergeProfiles(entry.getKey(), entry.getValue(), resolvedProfiles));

        profiles = Map.copyOf(resolvedProfiles);
    }

    private static void mergeProfiles(
            ResourceLocation resourceId,
            JsonElement element,
            Map<ResourceLocation, RemoteOwnerCastProfile> resolvedProfiles
    ) {
        var parseResult = RemoteOwnerCastProfileList.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse Remote Owner Cast spell profiles {}: {}", resourceId, message))
                .ifPresent(list -> {
                    for (var definition : list.values()) {
                        resolvedProfiles.put(definition.spell(), definition.profile());
                    }
                });
    }

    public interface GameTestProfileOverride extends AutoCloseable {
        @Override
        void close();
    }
}
