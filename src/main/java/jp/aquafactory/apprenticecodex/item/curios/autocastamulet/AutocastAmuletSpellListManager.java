package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

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
public final class AutocastAmuletSpellListManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "autocast_amulet_spell_lists";

    private static final Gson GSON = new GsonBuilder().create();
    private static final AutocastAmuletSpellListManager INSTANCE = new AutocastAmuletSpellListManager();
    private static volatile Set<ResourceLocation> allowlist = Set.of();

    private AutocastAmuletSpellListManager() {
        super(GSON, DIRECTORY);
    }

    public static boolean isAllowlisted(AbstractSpell spell) {
        return spell != null && allowlist.contains(spell.getSpellResource());
    }

    public static Set<ResourceLocation> getAllowlist() {
        return allowlist;
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        var resolvedAllowlist = new LinkedHashSet<ResourceLocation>();

        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergeList(entry.getKey(), entry.getValue(), resolvedAllowlist));

        allowlist = Set.copyOf(resolvedAllowlist);
    }

    private static void mergeList(
            ResourceLocation resourceId,
            JsonElement element,
            Set<ResourceLocation> resolvedAllowlist
    ) {
        var target = resolveTargetSet(resourceId, resolvedAllowlist);
        if (target == null) {
            ApprenticeCodex.LOGGER.error("Unknown Autocast Amulet spell list file: {}", resourceId);
            return;
        }

        var parseResult = AutocastAmuletSpellList.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse Autocast Amulet spell list {}: {}", resourceId, message))
                .ifPresent(list -> target.addAll(list.values()));
    }

    private static Set<ResourceLocation> resolveTargetSet(
            ResourceLocation resourceId,
            Set<ResourceLocation> resolvedAllowlist
    ) {
        var path = resourceId.getPath();
        var fileName = path.substring(path.lastIndexOf('/') + 1);
        if (fileName.equals("allowlist")) {
            return resolvedAllowlist;
        }
        return null;
    }
}
