package jp.aquafactory.apprenticecodex.item.spellgun;

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

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpellGunSpellListManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "spell_gun_spell_lists";

    private static final Gson GSON = new GsonBuilder().create();
    private static final SpellGunSpellListManager INSTANCE = new SpellGunSpellListManager();
    private static volatile Set<ResourceLocation> denylist = Set.of();

    private SpellGunSpellListManager() {
        super(GSON, DIRECTORY);
    }

    public static boolean isDenylisted(AbstractSpell spell) {
        return spell != null && denylist.contains(spell.getSpellResource());
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        var resolvedDenylist = new LinkedHashSet<ResourceLocation>();

        // datapack の重ね掛け順で結果がぶれないよう、resource id 順で処理する。
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
        if (isObsoleteLongAllowlist(resourceId)) {
            ApprenticeCodex.LOGGER.warn("Ignoring obsolete spell gun long_allowlist: {}", resourceId);
            return;
        }

        var target = resolveTargetSet(resourceId, resolvedDenylist);
        if (target == null) {
            ApprenticeCodex.LOGGER.error("Unknown spell gun spell list file: {}", resourceId);
            return;
        }

        var parseResult = SpellGunSpellList.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse spell gun spell list {}: {}", resourceId, message))
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

    private static boolean isObsoleteLongAllowlist(ResourceLocation resourceId) {
        var path = resourceId.getPath();
        var fileName = path.substring(path.lastIndexOf('/') + 1);
        return fileName.equals("long_allowlist");
    }
}
