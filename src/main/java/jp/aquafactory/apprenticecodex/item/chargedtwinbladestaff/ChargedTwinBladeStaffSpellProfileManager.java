package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileManager;
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
public final class ChargedTwinBladeStaffSpellProfileManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "charged_twin_blade_staff_spell_profiles";

    private static final Gson GSON = new GsonBuilder().create();
    private static final ChargedTwinBladeStaffSpellProfileManager INSTANCE = new ChargedTwinBladeStaffSpellProfileManager();
    private static volatile Map<ResourceLocation, ChargedTwinBladeStaffSpellProfile> profiles = Map.of();

    private ChargedTwinBladeStaffSpellProfileManager() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    public static Optional<ChargedTwinBladeStaffSpellProfile> getProfile(AbstractSpell spell) {
        return spell == null ? Optional.empty() : Optional.ofNullable(profiles.get(spell.getSpellResource()));
    }

    public static boolean isSupportedByStaffOrFallback(AbstractSpell spell) {
        return getProfile(spell).isPresent() || SpellDispenserSpellProfileManager.getProfile(spell).isPresent();
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resourceMap,
            @NotNull ResourceManager resourceManager,
            @NotNull ProfilerFiller profiler
    ) {
        var resolvedProfiles = new LinkedHashMap<ResourceLocation, ChargedTwinBladeStaffSpellProfile>();

        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergeProfiles(entry.getKey(), entry.getValue(), resolvedProfiles));

        profiles = Map.copyOf(resolvedProfiles);
    }

    private static void mergeProfiles(
            ResourceLocation resourceId,
            JsonElement element,
            Map<ResourceLocation, ChargedTwinBladeStaffSpellProfile> resolvedProfiles
    ) {
        var parseResult = ChargedTwinBladeStaffSpellProfileList.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse Charged Twin Blade Staff spell profiles {}: {}", resourceId, message))
                .ifPresent(list -> {
                    for (var definition : list.values()) {
                        resolvedProfiles.put(definition.spell(), definition.profile());
                    }
                });
    }
}
