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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
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
            resolvedBonuses.add(new ResolvedEnchantmentBonus(
                    bonus.enchantment(),
                    bonus.bonusPerLevel(),
                    bonus.flatBonus()
            ));
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
            var spellLevel = 1 + getEnchantmentLevel(stack, Enchantments.POWER.location());
            // Elemental Bow の属性ショットは preset spell level を tooltip / UI / 実詠唱で共有しているため、
            // 汎用イベント加算ではなくここで POWER 相当の基礎レベルとして先に合算する。
            spellLevel += jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(
                    stack,
                    jp.aquafactory.apprenticecodex.enchantment.Enchantments.TRANSCENDENCE
            );
            for (var bonus : enchantmentBonuses) {
                var enchantmentLevel = getEnchantmentLevel(stack, bonus.enchantmentId());
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
            int bonusPerLevel,
            int flatBonus
    ) {
    }

    private static int getEnchantmentLevel(ItemStack stack, ResourceLocation enchantmentId) {
        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (var enchantment : enchantments.keySet()) {
            var key = enchantment.unwrapKey().orElse(null);
            if (key != null && enchantmentId.equals(key.location())) {
                return enchantments.getLevel(enchantment);
            }
        }
        return 0;
    }
}
