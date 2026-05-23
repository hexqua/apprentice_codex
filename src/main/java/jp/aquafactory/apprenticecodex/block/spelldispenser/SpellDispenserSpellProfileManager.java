package jp.aquafactory.apprenticecodex.block.spelldispenser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpellDispenserSpellProfileManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "spell_dispenser_spell_profiles";

    private static final Gson GSON = new GsonBuilder().create();
    private static final SpellDispenserSpellProfileManager INSTANCE = new SpellDispenserSpellProfileManager();
    private static volatile Map<ResourceLocation, SpellDispenserSpellProfile> profiles = Map.of();

    private SpellDispenserSpellProfileManager() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    public static Optional<SpellDispenserSpellProfile> getProfile(AbstractSpell spell) {
        return spell == null ? Optional.empty() : Optional.ofNullable(profiles.get(spell.getSpellResource()));
    }

    public static SpellDispenserSpellProfile getResolvedProfile(AbstractSpell spell) {
        return getProfile(spell).orElse(SpellDispenserSpellProfile.DEFAULT);
    }

    public static SpellDispenserSpellProfile getResolvedProfile(SpellData spellData) {
        if (spellData == null || spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return SpellDispenserSpellProfile.DEFAULT;
        }
        return getResolvedProfile(spellData.getSpell());
    }

    public static boolean requiresOwner(SpellData spellData) {
        return getResolvedProfile(spellData).ownerRequired();
    }

    public static GameTestProfileOverride useProfilesForGameTest(Map<ResourceLocation, SpellDispenserSpellProfile> overrideProfiles) {
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
        var resolvedProfiles = new LinkedHashMap<ResourceLocation, SpellDispenserSpellProfile>();

        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergeProfiles(entry.getKey(), entry.getValue(), resolvedProfiles));

        profiles = Map.copyOf(resolvedProfiles);
    }

    private static void mergeProfiles(
            ResourceLocation resourceId,
            JsonElement element,
            Map<ResourceLocation, SpellDispenserSpellProfile> resolvedProfiles
    ) {
        var parseResult = SpellDispenserSpellProfileList.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse Spell Dispenser spell profiles {}: {}", resourceId, message))
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
