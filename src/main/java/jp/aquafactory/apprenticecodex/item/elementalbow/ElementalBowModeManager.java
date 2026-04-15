package jp.aquafactory.apprenticecodex.item.elementalbow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ElementalBowModeManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "elemental_bow_modes";

    private static final Gson GSON = new GsonBuilder().create();
    private static final ElementalBowModeManager INSTANCE = new ElementalBowModeManager();

    private static volatile List<ResolvedDefinition> resolvedDefinitions = List.of();
    private static volatile Map<ResourceLocation, ResolvedDefinition> resolvedDefinitionsBySchool = Map.of();
    private static volatile Set<ResourceLocation> spellIds = Set.of();

    private ElementalBowModeManager() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    public static List<ResolvedDefinition> getResolvedDefinitions() {
        return resolvedDefinitions;
    }

    @Nullable
    public static ResolvedDefinition getResolvedDefinition(@Nullable ResourceLocation schoolId) {
        return schoolId == null ? null : resolvedDefinitionsBySchool.get(schoolId);
    }

    public static boolean isElementalSpell(@Nullable AbstractSpell spell) {
        return spell != null && spellIds.contains(spell.getSpellResource());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        var resolvedBySchool = new LinkedHashMap<ResourceLocation, ResolvedDefinition>();
        var resolvedSpellIds = new java.util.LinkedHashSet<ResourceLocation>();

        resourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> mergeDefinitions(entry.getKey(), entry.getValue(), resolvedBySchool, resolvedSpellIds));

        resolvedDefinitions = List.copyOf(resolvedBySchool.values());
        resolvedDefinitionsBySchool = Map.copyOf(resolvedBySchool);
        spellIds = Set.copyOf(resolvedSpellIds);
    }

    private static void mergeDefinitions(
            ResourceLocation resourceId,
            JsonElement element,
            Map<ResourceLocation, ResolvedDefinition> resolvedBySchool,
            Set<ResourceLocation> resolvedSpellIds
    ) {
        var parseResult = ElementalBowModeList.CODEC.parse(JsonOps.INSTANCE, element);
        parseResult.resultOrPartial(message ->
                        ApprenticeCodex.LOGGER.error("Failed to parse Elemental Bow mode list {}: {}", resourceId, message))
                .ifPresent(list -> {
                    for (var definition : list.values()) {
                        mergeDefinition(resourceId, definition, resolvedBySchool, resolvedSpellIds);
                    }
                });
    }

    private static void mergeDefinition(
            ResourceLocation resourceId,
            ElementalBowModeDefinition definition,
            Map<ResourceLocation, ResolvedDefinition> resolvedBySchool,
            Set<ResourceLocation> resolvedSpellIds
    ) {
        var schoolId = definition.school();
        if (resolvedBySchool.containsKey(schoolId)) {
            ApprenticeCodex.LOGGER.warn(
                    "Elemental Bow mode {} in {} was ignored because school {} is already defined.",
                    definition.spell(),
                    resourceId,
                    schoolId
            );
            return;
        }

        var schoolType = SchoolRegistry.getSchool(schoolId);
        if (schoolType == null) {
            ApprenticeCodex.LOGGER.warn("Elemental Bow mode {} in {} was ignored because school {} could not be resolved.", definition.spell(), resourceId, schoolId);
            return;
        }

        var spell = SpellRegistry.getSpell(definition.spell());
        if (spell == null) {
            ApprenticeCodex.LOGGER.warn("Elemental Bow mode {} in {} was ignored because spell {} could not be resolved.", schoolId, resourceId, definition.spell());
            return;
        }

        var resolvedBonuses = resolveBonuses(resourceId, definition);
        if (resolvedBonuses == null) {
            return;
        }

        var resolvedDefinition = new ResolvedDefinition(
                schoolId,
                schoolType,
                definition.spell(),
                spell,
                resolvedBonuses
        );
        resolvedBySchool.put(schoolId, resolvedDefinition);
        resolvedSpellIds.add(definition.spell());
    }

    @Nullable
    private static List<ResolvedEnchantmentBonus> resolveBonuses(ResourceLocation resourceId, ElementalBowModeDefinition definition) {
        var resolvedBonuses = new ArrayList<ResolvedEnchantmentBonus>(definition.enchantmentBonuses().size());
        for (var bonus : definition.enchantmentBonuses()) {
            var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(bonus.enchantment());
            if (enchantment == null) {
                ApprenticeCodex.LOGGER.warn(
                        "Elemental Bow mode {} in {} was ignored because enchantment {} could not be resolved.",
                        definition.school(),
                        resourceId,
                        bonus.enchantment()
                );
                return null;
            }
            resolvedBonuses.add(new ResolvedEnchantmentBonus(bonus.enchantment(), enchantment, bonus.bonusPerLevel(), bonus.flatBonus()));
        }
        return List.copyOf(resolvedBonuses);
    }

    public record ResolvedDefinition(
            ResourceLocation schoolId,
            SchoolType schoolType,
            ResourceLocation spellId,
            AbstractSpell spell,
            List<ResolvedEnchantmentBonus> enchantmentBonuses
    ) {
        public ResolvedDefinition {
            enchantmentBonuses = List.copyOf(enchantmentBonuses);
        }

        public int resolveSpellLevel(ItemStack stack) {
            var spellLevel = 1 + stack.getEnchantmentLevel(Enchantments.POWER_ARROWS);
            for (var bonus : enchantmentBonuses) {
                var enchantmentLevel = stack.getEnchantmentLevel(bonus.enchantment());
                if (enchantmentLevel <= 0) {
                    continue;
                }
                spellLevel += bonus.flatBonus() + bonus.bonusPerLevel() * enchantmentLevel;
            }
            return net.minecraft.util.Mth.clamp(spellLevel, spell.getMinLevel(), spell.getMaxLevel());
        }

        public int color() {
            return SchoolAffinityRegistry.resolveColor(schoolType);
        }
    }

    public record ResolvedEnchantmentBonus(
            ResourceLocation enchantmentId,
            Enchantment enchantment,
            int bonusPerLevel,
            int flatBonus
    ) {
    }
}
