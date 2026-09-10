package jp.aquafactory.apprenticecodex.item.elementalbow;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ElementalBowModeManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "elemental_bow_modes";
    private static final ElementalBowModeManager INSTANCE = new ElementalBowModeManager();
    private static volatile Map<ResourceLocation, ResolvedDefinition> definitions = Map.of();

    private ElementalBowModeManager() {
        super(new Gson(), DIRECTORY);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    public static List<ResolvedDefinition> getResolvedDefinitions() {
        return List.copyOf(definitions.values());
    }

    @Nullable
    public static ResolvedDefinition getResolvedDefinition(@Nullable ResourceLocation spellId) {
        return spellId == null ? null : definitions.get(spellId);
    }

    public static boolean isElementalSpell(@Nullable AbstractSpell spell) {
        return spell != null && spell.isEnabled() && definitions.containsKey(spell.getSpellResource());
    }

    public static List<ElementalBowModeDefinition> createSnapshot() {
        return definitions.values().stream().map(d -> new ElementalBowModeDefinition(d.spellId(), d.requiredDrawTicks())).toList();
    }

    public static void applySnapshot(List<ElementalBowModeDefinition> snapshot) {
        var resolved = new LinkedHashMap<ResourceLocation, ResolvedDefinition>();
        for (var definition : snapshot) {
            var spell = SpellRegistry.getSpell(definition.spell());
            if (spell != null && spell != SpellRegistry.none()) {
                resolved.put(definition.spell(), new ResolvedDefinition(definition.spell(), spell, definition.requiredDrawTicks()));
            }
        }
        definitions = java.util.Collections.unmodifiableMap(resolved);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, @NotNull ResourceManager manager,
                         @NotNull ProfilerFiller profiler) {
        var resolved = new LinkedHashMap<ResourceLocation, ResolvedDefinition>();
        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> ElementalBowModeList.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .resultOrPartial(message -> ApprenticeCodex.LOGGER.error(
                                "Failed to parse Elemental Bow mode list {}: {}", entry.getKey(), message))
                        .ifPresent(list -> {
                            for (var definition : list.values()) {
                                var spell = SpellRegistry.getSpell(definition.spell());
                                if (spell == null || spell == SpellRegistry.none()) continue;
                                resolved.put(definition.spell(), new ResolvedDefinition(
                                        definition.spell(),
                                        spell, definition.requiredDrawTicks()));
                            }
                        }));
        definitions = java.util.Collections.unmodifiableMap(resolved);
    }

    public record ResolvedDefinition(ResourceLocation spellId, AbstractSpell spell, int requiredDrawTicks) {
        // Iron's の設定は reload listener より後で確定するため、school を早期に固定しない。
        public SchoolType schoolType() { return spell.getSchoolType(); }
        public ResourceLocation schoolId() { return schoolType().getId(); }
        public int resolveSpellLevel(ItemStack stack, int scrollLevel) {
            // 表示と射撃で同じ値を使い、汎用イベントによる二重加算を避ける。
            int bonus = jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                    stack, jp.aquafactory.apprenticecodex.enchantment.Enchantments.TRANSCENDENCE);
            return net.minecraft.util.Mth.clamp(scrollLevel + bonus, spell.getMinLevel(), spell.getMaxLevel());
        }

        public int resolveRequiredDrawTicks() {
            double ticks = Math.ceil(requiredDrawTicks * ApprenticeCodexServerConfig.elementalBowMagicReadyDrawTicksMultiplier());
            return (int) Math.min(Math.max(0, ticks), Integer.MAX_VALUE);
        }

        public int color() {
            return SchoolAffinityRegistry.resolveColor(schoolType());
        }
    }
}
