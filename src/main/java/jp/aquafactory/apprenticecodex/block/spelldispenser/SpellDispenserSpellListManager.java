package jp.aquafactory.apprenticecodex.block.spelldispenser;

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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpellDispenserSpellListManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "spell_dispenser_spell_lists";

    private static final Gson GSON = new GsonBuilder().create();
    private static final SpellDispenserSpellListManager INSTANCE = new SpellDispenserSpellListManager();
    private static volatile Set<ResourceLocation> denylist = Set.of();

    private SpellDispenserSpellListManager() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    public static boolean isDenylisted(AbstractSpell spell) {
        return spell != null && denylist.contains(spell.getSpellResource());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        var resolvedDenylist = new LinkedHashSet<ResourceLocation>();

        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergeList(entry.getKey(), entry.getValue(), resolvedDenylist));

        denylist = Set.copyOf(resolvedDenylist);
    }

    private static void mergeList(
            ResourceLocation resourceId,
            JsonElement element,
            Set<ResourceLocation> resolvedDenylist
    ) {
        if (isObsoleteAllowlist(resourceId)) {
            ApprenticeCodex.LOGGER.warn("Ignoring obsolete Spell Dispenser allowlist: {}", resourceId);
            return;
        }

        var target = resolveTargetSet(resourceId, resolvedDenylist);
        if (target == null) {
            ApprenticeCodex.LOGGER.error("Unknown Spell Dispenser spell list file: {}", resourceId);
            return;
        }

        var parseResult = SpellDispenserSpellList.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse Spell Dispenser spell list {}: {}", resourceId, message))
                .ifPresent(list -> target.addAll(list.values()));
    }

    private static Set<ResourceLocation> resolveTargetSet(
            ResourceLocation resourceId,
            Set<ResourceLocation> resolvedDenylist
    ) {
        var path = resourceId.getPath();
        var fileName = path.substring(path.lastIndexOf('/') + 1);
        if (fileName.equals("denylist")) {
            return resolvedDenylist;
        }
        return null;
    }

    private static boolean isObsoleteAllowlist(ResourceLocation resourceId) {
        var path = resourceId.getPath();
        var fileName = path.substring(path.lastIndexOf('/') + 1);
        return fileName.equals("allowlist");
    }
}
