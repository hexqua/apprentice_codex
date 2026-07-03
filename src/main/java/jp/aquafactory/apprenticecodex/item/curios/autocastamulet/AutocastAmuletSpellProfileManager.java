package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AutocastAmuletSpellProfileManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "autocast_amulet_spell_profiles";

    private static final Gson GSON = new GsonBuilder().create();
    private static final AutocastAmuletSpellProfileManager INSTANCE = new AutocastAmuletSpellProfileManager();
    private static volatile Map<ResourceLocation, AutocastAmuletSpellProfile> profiles = Map.of();

    private AutocastAmuletSpellProfileManager() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    public static Optional<AutocastAmuletSpellProfile> getProfile(AbstractSpell spell) {
        return spell == null ? Optional.empty() : Optional.ofNullable(profiles.get(spell.getSpellResource()));
    }

    public static boolean canCastWithWisdomShard(ServerPlayer player, SpellData spellData) {
        if (spellData == null || spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return false;
        }

        return getProfile(spellData.getSpell())
                .map(profile -> profile.matches(player))
                .orElse(false);
    }

    public static GameTestProfileOverride useProfilesForGameTest(
            Map<ResourceLocation, AutocastAmuletSpellProfile> overrideProfiles
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
        var resolvedProfiles = new LinkedHashMap<ResourceLocation, AutocastAmuletSpellProfile>();

        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergeProfiles(entry.getKey(), entry.getValue(), resolvedProfiles));

        profiles = Map.copyOf(resolvedProfiles);
    }

    private static void mergeProfiles(
            ResourceLocation resourceId,
            JsonElement element,
            Map<ResourceLocation, AutocastAmuletSpellProfile> resolvedProfiles
    ) {
        var parseResult = AutocastAmuletSpellProfileList.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse Autocast Amulet spell profiles {}: {}", resourceId, message))
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
