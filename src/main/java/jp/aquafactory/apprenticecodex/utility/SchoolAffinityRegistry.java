package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.effect.SchoolAffinityEffect;
import jp.aquafactory.apprenticecodex.potion.SchoolAffinityPotion;
import jp.aquafactory.apprenticecodex.potion.SchoolAffinityPotionVariant;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SchoolAffinityRegistry {
    private static final List<ResourceLocation> BUILTIN_IRONS_SCHOOL_IDS = List.of(
            SchoolRegistry.FIRE_RESOURCE,
            SchoolRegistry.ICE_RESOURCE,
            SchoolRegistry.LIGHTNING_RESOURCE,
            SchoolRegistry.HOLY_RESOURCE,
            SchoolRegistry.ENDER_RESOURCE,
            SchoolRegistry.BLOOD_RESOURCE,
            SchoolRegistry.EVOCATION_RESOURCE,
            SchoolRegistry.NATURE_RESOURCE,
            SchoolRegistry.ELDRITCH_RESOURCE
    );
    private static final int BUILTIN_AFFINITY_SLOT_COUNT = BUILTIN_IRONS_SCHOOL_IDS.size();
    private static final int EXTRA_AFFINITY_SLOT_COUNT = 8;
    public static final int MAX_AFFINITY_SLOTS = BUILTIN_AFFINITY_SLOT_COUNT + EXTRA_AFFINITY_SLOT_COUNT;

    private static final List<SchoolAffinityDefinition> DEFINITIONS = new ArrayList<>();
    private static final Map<MobEffect, SchoolAffinityDefinition> DEFINITION_BY_EFFECT = new IdentityHashMap<>();
    private static final Map<Potion, SchoolAffinityDefinition> DEFINITION_BY_POTION = new IdentityHashMap<>();
    private static final SchoolType[] ASSIGNED_SCHOOLS = new SchoolType[MAX_AFFINITY_SLOTS];
    private static final Map<ResourceLocation, SchoolAffinityDefinition> DEFINITION_BY_SCHOOL_ID = new LinkedHashMap<>();

    @Nullable
    private static Map<Item, SchoolAffinityDefinition> catalystDefinitionByItem;

    private static boolean bindingsResolved;
    private static boolean warnedExtraSchoolCount;

    private SchoolAffinityRegistry() {
    }

    public static synchronized List<SchoolAffinityDefinition> getDefinitions() {
        if (!DEFINITIONS.isEmpty()) {
            return List.copyOf(DEFINITIONS);
        }

        for (var slotIndex = 0; slotIndex < MAX_AFFINITY_SLOTS; slotIndex++) {
            var suffix = String.format("%02d", slotIndex);
            var effect = new SchoolAffinityEffect(slotIndex);
            var basePotion = new SchoolAffinityPotion(slotIndex, SchoolAffinityPotionVariant.BASE, effect);
            var longPotion = new SchoolAffinityPotion(slotIndex, SchoolAffinityPotionVariant.LONG, effect);
            var strongPotion = new SchoolAffinityPotion(slotIndex, SchoolAffinityPotionVariant.STRONG, effect);

            var definition = new SchoolAffinityDefinition(
                    slotIndex,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "affinity_" + suffix),
                    effect,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "affinity_" + suffix),
                    basePotion,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "long_affinity_" + suffix),
                    longPotion,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "strong_affinity_" + suffix),
                    strongPotion
            );
            DEFINITIONS.add(definition);
            DEFINITION_BY_EFFECT.put(effect, definition);
            DEFINITION_BY_POTION.put(basePotion, definition);
            DEFINITION_BY_POTION.put(longPotion, definition);
            DEFINITION_BY_POTION.put(strongPotion, definition);
        }

        return List.copyOf(DEFINITIONS);
    }

    public static Optional<SchoolAffinityDefinition> findByEffect(MobEffect effect) {
        ensureBindingsResolved();

        var definition = DEFINITION_BY_EFFECT.get(effect);
        if (definition == null || getAssignedSchool(definition.slotIndex()).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(definition);
    }

    public static Optional<SchoolAffinityDefinition> findByPotion(Potion potion) {
        ensureBindingsResolved();

        var definition = DEFINITION_BY_POTION.get(potion);
        if (definition == null || getAssignedSchool(definition.slotIndex()).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(definition);
    }

    public static Optional<SchoolAffinityDefinition> findByCatalyst(ItemStack ingredient) {
        ensureBindingsResolved();

        if (ingredient.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(getCatalystDefinitionByItem().get(ingredient.getItem()));
    }

    public static synchronized Map<Item, SchoolAffinityDefinition> getBrewingDefinitionsByCatalyst() {
        ensureBindingsResolved();
        return Map.copyOf(getCatalystDefinitionByItem());
    }

    public static synchronized void invalidateBindings() {
        Arrays.fill(ASSIGNED_SCHOOLS, null);
        DEFINITION_BY_SCHOOL_ID.clear();
        catalystDefinitionByItem = null;
        bindingsResolved = false;
    }

    public static Component createAffinityName(SchoolType schoolType) {
        return Component.translatable("effect.apprenticecodex.school_affinity.dynamic", schoolType.getDisplayName());
    }

    public static ItemStack createIconStack(@Nullable SchoolType schoolType) {
        if (schoolType == null) {
            return new ItemStack(ItemRegistry.ARCANE_CINDER.get());
        }

        var schoolId = schoolType.getId();
        var runeId = ResourceLocation.fromNamespaceAndPath(schoolId.getNamespace(), schoolId.getPath() + "_rune");
        var runeItem = BuiltInRegistries.ITEM.getOptional(runeId).orElse(null);
        if (runeItem != null && runeItem != Items.AIR) {
            return new ItemStack(runeItem);
        }

        var focusItem = resolveSingleFocusItem(schoolType);
        if (focusItem != null) {
            return new ItemStack(focusItem);
        }

        return new ItemStack(ItemRegistry.ARCANE_CINDER.get());
    }

    public static Optional<SchoolType> getAssignedSchool(int slotIndex) {
        ensureBindingsResolved();

        if (slotIndex < 0 || slotIndex >= ASSIGNED_SCHOOLS.length) {
            return Optional.empty();
        }
        return Optional.ofNullable(ASSIGNED_SCHOOLS[slotIndex]);
    }

    public static UUID createModifierUuid(int slotIndex) {
        return UUID.nameUUIDFromBytes(("apprenticecodex:school_affinity_slot:" + slotIndex).getBytes(StandardCharsets.UTF_8));
    }

    public static int resolveColor(SchoolType schoolType) {
        var textColor = schoolType.getDisplayName().getStyle().getColor();
        if (textColor != null) {
            return textColor.getValue();
        }

        var targetingColor = schoolType.getTargetingColor();
        var red = Math.max(0, Math.min(255, Math.round(targetingColor.x() * 255f)));
        var green = Math.max(0, Math.min(255, Math.round(targetingColor.y() * 255f)));
        var blue = Math.max(0, Math.min(255, Math.round(targetingColor.z() * 255f)));
        return (red << 16) | (green << 8) | blue;
    }

    private static synchronized void ensureBindingsResolved() {
        if (bindingsResolved) {
            return;
        }

        getDefinitions();
        Arrays.fill(ASSIGNED_SCHOOLS, null);
        DEFINITION_BY_SCHOOL_ID.clear();
        catalystDefinitionByItem = new LinkedHashMap<>();

        var schoolRegistry = SchoolRegistry.REGISTRY.get();
        var supportedDefinitionsByCatalyst = new LinkedHashMap<Item, List<SchoolAffinityDefinition>>();

        // Iron's 本体分はスロットを固定し、今後もセーブデータ上の対応がずれないようにする.
        for (var slotIndex = 0; slotIndex < BUILTIN_AFFINITY_SLOT_COUNT; slotIndex++) {
            var schoolId = BUILTIN_IRONS_SCHOOL_IDS.get(slotIndex);
            var schoolType = schoolRegistry.getValue(schoolId);
            bindSchoolToSlot(slotIndex, schoolType, supportedDefinitionsByCatalyst);
        }

        var extraSchools = schoolRegistry.getValues().stream()
                .filter(school -> !BUILTIN_IRONS_SCHOOL_IDS.contains(school.getId()))
                .filter(school -> MagicTools.resolveSchoolPowerAttribute(school) != null)
                .sorted(Comparator.comparing(SchoolAffinityRegistry::getExtraSchoolSortKey))
                .toList();

        if (extraSchools.size() > EXTRA_AFFINITY_SLOT_COUNT && !warnedExtraSchoolCount) {
            ApprenticeCodex.LOGGER.warn(
                    "School Affinity has {} extra schools loaded, but only {} extra slots are available. Some schools may be unsupported.",
                    extraSchools.size(),
                    EXTRA_AFFINITY_SLOT_COUNT
            );
            warnedExtraSchoolCount = true;
        }

        for (var extraIndex = 0; extraIndex < Math.min(extraSchools.size(), EXTRA_AFFINITY_SLOT_COUNT); extraIndex++) {
            var slotIndex = BUILTIN_AFFINITY_SLOT_COUNT + extraIndex;
            bindSchoolToSlot(slotIndex, extraSchools.get(extraIndex), supportedDefinitionsByCatalyst);
        }

        var uniqueDefinitionsByCatalyst = new LinkedHashMap<Item, SchoolAffinityDefinition>();
        for (var entry : supportedDefinitionsByCatalyst.entrySet()) {
            if (entry.getValue().size() != 1) {
                continue;
            }

            uniqueDefinitionsByCatalyst.put(entry.getKey(), entry.getValue().get(0));
        }

        catalystDefinitionByItem = uniqueDefinitionsByCatalyst;
        bindingsResolved = true;
        logResolvedAssignments();
    }

    private static void bindSchoolToSlot(int slotIndex, @Nullable SchoolType schoolType, Map<Item, List<SchoolAffinityDefinition>> supportedDefinitionsByCatalyst) {
        if (schoolType == null || slotIndex < 0 || slotIndex >= DEFINITIONS.size()) {
            return;
        }

        var spellPowerAttribute = MagicTools.resolveSchoolPowerAttribute(schoolType);
        if (spellPowerAttribute == null) {
            ApprenticeCodex.LOGGER.warn("Skipped School Affinity for {} because spell power attribute could not be resolved.", schoolType.getId());
            return;
        }

        var definition = DEFINITIONS.get(slotIndex);
        ASSIGNED_SCHOOLS[slotIndex] = schoolType;
        DEFINITION_BY_SCHOOL_ID.put(schoolType.getId(), definition);

        var focusItem = resolveSingleFocusItem(schoolType);
        if (focusItem == null) {
            return;
        }

        // 素材の競合はここで潰す. レッドストーン/グロウストーン粉は先にブロックへ置換する.
        var catalyst = substituteCatalystItem(focusItem);
        supportedDefinitionsByCatalyst.computeIfAbsent(catalyst, ignored -> new ArrayList<>()).add(definition);
    }

    private static synchronized Map<Item, SchoolAffinityDefinition> getCatalystDefinitionByItem() {
        ensureBindingsResolved();

        if (catalystDefinitionByItem == null) {
            catalystDefinitionByItem = new LinkedHashMap<>();
        }
        return catalystDefinitionByItem;
    }

    @Nullable
    private static Item resolveSingleFocusItem(SchoolType schoolType) {
        Item matchedItem = null;

        for (var item : BuiltInRegistries.ITEM) {
            if (!new ItemStack(item).is(schoolType.getFocus())) {
                continue;
            }

            if (matchedItem != null && matchedItem != item) {
                return null;
            }
            matchedItem = item;
        }

        return matchedItem;
    }

    private static Item substituteCatalystItem(Item item) {
        if (item == Items.REDSTONE) {
            return Items.REDSTONE_BLOCK;
        }
        if (item == Items.GLOWSTONE_DUST) {
            return Items.GLOWSTONE;
        }
        return item;
    }

    private static String getExtraSchoolSortKey(SchoolType schoolType) {
        var contents = schoolType.getDisplayName().getContents();
        if (contents instanceof TranslatableContents translatableContents) {
            return translatableContents.getKey();
        }
        return schoolType.getId().toString();
    }

    private static void logResolvedAssignments() {
        ApprenticeCodex.LOGGER.info("School Affinity potion assignments resolved.");
        for (var definition : DEFINITIONS) {
            var schoolType = ASSIGNED_SCHOOLS[definition.slotIndex()];
            var assignedSchoolId = schoolType != null ? schoolType.getId().toString() : "<empty>";
            ApprenticeCodex.LOGGER.info(
                    "School Affinity slot {}: {} -> {}",
                    String.format("%02d", definition.slotIndex()),
                    definition.basePotionId(),
                    assignedSchoolId
            );
        }
    }

    public record SchoolAffinityDefinition(
            int slotIndex,
            ResourceLocation effectId,
            SchoolAffinityEffect effect,
            ResourceLocation basePotionId,
            SchoolAffinityPotion basePotion,
            ResourceLocation longPotionId,
            SchoolAffinityPotion longPotion,
            ResourceLocation strongPotionId,
            SchoolAffinityPotion strongPotion
    ) {
    }
}
