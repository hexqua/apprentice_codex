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
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MulticastEchoStaffAttackProfileManager extends SimpleJsonResourceReloadListener {
    public interface GameTestProfileOverride extends AutoCloseable {
        @Override
        void close();
    }

    public static final String DIRECTORY = "multicast_echo_staff_attack_profiles";

    private static final Gson GSON = new GsonBuilder().create();
    private static final MulticastEchoStaffAttackProfileManager INSTANCE = new MulticastEchoStaffAttackProfileManager();
    private static volatile Map<ResourceLocation, MulticastEchoStaffAttackProfile> profiles = Map.of();
    private static volatile Set<ResourceLocation> clientSyncedProfileSpellIds = Set.of();

    public static List<ResourceLocation> createProfileSpellIdSnapshot() {
        return profiles.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    public static void applyClientSyncedProfileSpellIds(Collection<ResourceLocation> ids) {
        clientSyncedProfileSpellIds = Set.copyOf(ids);
    }

    public static void clearClientSyncedProfileSpellIds() {
        clientSyncedProfileSpellIds = Set.of();
    }

    public static boolean hasClientSyncedProfile(AbstractSpell spell) {
        // 統合サーバーでも同期済み集合だけを使い、別サーバーの情報を表示しない。
        return spell != null && clientSyncedProfileSpellIds.contains(spell.getSpellResource());
    }

    private MulticastEchoStaffAttackProfileManager() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    public static Optional<MulticastEchoStaffAttackProfile> getProfile(AbstractSpell spell) {
        return spell == null ? Optional.empty() : Optional.ofNullable(profiles.get(spell.getSpellResource()));
    }

    public static GameTestProfileOverride useProfilesForGameTest(
            Map<ResourceLocation, MulticastEchoStaffAttackProfile> overrideProfiles
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
        var resolvedProfiles = new LinkedHashMap<ResourceLocation, MulticastEchoStaffAttackProfile>();

        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergeProfiles(entry.getKey(), entry.getValue(), resolvedProfiles));

        profiles = Map.copyOf(resolvedProfiles);
    }

    private static void mergeProfiles(
            ResourceLocation resourceId,
            JsonElement element,
            Map<ResourceLocation, MulticastEchoStaffAttackProfile> resolvedProfiles
    ) {
        var parseResult = MulticastEchoStaffAttackProfileList.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse Multicast Echo Staff attack profiles {}: {}", resourceId, message))
                .ifPresent(list -> {
                    for (var definition : list.values()) {
                        resolvedProfiles.put(definition.spell(), definition.profile());
                    }
                });
    }
}
